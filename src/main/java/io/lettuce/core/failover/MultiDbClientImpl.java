package io.lettuce.core.failover;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.Delegating;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.MultiDbAsyncConnectionBuilder.RedisDatabaseAsyncCompletion;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.failover.health.HealthCheck;
import io.lettuce.core.failover.health.HealthCheckStrategy;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.failover.health.HealthStatusManagerImpl;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Failover-aware client that composes multiple standalone Redis endpoints and returns a single Stateful connection wrapper
 * which can switch the active endpoint without requiring users to recreate command objects.
 *
 * <p>
 * Standalone-only POC. Not for Sentinel/Cluster.
 * </p>
 *
 * @author Ali Takavci
 * @since 7.4
 */
class MultiDbClientImpl extends RedisClient implements MultiDbClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultiDbClientImpl.class);

    private static final RedisURI EMPTY_URI = new ImmutableRedisURI(new RedisURI());

    private final Map<RedisURI, DatabaseConfig> databaseConfigs;

    private ThreadLocal<ClientOptions> localClientOptions = new ThreadLocal<>();

    MultiDbClientImpl(Collection<DatabaseConfig> databaseConfigs) {
        this(null, databaseConfigs);
    }

    MultiDbClientImpl(ClientResources clientResources, Collection<DatabaseConfig> databaseConfigs) {
        super(clientResources, EMPTY_URI);

        if (databaseConfigs == null || databaseConfigs.isEmpty()) {
            this.databaseConfigs = new ConcurrentHashMap<>();
        } else {
            this.databaseConfigs = new ConcurrentHashMap<>(databaseConfigs.size());
            for (DatabaseConfig config : databaseConfigs) {
                LettuceAssert.notNull(config, "DatabaseConfig must not be null");
                LettuceAssert.notNull(config.getRedisURI(), "RedisURI must not be null");
                this.databaseConfigs.put(config.getRedisURI(), config);
            }
        }
    }

    /**
     * Open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection
     */
    public StatefulRedisMultiDbConnection<String, String> connect() {
        return connect(newStringStringCodec());
    }

    @Override
    public Collection<RedisURI> getRedisURIs() {
        return databaseConfigs.keySet();
    }

    @Override
    public ClientOptions getOptions() {
        ClientOptions options = localClientOptions.get();
        if (options == null) {
            throw new IllegalStateException("ClientOptions not set!");
        }
        return options;
    }

    @Override
    public void setOptions(ClientOptions clientOptions) {
        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");
        this.localClientOptions.set(clientOptions);
    }

    void resetOptions() {
        this.localClientOptions.remove();
    }

    /**
     * Open a new connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and values. This
     * method is synchronous and will block until all database connections are established. It also waits for the initial health
     * checks to complete starting from most weighted database, ensuring that at least one database is healthy before returning
     * to use in the order of their weights.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis connection
     */
    public <K, V> StatefulRedisMultiDbConnection<K, V> connect(RedisCodec<K, V> codec) {

        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }

        HealthStatusManager healthStatusManager = createHealthStatusManager();

        Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> databases = new ConcurrentHashMap<>(
                databaseConfigs.size());
        for (Map.Entry<RedisURI, DatabaseConfig> entry : databaseConfigs.entrySet()) {
            RedisURI uri = entry.getKey();
            DatabaseConfig config = entry.getValue();

            // HACK: looks like repeating the implementation all around 'RedisClient.connect' is an overkill.
            // connections.put(uri, connect(codec, uri));
            // Instead we will use it from delegate
            RedisDatabaseImpl<StatefulRedisConnection<K, V>> database = createRedisDatabase(config, codec, healthStatusManager);

            databases.put(uri, database);
        }

        StatusTracker statusTracker = new StatusTracker(healthStatusManager, getResources());
        // Wait for health checks to complete if configured
        waitForInitialHealthyDatabase(statusTracker, databases);

        // Provide a connection factory for dynamic database addition
        return createMultiDbConnection(databases, codec, healthStatusManager);
    }

    protected HealthStatusManager createHealthStatusManager() {
        return new HealthStatusManagerImpl();
    }

    private <K, V> RedisDatabaseImpl<StatefulRedisConnection<K, V>> createRedisDatabase(DatabaseConfig config,
            RedisCodec<K, V> codec, HealthStatusManager healthStatusManager) {
        RedisURI uri = config.getRedisURI();
        setOptions(config.getClientOptions());
        try {
            StatefulRedisConnection<K, V> connection = connect(codec, uri);
            DatabaseEndpoint databaseEndpoint = extractDatabaseEndpoint(connection);
            CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(config.getCircuitBreakerConfig());
            databaseEndpoint.bind(circuitBreaker);

            HealthCheck healthCheck = null;
            if (HealthCheckStrategySupplier.NO_HEALTH_CHECK != config.getHealthCheckStrategySupplier()) {
                HealthCheckStrategy hcStrategy = config.getHealthCheckStrategySupplier().get(config.getRedisURI(),
                        new DatabaseRawConnectionFactoryImpl(config.getClientOptions()));
                healthCheck = healthStatusManager.add(uri, hcStrategy);
            }

            RedisDatabaseImpl<StatefulRedisConnection<K, V>> database = new RedisDatabaseImpl<>(config, connection,
                    databaseEndpoint, circuitBreaker, healthCheck);
            if (logger.isInfoEnabled()) {
                logger.info("Created database: {} with CircuitBreaker {} and HealthCheck {}", database.getId(),
                        circuitBreaker.getId(), healthCheck != null ? healthCheck.getEndpoint() : "N/A");
            }
            return database;
        } finally {
            resetOptions();
        }
    }

    // ASYNC CONNECT

    /**
     * Asynchronously open a new connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values. This method is asynchronous and returns a {@link MultiDbConnectionFuture} that completes when all database
     * connections are established and initial health checks (if configured) have completed.
     * <p>
     * The returned {@link MultiDbConnectionFuture} ensures that all callbacks (thenApply, thenAccept, etc.) execute on a
     * separate thread pool rather than on Netty event loop threads, preventing deadlocks when calling blocking sync operations
     * inside callbacks.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis connection future
     */
    @Override
    public <K, V> MultiDbConnectionFuture<K, V> connectAsync(RedisCodec<K, V> codec) {
        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }

        MultiDbAsyncConnectionBuilder<K, V> builder = createConnectionBuilder(codec);

        CompletableFuture<StatefulRedisMultiDbConnection<K, V>> future = builder.connectAsync(databaseConfigs);

        return MultiDbConnectionFuture.from(future, getResources().eventExecutorGroup());
    }

    /**
     * Creates a new {@link MultiDbAsyncConnectionBuilder} instance.
     *
     * @param <K> Key type
     * @param <V> Value type
     * @return a new multi-database async connection builder
     */
    protected <K, V> MultiDbAsyncConnectionBuilder<K, V> createConnectionBuilder(RedisCodec<K, V> codec) {
        return new MultiDbAsyncConnectionBuilder<>(this, getResources(), codec, this::createMultiDbConnection);
    }

    /**
     * Creates a new {@link StatefulRedisMultiDbConnection} instance with the provided healthy database map.
     *
     * @param healthyDatabaseMap the map of healthy databases
     * @param codec the Redis codec
     * @param healthStatusManager the health status manager
     * @param <K> Key type
     * @param <V> Value type
     * @return a new multi-database connection
     */
    protected <K, V> StatefulRedisMultiDbConnection<K, V> createMultiDbConnection(
            Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> healthyDatabaseMap, RedisCodec<K, V> codec,
            HealthStatusManager healthStatusManager) {

        return new StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<K, V>, K, V>(healthyDatabaseMap, getResources(),
                codec, this::createRedisDatabase, healthStatusManager);
    }

    /**
     * Creates a new {@link StatefulRedisMultiDbConnection} instance with the provided healthy database map.
     *
     * @param healthyDatabaseMap the map of healthy databases
     * @param codec the Redis codec
     * @param healthStatusManager the health status manager
     * @param <K> Key type
     * @param <V> Value type
     * @return a new multi-database connection
     */
    protected <K, V> StatefulRedisMultiDbConnection<K, V> createMultiDbConnection(
            RedisDatabaseImpl<StatefulRedisConnection<K, V>> selected,
            Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> databases, RedisCodec<K, V> codec,
            HealthStatusManager healthStatusManager, RedisDatabaseAsyncCompletion<StatefulRedisConnection<K, V>> completion) {

        return new StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<K, V>, K, V>(selected, databases, getResources(),
                codec, this::createRedisDatabase, healthStatusManager, completion);
    }
    // END OF ASYNC CONNECT

    /**
     * Open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection
     */
    public StatefulRedisMultiDbPubSubConnection<String, String> connectPubSub() {
        return connectPubSub(newStringStringCodec());
    }

    public <K, V> StatefulRedisMultiDbPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec) {

        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }

        HealthStatusManager healthStatusManager = createHealthStatusManager();

        Map<RedisURI, RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>>> databases = new ConcurrentHashMap<>(
                databaseConfigs.size());
        for (Map.Entry<RedisURI, DatabaseConfig> entry : databaseConfigs.entrySet()) {
            RedisURI uri = entry.getKey();
            DatabaseConfig config = entry.getValue();

            RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>> database = createRedisDatabaseWithPubSub(config, codec,
                    healthStatusManager);
            databases.put(uri, database);
        }

        StatusTracker statusTracker = new StatusTracker(healthStatusManager, getResources());
        // Wait for health checks to complete if configured
        waitForInitialHealthyDatabase(statusTracker, databases);

        // Provide a connection factory for dynamic database addition
        return new StatefulRedisMultiDbPubSubConnectionImpl<K, V>(databases, getResources(), codec,
                this::createRedisDatabaseWithPubSub, healthStatusManager);
    }

    private <K, V> RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>> createRedisDatabaseWithPubSub(DatabaseConfig config,
            RedisCodec<K, V> codec, HealthStatusManager healthStatusManager) {
        RedisURI uri = config.getRedisURI();
        setOptions(config.getClientOptions());
        try {
            StatefulRedisPubSubConnection<K, V> connection = connectPubSub(codec, uri);
            DatabaseEndpoint databaseEndpoint = extractDatabaseEndpoint(connection);
            CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config.getCircuitBreakerConfig());
            databaseEndpoint.bind(circuitBreaker);

            HealthCheck healthCheck = null;
            if (HealthCheckStrategySupplier.NO_HEALTH_CHECK != config.getHealthCheckStrategySupplier()) {
                HealthCheckStrategy hcStrategy = config.getHealthCheckStrategySupplier().get(config.getRedisURI(),
                        new DatabaseRawConnectionFactoryImpl(config.getClientOptions()));
                healthCheck = healthStatusManager.add(uri, hcStrategy);
            }

            RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>> database = new RedisDatabaseImpl<>(config, connection,
                    databaseEndpoint, circuitBreaker, healthCheck);
            return database;
        } finally {
            resetOptions();
        }
    }

    private DatabaseEndpoint extractDatabaseEndpoint(StatefulRedisConnection<?, ?> connection) {
        RedisChannelWriter writer = ((StatefulRedisConnectionImpl<?, ?>) connection).getChannelWriter();
        if (writer instanceof Delegating) {
            writer = (RedisChannelWriter) ((Delegating<?>) writer).unwrap();
        }
        return (DatabaseEndpoint) writer;
    }

    @Override
    protected DefaultEndpoint createEndpoint() {
        return new DatabaseEndpointImpl(getOptions(), getResources());
    }

    @Override
    protected <K, V> PubSubEndpoint<K, V> createPubSubEndpoint() {
        return new DatabasePubSubEndpointImpl<>(getOptions(), getResources());
    }

    /**
     * Waits for initial health check results and selects the first healthy database based on weight priority. Blocks until at
     * least one database becomes healthy or all databases are determined to be unhealthy.
     *
     * @param statusTracker the status tracker to use for waiting on health check results
     * @param databaseMap the map of databases to evaluate
     * @throws RedisConnectionException if all databases are unhealthy
     */
    private void waitForInitialHealthyDatabase(StatusTracker statusTracker,
            Map<RedisURI, ? extends RedisDatabaseImpl<?>> databaseMap) {
        // Sort databases by weight in descending order
        List<? extends Map.Entry<RedisURI, ? extends RedisDatabaseImpl<?>>> sortedDatabases = databaseMap.entrySet().stream()
                .sorted(Map.Entry
                        .comparingByValue(Comparator.comparing((RedisDatabaseImpl<?> db) -> db.getWeight()).reversed()))
                .collect(Collectors.toList());
        logger.info("Selecting initial database from {} configured databases", sortedDatabases.size());

        // Select database in weight order
        for (Map.Entry<RedisURI, ? extends RedisDatabaseImpl<?>> entry : sortedDatabases) {
            RedisURI endpoint = entry.getKey();
            RedisDatabaseImpl<?> database = entry.getValue();

            logger.info("Evaluating database {} (weight: {})", endpoint, database.getWeight());

            HealthStatus status;

            // Check if health checks are enabled for this database
            if (database.getHealthCheck() != null) {
                logger.info("Health checks enabled for {}, waiting for result", endpoint);
                // Wait for this database's health status to be determined
                status = statusTracker.waitForHealthStatus(endpoint);
            } else {
                // No health check configured - assume healthy
                logger.info("No health check configured for database {}, defaulting to HEALTHY", endpoint);
                status = HealthStatus.HEALTHY;
            }

            if (status.isHealthy()) {
                logger.info("Found healthy database: {} (weight: {})", endpoint, database.getWeight());
                return;
            } else {
                logger.info("Database {} is unhealthy, trying next database", endpoint);
            }
        }

        // All databases are unhealthy
        throw new RedisConnectionException("All configured databases are unhealthy.");
    }

    private class DatabaseRawConnectionFactoryImpl implements DatabaseRawConnectionFactory {

        private ClientOptions clientOptions;

        public DatabaseRawConnectionFactoryImpl(ClientOptions clientOptions) {
            this.clientOptions = clientOptions;
        }

        @Override
        public StatefulRedisConnection<?, ?> connectToDatabase(RedisURI endpoint) {
            MultiDbClientImpl.this.setOptions(clientOptions);
            try {
                return MultiDbClientImpl.this.connect(endpoint);
            } finally {
                MultiDbClientImpl.this.resetOptions();
            }
        }

    }

}
