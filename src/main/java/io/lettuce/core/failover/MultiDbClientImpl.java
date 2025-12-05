package io.lettuce.core.failover;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.lettuce.core.Delegating;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.failover.health.HealthCheck;
import io.lettuce.core.failover.health.HealthCheckStrategy;
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
 * Standalone-only POC. Not for Sentinel/Cluster.
 *
 * @author Ali Takavci
 * @since 7.1
 */
class MultiDbClientImpl extends RedisClient implements MultiDbClient {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultiDbClientImpl.class);

    private static final RedisURI EMPTY_URI = new RedisURI();

    private final Map<RedisURI, DatabaseConfig> databaseConfigs;

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

    public <K, V> StatefulRedisMultiDbConnection<K, V> connect(RedisCodec<K, V> codec) {

        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }

        HealthStatusManager healthStatusManager = createHealthStatusManager();

        Map<RedisURI, RedisDatabase<StatefulRedisConnection<K, V>>> databases = new ConcurrentHashMap<>(databaseConfigs.size());
        for (Map.Entry<RedisURI, DatabaseConfig> entry : databaseConfigs.entrySet()) {
            RedisURI uri = entry.getKey();
            DatabaseConfig config = entry.getValue();

            // HACK: looks like repeating the implementation all around 'RedisClient.connect' is an overkill.
            // connections.put(uri, connect(codec, uri));
            // Instead we will use it from delegate
            RedisDatabase<StatefulRedisConnection<K, V>> database = createRedisDatabase(config, codec, healthStatusManager);

            databases.put(uri, database);
        }

        StatusTracker statusTracker = new StatusTracker(healthStatusManager);
        // Wait for health checks to complete if configured
        waitForInitialHealthyDatabase(statusTracker, databases);

        // Provide a connection factory for dynamic database addition
        return new StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<K, V>, K, V>(databases, getResources(), codec,
                getOptions().getJsonParser(), this::createRedisDatabase, healthStatusManager);
    }

    protected HealthStatusManager createHealthStatusManager() {
        return new HealthStatusManagerImpl();
    }

    private <K, V> RedisDatabase<StatefulRedisConnection<K, V>> createRedisDatabase(DatabaseConfig config,
            RedisCodec<K, V> codec, HealthStatusManager healthStatusManager) {
        RedisURI uri = config.getRedisURI();
        StatefulRedisConnection<K, V> connection = connect(codec, uri);
        DatabaseEndpoint databaseEndpoint = extractDatabaseEndpoint(connection);

        HealthCheck healthCheck;
        if (config.getHealthCheckStrategySupplier() != null) {
            HealthCheckStrategy hcStrategy = config.getHealthCheckStrategySupplier().get(config.getRedisURI(),
                    connection.getOptions());
            healthCheck = healthStatusManager.add(uri, hcStrategy);
        } else {
            healthCheck = null;
        }

        RedisDatabase<StatefulRedisConnection<K, V>> database = new RedisDatabase<>(config, connection, databaseEndpoint,
                healthCheck);

        return database;
    }

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

        Map<RedisURI, RedisDatabase<StatefulRedisPubSubConnection<K, V>>> databases = new ConcurrentHashMap<>(
                databaseConfigs.size());
        for (Map.Entry<RedisURI, DatabaseConfig> entry : databaseConfigs.entrySet()) {
            RedisURI uri = entry.getKey();
            DatabaseConfig config = entry.getValue();

            RedisDatabase<StatefulRedisPubSubConnection<K, V>> database = createRedisDatabaseWithPubSub(config, codec,
                    healthStatusManager);
            databases.put(uri, database);
        }

        StatusTracker statusTracker = new StatusTracker(healthStatusManager);
        // Wait for health checks to complete if configured
        waitForInitialHealthyDatabase(statusTracker, databases);

        // Provide a connection factory for dynamic database addition
        return new StatefulRedisMultiDbPubSubConnectionImpl<K, V>(databases, getResources(), codec,
                getOptions().getJsonParser(), this::createRedisDatabaseWithPubSub, healthStatusManager);
    }

    private <K, V> RedisDatabase<StatefulRedisPubSubConnection<K, V>> createRedisDatabaseWithPubSub(DatabaseConfig config,
            RedisCodec<K, V> codec, HealthStatusManager healthStatusManager) {
        RedisURI uri = config.getRedisURI();
        StatefulRedisPubSubConnection<K, V> connection = connectPubSub(codec, uri);
        DatabaseEndpoint databaseEndpoint = extractDatabaseEndpoint(connection);

        HealthCheck healthCheck;
        if (config.getHealthCheckStrategySupplier() != null) {
            HealthCheckStrategy hcStrategy = config.getHealthCheckStrategySupplier().get(config.getRedisURI(),
                    connection.getOptions());
            healthCheck = healthStatusManager.add(uri, hcStrategy);
        } else {
            healthCheck = null;
        }

        RedisDatabase<StatefulRedisPubSubConnection<K, V>> database = new RedisDatabase<>(config, connection, databaseEndpoint,
                healthCheck);
        return database;
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
            Map<RedisURI, ? extends RedisDatabase<?>> databaseMap) {
        // Sort databases by weight in descending order
        List<? extends Map.Entry<RedisURI, ? extends RedisDatabase<?>>> sortedDatabases = databaseMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparing((RedisDatabase<?> db) -> db.getWeight()).reversed()))
                .collect(Collectors.toList());
        logger.info("Selecting initial database from {} configured databases", sortedDatabases.size());

        // Select database in weight order
        for (Map.Entry<RedisURI, ? extends RedisDatabase<?>> entry : sortedDatabases) {
            RedisURI endpoint = entry.getKey();
            RedisDatabase<?> database = entry.getValue();

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

            if (status == HealthStatus.HEALTHY) {
                logger.info("Found healthy database: {} (weight: {})", endpoint, database.getWeight());
                return;
            } else {
                logger.info("Database {} is unhealthy, trying next database", endpoint);
            }
        }

        // All databases are unhealthy
        throw new RedisConnectionException("All configured databases are unhealthy.");
    }

}
