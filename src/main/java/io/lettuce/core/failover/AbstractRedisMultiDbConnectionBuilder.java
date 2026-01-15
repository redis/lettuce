package io.lettuce.core.failover;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.Delegating;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.BaseRedisMultiDbConnection;
import io.lettuce.core.failover.health.HealthCheck;
import io.lettuce.core.failover.health.HealthCheckStrategy;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.failover.health.HealthStatusManagerImpl;
import io.lettuce.core.internal.Exceptions;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Abstract builder for creating asynchronous multi-database Redis connections with health checking and failover support.
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
 * @param <MC> the multi-database connection type
 * @param <SC> the underlying single connection type (StatefulRedisConnection or StatefulRedisPubSubConnection)
 * @param <K> the key type
 * @param <V> the value type
 * @author Lettuce Contributors
 */
abstract class AbstractRedisMultiDbConnectionBuilder<MC extends BaseRedisMultiDbConnection, SC extends StatefulRedisConnection<K, V>, K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractRedisMultiDbConnectionBuilder.class);

    protected final MultiDbClientImpl client;

    protected final ClientResources resources;

    protected final RedisCodec<K, V> codec;

    /**
     * Creates a new {@link AbstractRedisMultiDbConnectionBuilder}.
     *
     * @param client the multi-database client instance
     * @param resources the client resources for event loops and thread pools
     * @param codec the codec for encoding/decoding keys and values
     */
    AbstractRedisMultiDbConnectionBuilder(MultiDbClientImpl client, ClientResources resources, RedisCodec<K, V> codec) {
        this.resources = resources;
        this.client = client;
        this.codec = codec;
    }

    /**
     * Creates a standalone connection to a single Redis database. Subclasses implement this to provide either regular or PubSub
     * connections.
     *
     * @param codec the codec for encoding/decoding
     * @param uri the Redis URI to connect to
     * @return a future that completes with the connection
     */
    protected abstract ConnectionFuture<SC> connectAsync(RedisCodec<K, V> codec, RedisURI uri);

    /**
     * Creates a multi-database connection wrapper. Subclasses implement this to provide either regular or PubSub multi-database
     * connections.
     *
     * @param selected the initially selected database
     * @param databases map of all available databases
     * @param codec the codec for encoding/decoding
     * @param healthStatusManager the health status manager
     * @param completion handler for async database completion
     * @return the multi-database connection
     */
    protected abstract MC createMultiDbConnection(RedisDatabaseImpl<SC> selected,
            Map<RedisURI, RedisDatabaseImpl<SC>> databases, RedisCodec<K, V> codec, HealthStatusManager healthStatusManager,
            RedisDatabaseAsyncCompletion<SC> completion);

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
     * @return a {@link CompletableFuture} that completes with a multi-database connection when at least one healthy database is
     *         available, or completes exceptionally if all databases fail
     */
    CompletableFuture<MC> connectAsync(Map<RedisURI, DatabaseConfig> databaseConfigs) {

        HealthStatusManager healthStatusManager = createHealthStatusManager();

        // Create a map to hold the final database instances
        DatabaseMap<SC> databases = new DatabaseMap<>(databaseConfigs.size());

        // Create async database connections for all configured endpoints
        DatabaseFutureMap<SC> databaseFutures = createDatabaseFutures(databaseConfigs, databases, healthStatusManager);

        // Create a map of futures, one for each database's health status
        Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = createHealthStatusFutures(databaseFutures,
                healthStatusManager);

        // Build the final connection future
        CompletableFuture<MC> connectionFuture = buildFuture(databaseConfigs, healthStatusManager, databases, databaseFutures,
                healthStatusFutures);

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
    CompletableFuture<MC> buildFuture(Map<RedisURI, DatabaseConfig> databaseConfigs, HealthStatusManager healthStatusManager,
            DatabaseMap<SC> databases, DatabaseFutureMap<SC> databaseFutures,
            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures) {

        CompletableFuture<MC> connectionFuture = new CompletableFuture<>();

        // Sort the database configs by weight in descending order
        List<DatabaseConfig> sortedConfigs = databaseConfigs.values().stream()
                .sorted(Comparator.comparingDouble(DatabaseConfig::getWeight).reversed()).collect(Collectors.toList());

        AtomicReference<RedisDatabaseImpl<SC>> initialDb = new AtomicReference<>();

        for (CompletableFuture<HealthStatus> healthStatusFuture : healthStatusFutures.values()) {
            healthStatusFuture.handle((healthStatus, throwable) -> {

                MC conn = null;
                Exception capturedFailure = null;
                RedisDatabaseImpl<SC> selected = findInitialDbCandidate(sortedConfigs, databases, initialDb);
                try {
                    if (selected != null) {
                        logger.info("Selected {} as primary database", selected);
                        conn = buildConn(healthStatusManager, databases, databaseFutures, selected);
                        connectionFuture.complete(conn);
                    }
                } catch (Exception e) {
                    capturedFailure = e;
                } finally {
                    // if we dont have the connection then its either
                    // - no selected db yet
                    // - or attempted to build connection but failed.
                    // in both cases we need to check if all failed, and complete the future accordingly.
                    if (conn == null) {
                        // check if everything seems to be somehow failed at this point.
                        if (checkIfAllFailed(healthStatusFutures)) {
                            connectionFuture.completeExceptionally(capturedFailure != null ? capturedFailure
                                    : new RedisConnectionException("No healthy database available !!"));
                        }
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
     * @return a multi-database connection ready for use
     */
    MC buildConn(HealthStatusManager healthStatusManager, DatabaseMap<SC> databases, DatabaseFutureMap<SC> databaseFutures,
            RedisDatabaseImpl<SC> selected) {
        DatabaseMap<SC> clone = new DatabaseMap<>(databases);

        List<CompletableFuture<RedisDatabaseImpl<SC>>> remainingDbFutures;
        remainingDbFutures = databaseFutures.entrySet().stream().filter(entry -> clone.containsKey(entry.getKey()))
                .map(entry -> entry.getValue()).collect(Collectors.toList());

        RedisDatabaseAsyncCompletion<SC> completion = new RedisDatabaseAsyncCompletion<SC>(remainingDbFutures);
        return createMultiDbConnection(selected, clone, codec, healthStatusManager, completion);
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
    DatabaseFutureMap<SC> createDatabaseFutures(Map<RedisURI, DatabaseConfig> databaseConfigs, DatabaseMap<SC> databases,
            HealthStatusManager healthStatusManager) {

        // Create async database connections for all configured endpoints
        DatabaseFutureMap<SC> databaseFutures = new DatabaseFutureMap<>(databaseConfigs.size());

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
    CompletableFuture<RedisDatabaseImpl<SC>> createRedisDatabaseAsync(DatabaseConfig config,
            HealthStatusManager healthStatusManager) {

        RedisURI uri = config.getRedisURI();
        client.setOptions(config.getClientOptions());

        try {
            // Use the async connect method
            ConnectionFuture<SC> connectionFuture = connectAsync(codec, uri);
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

                    RedisDatabaseImpl<SC> database = new RedisDatabaseImpl<>(config, connection, databaseEndpoint,
                            circuitBreaker, healthCheck);
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
            CompletableFuture<RedisDatabaseImpl<SC>> failedFuture = new CompletableFuture<>();
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
    Map<RedisURI, CompletableFuture<HealthStatus>> createHealthStatusFutures(DatabaseFutureMap<SC> databaseFutures,
            HealthStatusManager healthStatusManager) {

        StatusTracker statusTracker = new StatusTracker(healthStatusManager, resources);

        // Create a map of futures, one for each database's health status
        Map<RedisURI, CompletableFuture<HealthStatus>> healthCheckFutures = new HashMap<>();

        for (Map.Entry<RedisURI, CompletableFuture<RedisDatabaseImpl<SC>>> entry : databaseFutures.entrySet()) {

            RedisURI endpoint = entry.getKey();
            CompletableFuture<RedisDatabaseImpl<SC>> dbFuture = entry.getValue();

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
    RedisDatabaseImpl<SC> findInitialDbCandidate(List<DatabaseConfig> sortedConfigs, DatabaseMap<SC> databases,
            AtomicReference<RedisDatabaseImpl<SC>> initialDb) {

        for (DatabaseConfig config : sortedConfigs) {
            RedisDatabaseImpl<SC> database = databases.get(config.getRedisURI());

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
     * Creates a Redis database synchronously by blocking on the async creation. Used as a {@link DatabaseConnectionFactory} for
     * dynamic database addition.
     *
     * @param config the database configuration
     * @param codec the codec to use for encoding/decoding
     * @param healthStatusManager manager for registering health checks
     * @return a new RedisDatabase instance
     */
    protected RedisDatabaseImpl<SC> createRedisDatabase(DatabaseConfig config, RedisCodec<K, V> codec,
            HealthStatusManager healthStatusManager) {

        CompletableFuture<RedisDatabaseImpl<SC>> future = createRedisDatabaseAsync(config, healthStatusManager);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw RedisConnectionException.create(e);
        } catch (Exception e) {
            throw RedisConnectionException.create(Exceptions.unwrap(e));
        }
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
     * Functional interface for creating single database connections.
     * <p>
     * This factory is used to create individual connections to Redis endpoints.
     *
     * @param <C> the connection type
     * @param <K> the key type
     * @param <V> the value type
     */
    @FunctionalInterface
    interface StandaloneConnectionFactory<C extends StatefulRedisConnection<K, V>, K, V> {

        /**
         * Creates an asynchronous connection to a Redis endpoint.
         *
         * @param codec the codec for encoding/decoding
         * @param uri the Redis URI to connect to
         * @return a future that completes with the connection
         */
        ConnectionFuture<C> connectAsync(RedisCodec<K, V> codec, RedisURI uri);

    }

    /**
     * Functional interface for creating multi-database connections.
     * <p>
     * This factory is used to create the actual {@link StatefulRedisMultiDbConnection} instance with the selected primary
     * database and all available databases.
     *
     * @param <MC> the multi-database connection type
     * @param <SC> the single database connection type
     * @param <K> the key type
     * @param <V> the value type
     */
    @FunctionalInterface
    interface MultiDbConnectionFactory<MC extends BaseRedisMultiDbConnection, SC extends StatefulRedisConnection<K, V>, K, V> {

        /**
         * Creates a multi-database connection.
         *
         * @param selected the database selected as the initial primary
         * @param databases map of all available databases
         * @param codec the codec for encoding/decoding
         * @param healthStatusManager the health status manager
         * @param completion handler for databases that complete asynchronously
         * @return a new multi-database connection
         */
        MC create(RedisDatabaseImpl<SC> selected, Map<RedisURI, RedisDatabaseImpl<SC>> databases, RedisCodec<K, V> codec,
                HealthStatusManager healthStatusManager, RedisDatabaseAsyncCompletion<SC> completion);

    }

    static class DatabaseMap<SC extends StatefulRedisConnection<?, ?>>
            extends ConcurrentHashMap<RedisURI, RedisDatabaseImpl<SC>> {

        public DatabaseMap() {
            super();
        }

        public DatabaseMap(int initialCapacity) {
            super(initialCapacity);
        }

        public DatabaseMap(Map<RedisURI, RedisDatabaseImpl<SC>> map) {
            super(map);
        }

    }

    static class DatabaseFutureMap<SC extends StatefulRedisConnection<?, ?>>
            extends ConcurrentHashMap<RedisURI, CompletableFuture<RedisDatabaseImpl<SC>>> {

        public DatabaseFutureMap() {
            super();
        }

        public DatabaseFutureMap(int initialCapacity) {
            super(initialCapacity);
        }

        public DatabaseFutureMap(Map<RedisURI, CompletableFuture<RedisDatabaseImpl<SC>>> map) {
            super(map);
        }

    }

}
