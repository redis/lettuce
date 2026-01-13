package io.lettuce.core.failover;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.Delegating;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.health.HealthCheck;
import io.lettuce.core.failover.health.HealthCheckStrategy;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.failover.health.HealthStatusManagerImpl;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Builder for creating asynchronous multi-database Redis connections with health checking and failover support.
 * <p>
 * This class orchestrates the creation of connections to multiple Redis databases, manages health checks for each database, and
 * selects an initial primary database based on configuration weights and health status. It supports:
 * <ul>
 * <li>Asynchronous connection establishment to multiple Redis endpoints</li>
 * <li>Health check integration with configurable strategies</li>
 * <li>Circuit breaker pattern for database resilience</li>
 * <li>Weight-based database selection for initial primary</li>
 * <li>Automatic failover when databases become unhealthy</li>
 * </ul>
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Lettuce Contributors
 */
class MultiDbAsyncConnectionBuilder<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultiDbAsyncConnectionBuilder.class);

    private final MultiDbClientImpl client;

    private final ClientResources resources;

    private final RedisCodec<K, V> codec;

    private final MultiDbConnectionFactory multiDbConnectionFactory;

    /**
     * Creates a new {@link MultiDbAsyncConnectionBuilder}.
     *
     * @param client the multi-database client instance
     * @param resources the client resources for event loops and thread pools
     * @param codec the codec for encoding/decoding keys and values
     * @param multiDbConnectionFactory factory for creating multi-database connections
     */
    MultiDbAsyncConnectionBuilder(MultiDbClientImpl client, ClientResources resources, RedisCodec<K, V> codec,
            MultiDbConnectionFactory multiDbConnectionFactory) {
        this.resources = resources;
        this.client = client;
        this.codec = codec;
        this.multiDbConnectionFactory = multiDbConnectionFactory;
    }

    /**
     * Asynchronously establishes connections to all configured Redis databases and creates a multi-database connection.
     * <p>
     * This method:
     * <ol>
     * <li>Creates a health status manager for tracking database health</li>
     * <li>Initiates asynchronous connections to all configured databases</li>
     * <li>Sets up health checks for each database (if configured)</li>
     * <li>Waits for health check results to determine the initial primary database</li>
     * <li>Selects the highest-weighted healthy database as the initial primary</li>
     * <li>Returns a future that completes with the multi-database connection</li>
     * </ol>
     *
     * @param databaseConfigs map of Redis URIs to their database configurations
     * @return a {@link CompletableFuture} that completes with a {@link StatefulRedisMultiDbConnection} when at least one
     *         healthy database is available, or completes exceptionally if all databases fail
     */
    CompletableFuture<StatefulRedisMultiDbConnection<K, V>> connectAsync(Map<RedisURI, DatabaseConfig> databaseConfigs) {

        HealthStatusManager healthStatusManager = createHealthStatusManager();

        // Create a map to hold the final database instances
        DatabaseMap<K, V> databases = new DatabaseMap<>(databaseConfigs.size());

        // Create async database connections for all configured endpoints
        DatabaseFutureMap<K, V> databaseFutures = createDatabaseFutures(databaseConfigs, databases, healthStatusManager);

        // Create a map of futures, one for each database's health status
        Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = createHealthStatusFutures(databaseFutures,
                healthStatusManager);

        // Build the final connection future
        CompletableFuture<StatefulRedisMultiDbConnection<K, V>> connectionFuture = buildFuture(databaseConfigs,
                healthStatusManager, databases, databaseFutures, healthStatusFutures);
        return connectionFuture;
    }

    /**
     * Builds the connection future that completes when an initial primary database is selected.
     * <p>
     * This method sets up completion handlers for all health check futures. When any health check completes, it attempts to
     * find the highest-weighted healthy database to use as the initial primary. The connection future completes successfully
     * when a suitable primary is found, or exceptionally if all databases fail their health checks.
     *
     * @param databaseConfigs map of database configurations
     * @param healthStatusManager manager for tracking health status of all databases
     * @param databases map of successfully created database instances
     * @param databaseFutures map of futures for database creation
     * @param healthStatusFutures map of futures for health check results
     * @return a {@link CompletableFuture} that completes with the multi-database connection
     */
    CompletableFuture<StatefulRedisMultiDbConnection<K, V>> buildFuture(Map<RedisURI, DatabaseConfig> databaseConfigs,
            HealthStatusManager healthStatusManager, DatabaseMap<K, V> databases, DatabaseFutureMap<K, V> databaseFutures,
            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures) {

        CompletableFuture<StatefulRedisMultiDbConnection<K, V>> connectionFuture = new CompletableFuture<>();

        // Sort the database configs by weight in descending order
        List<DatabaseConfig> sortedConfigs = databaseConfigs.values().stream()
                .sorted(Comparator.comparingDouble(DatabaseConfig::getWeight).reversed()).collect(Collectors.toList());

        AtomicReference<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> initialDb = new AtomicReference<>();

        for (CompletableFuture<HealthStatus> healthStatusFuture : healthStatusFutures.values()) {
            healthStatusFuture.handle((healthStatus, throwable) -> {
                RedisDatabaseImpl<StatefulRedisConnection<K, V>> selected = findInitialDbCandidate(sortedConfigs, databases,
                        initialDb);
                if (selected != null) {
                    logger.info("Selected {} as primary database", selected);

                    StatefulRedisMultiDbConnection<K, V> conn = buildConn(healthStatusManager, databases, databaseFutures,
                            selected);

                    connectionFuture.complete(conn);
                } else {
                    // check if everything seems to be somehow failed at this point.
                    if (checkIfAllFailed(healthStatusFutures)) {
                        connectionFuture
                                .completeExceptionally(new RedisConnectionException("No healthy database available !!"));
                    }
                }
                return null;
            });
        }
        return connectionFuture;
    }

    /**
     * Builds the actual multi-database connection with the selected primary database.
     * <p>
     * This method creates a snapshot of the current database map and sets up completion handlers for any databases that are
     * still being created asynchronously. The connection is immediately usable with the selected primary database, and
     * additional databases will be added as they become available.
     *
     * @param healthStatusManager manager for tracking health status
     * @param databases map of currently available databases
     * @param databaseFutures map of futures for databases still being created
     * @param selected the database selected as the initial primary
     * @return a {@link StatefulRedisMultiDbConnection} ready for use
     */
    StatefulRedisMultiDbConnection<K, V> buildConn(HealthStatusManager healthStatusManager, DatabaseMap<K, V> databases,
            DatabaseFutureMap<K, V> databaseFutures, RedisDatabaseImpl<StatefulRedisConnection<K, V>> selected) {
        DatabaseMap<K, V> clone = new DatabaseMap<>(databases);

        List<CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> remainingDbFutures;
        remainingDbFutures = databaseFutures.entrySet().stream().filter(entry -> clone.containsKey(entry.getKey()))
                .map(entry -> entry.getValue()).collect(Collectors.toList());

        StatefulRedisMultiDbConnection<K, V> conn = multiDbConnectionFactory.create(selected, clone, codec, healthStatusManager,
                new RedisDatabaseAsyncCompletion<StatefulRedisConnection<K, V>>(remainingDbFutures));
        return conn;
    }

    /**
     * Creates asynchronous connection futures for all configured databases.
     * <p>
     * This method initiates parallel connection attempts to all configured Redis endpoints. Each connection is created
     * asynchronously and wrapped in a {@link RedisDatabaseImpl} that includes circuit breaker and health check support.
     *
     * @param databaseConfigs map of database configurations
     * @param databases map to populate with successfully created databases (populated asynchronously)
     * @param healthStatusManager manager for registering health checks
     * @return a map of futures for database creation, keyed by Redis URI
     */
    DatabaseFutureMap<K, V> createDatabaseFutures(Map<RedisURI, DatabaseConfig> databaseConfigs, DatabaseMap<K, V> databases,
            HealthStatusManager healthStatusManager) {

        // Create async database connections for all configured endpoints
        DatabaseFutureMap<K, V> databaseFutures = new DatabaseFutureMap<>(databaseConfigs.size());

        // Create async database connections for all configured endpoints
        for (Map.Entry<RedisURI, DatabaseConfig> entry : databaseConfigs.entrySet()) {
            RedisURI uri = entry.getKey();
            DatabaseConfig config = entry.getValue();

            databaseFutures.put(uri, createRedisDatabaseAsync(config, healthStatusManager));
        }
        return databaseFutures;
    }

    /**
     * Creates a single Redis database connection asynchronously.
     * <p>
     * This method:
     * <ol>
     * <li>Establishes an async connection to the Redis endpoint</li>
     * <li>Registers a health check if configured</li>
     * <li>Extracts the database endpoint for circuit breaker binding</li>
     * <li>Creates a circuit breaker with the configured settings</li>
     * <li>Wraps everything in a {@link RedisDatabaseImpl}</li>
     * </ol>
     * If any step fails, the connection is closed and the future completes exceptionally.
     *
     * @param config the database configuration
     * @param healthStatusManager manager for registering health checks
     * @return a future that completes with the database instance or exceptionally on failure
     */
    CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> createRedisDatabaseAsync(DatabaseConfig config,
            HealthStatusManager healthStatusManager) {

        RedisURI uri = config.getRedisURI();
        client.setOptions(config.getClientOptions());

        try {
            // Use the async connect method from RedisClient
            ConnectionFuture<StatefulRedisConnection<K, V>> connectionFuture = client.connectAsync(codec, uri);
            // Reset options immediately after connectAsync() call
            client.resetOptions();

            return connectionFuture.toCompletableFuture().thenApply(connection -> {
                try {

                    HealthCheck healthCheck = null;
                    if (HealthCheckStrategySupplier.NO_HEALTH_CHECK != config.getHealthCheckStrategySupplier()) {
                        HealthCheckStrategy hcStrategy = config.getHealthCheckStrategySupplier().get(config.getRedisURI(),
                                new DatabaseRawConnectionFactoryImpl(config.getClientOptions(), client));
                        healthCheck = healthStatusManager.add(uri, hcStrategy);
                    }

                    DatabaseEndpoint databaseEndpoint = extractDatabaseEndpoint(connection);
                    CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(config.getCircuitBreakerConfig());
                    databaseEndpoint.bind(circuitBreaker);

                    RedisDatabaseImpl<StatefulRedisConnection<K, V>> database = new RedisDatabaseImpl<>(config, connection,
                            databaseEndpoint, circuitBreaker, healthCheck);
                    if (logger.isInfoEnabled()) {
                        logger.info("Created database: {} with CircuitBreaker {} and HealthCheck {}", database.getId(),
                                circuitBreaker.getId(), healthCheck != null ? healthCheck.getEndpoint() : "N/A");
                    }
                    return database;
                } catch (Exception e) {
                    // If database setup fails, close the connection
                    connection.closeAsync();
                    throw e;
                }
            });
        } catch (Exception e) {
            client.resetOptions();
            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    /**
     * Creates futures that track health check results for all databases.
     * <p>
     * For each database being created, this method sets up a future that will complete when the database's health status is
     * determined. Databases without health checks are immediately considered healthy. This allows the connection builder to
     * wait for health check results before selecting an initial primary database.
     *
     * @param databaseFutures map of database creation futures
     * @param healthStatusManager manager for tracking health status
     * @return a map of health status futures, keyed by Redis URI
     */
    Map<RedisURI, CompletableFuture<HealthStatus>> createHealthStatusFutures(DatabaseFutureMap<K, V> databaseFutures,
            HealthStatusManager healthStatusManager) {

        StatusTracker statusTracker = new StatusTracker(healthStatusManager, resources);

        // Create a map of futures, one for each database's health status
        Map<RedisURI, CompletableFuture<HealthStatus>> healthCheckFutures = new HashMap<>();

        for (Map.Entry<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> entry : databaseFutures
                .entrySet()) {

            RedisURI endpoint = entry.getKey();
            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> dbFuture = entry.getValue();

            CompletableFuture<HealthStatus> healthCheckFuture = dbFuture.thenCompose(database -> {
                // Check if health checks are enabled for this database
                if (database.getHealthCheck() != null) {
                    logger.info("Health checks enabled for {}, waiting for result", endpoint);
                    // Wait asynchronously for this database's health status to be determined
                    return statusTracker.waitForHealthStatusAsync(endpoint);
                } else {
                    // No health check configured - assume healthy
                    logger.info("No health check configured for database {}, defaulting to HEALTHY", endpoint);
                    return CompletableFuture.completedFuture(HealthStatus.HEALTHY);
                }
            });
            healthCheckFutures.put(endpoint, healthCheckFuture);
        }
        return healthCheckFutures;
    }

    /**
     * Finds the best candidate for the initial primary database based on weight and health status.
     * <p>
     * This method iterates through databases in descending order of weight and selects the first one that:
     * <ul>
     * <li>Has successfully connected</li>
     * <li>Has completed its health check (if configured)</li>
     * <li>Is healthy (or has no health check)</li>
     * </ul>
     * The selection is atomic to ensure only one database is selected even if multiple threads call this method concurrently.
     *
     * @param sortedConfigs list of database configurations sorted by weight (descending)
     * @param databases map of available database instances
     * @param initialDb atomic reference for storing the selected database
     * @return the selected database, or {@code null} if no suitable candidate is available yet
     */
    RedisDatabaseImpl<StatefulRedisConnection<K, V>> findInitialDbCandidate(List<DatabaseConfig> sortedConfigs,
            DatabaseMap<K, V> databases, AtomicReference<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> initialDb) {

        for (DatabaseConfig config : sortedConfigs) {
            RedisDatabaseImpl<StatefulRedisConnection<K, V>> database = databases.get(config.getRedisURI());

            // we will skip if most weighted database is not yet complete with connection creation
            if (database == null) {
                return null;
            }

            // this means we have a connection for most weighted one but not yet received a health check result.
            // this is a bit awkward expression below; its purely due to NO_HEALTH_CHECK configuration results with UNKNOWN
            if (database.getHealthCheck() != null && database.getHealthCheckStatus() == HealthStatus.UNKNOWN) {
                return null;
            }

            // we have a connection and health check result where all prior(more weighted) databases are unhealthy.
            // So this one is the best bet we have so far.
            if (database.getHealthCheck() == null || database.getHealthCheckStatus().isHealthy()) {
                if (initialDb.compareAndSet(null, database)) {
                    return database;
                }
            }
        }
        return null;
    }

    /**
     * Checks if all databases have failed their health checks.
     * <p>
     * This method returns {@code true} only if:
     * <ul>
     * <li>All health check futures have completed</li>
     * <li>None of the databases are healthy</li>
     * </ul>
     * This is used to determine when to fail the connection attempt rather than continuing to wait for health checks.
     *
     * @param healthStatusFutures map of health status futures
     * @return {@code true} if all databases have completed health checks and none are healthy, {@code false} otherwise
     */
    boolean checkIfAllFailed(Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures) {
        // check if all health checks completed, if not lets wait more.
        boolean allHealthChecksCompleted = healthStatusFutures.values().stream().allMatch(CompletableFuture::isDone);
        if (allHealthChecksCompleted) {

            // check if none of the databases are healthy, no need to wait more, just fail.
            boolean noneHealthy = healthStatusFutures.values().stream()
                    .filter(singleFuture -> !singleFuture.isCompletedExceptionally())
                    .map(singleFuture -> singleFuture.getNow(null)).noneMatch(status -> status.isHealthy());

            if (noneHealthy) {
                // here it means we have all databases completed and all health checks completed,
                // and none of them are healthy.
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts the {@link DatabaseEndpoint} from a Redis connection.
     * <p>
     * This method unwraps the connection's channel writer to access the underlying database endpoint, which is needed for
     * circuit breaker binding.
     *
     * @param connection the Redis connection
     * @return the database endpoint
     */
    DatabaseEndpoint extractDatabaseEndpoint(StatefulRedisConnection<?, ?> connection) {
        RedisChannelWriter writer = ((StatefulRedisConnectionImpl<?, ?>) connection).getChannelWriter();
        if (writer instanceof Delegating) {
            writer = (RedisChannelWriter) ((Delegating<?>) writer).unwrap();
        }
        return (DatabaseEndpoint) writer;
    }

    /**
     * Creates a new health status manager for tracking database health.
     *
     * @return a new {@link HealthStatusManager} instance
     */
    protected HealthStatusManager createHealthStatusManager() {
        return new HealthStatusManagerImpl();
    }

    /**
     * Functional interface for creating multi-database connections.
     * <p>
     * This factory is used to create the actual {@link StatefulRedisMultiDbConnection} instance with the selected primary
     * database and all available databases.
     */
    @FunctionalInterface
    interface MultiDbConnectionFactory {

        /**
         * Creates a multi-database connection.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param selected the database selected as the initial primary
         * @param databases map of all available databases
         * @param codec the codec for encoding/decoding
         * @param healthStatusManager the health status manager
         * @param completion handler for databases that complete asynchronously
         * @return a new multi-database connection
         */
        <K, V> StatefulRedisMultiDbConnection<K, V> create(RedisDatabaseImpl<StatefulRedisConnection<K, V>> selected,
                Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> databases, RedisCodec<K, V> codec,
                HealthStatusManager healthStatusManager,
                RedisDatabaseAsyncCompletion<StatefulRedisConnection<K, V>> completion);

    }

    /**
     * Implementation of {@link DatabaseRawConnectionFactory} for creating raw connections to databases.
     * <p>
     * This factory is used by health check strategies to create dedicated connections for health checking, separate from the
     * main application connections.
     */
    private static class DatabaseRawConnectionFactoryImpl implements DatabaseRawConnectionFactory {

        private final io.lettuce.core.ClientOptions clientOptions;

        private final MultiDbClientImpl client;

        /**
         * Creates a new raw connection factory.
         *
         * @param clientOptions the client options to use for connections
         * @param client the multi-database client
         */
        public DatabaseRawConnectionFactoryImpl(io.lettuce.core.ClientOptions clientOptions, MultiDbClientImpl client) {
            this.clientOptions = clientOptions;
            this.client = client;
        }

        @Override
        public StatefulRedisConnection<?, ?> connectToDatabase(RedisURI endpoint) {
            client.setOptions(clientOptions);
            try {
                return client.connect(endpoint);
            } finally {
                client.resetOptions();
            }
        }

    }

    /**
     * Completion handler for databases that are created asynchronously after the initial connection is established.
     * <p>
     * This class allows the multi-database connection to register callbacks that will be invoked when additional databases
     * complete their connection and health check process.
     *
     * @param <C> the connection type
     */
    static class RedisDatabaseAsyncCompletion<C extends StatefulRedisConnection<?, ?>> {

        private final List<CompletableFuture<RedisDatabaseImpl<C>>> databaseFutures;

        /**
         * Creates a new async completion handler.
         *
         * @param databaseFutures list of futures for databases being created
         */
        RedisDatabaseAsyncCompletion(List<CompletableFuture<RedisDatabaseImpl<C>>> databaseFutures) {
            this.databaseFutures = databaseFutures;
        }

        /**
         * Registers a callback to be invoked when each database future completes.
         *
         * @param action the callback to invoke with the database instance or exception
         */
        void whenComplete(BiConsumer<RedisDatabaseImpl<C>, Throwable> action) {
            databaseFutures.forEach(future -> future.whenComplete(action));
        }

    }

    static class DatabaseMap<K, V> extends ConcurrentHashMap<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> {

        public DatabaseMap() {
            super();
        }

        public DatabaseMap(int initialCapacity) {
            super(initialCapacity);
        }

        public DatabaseMap(Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> map) {
            super(map);
        }

    }

    static class DatabaseFutureMap<K, V>
            extends ConcurrentHashMap<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> {

        public DatabaseFutureMap() {
            super();
        }

        public DatabaseFutureMap(int initialCapacity) {
            super(initialCapacity);
        }

        public DatabaseFutureMap(Map<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> map) {
            super(map);
        }

    }

}
