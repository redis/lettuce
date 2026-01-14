package io.lettuce.core.failover;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Manages asynchronous connection creation and health checking for multiple Redis databases. This class handles the complex
 * async logic of establishing connections to multiple databases, waiting for health checks, and filtering out unhealthy or
 * failed connections.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Ali Takavci
 * @since 7.4
 */
class MultiDbAsyncConnectionBuilder<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultiDbAsyncConnectionBuilder.class);

    private final HealthStatusManager healthStatusManager;

    private final ClientResources resources;

    private final MultiDbClientImpl client;

    /**
     * Creates a new MultiDbAsyncConnectionBuilder.
     *
     * @param healthStatusManager the health status manager to use for tracking database health
     * @param resources the client resources
     * @param client the MultiDbClient instance for creating connections
     */
    MultiDbAsyncConnectionBuilder(HealthStatusManager healthStatusManager, ClientResources resources,
            MultiDbClientImpl client) {
        this.healthStatusManager = healthStatusManager;
        this.resources = resources;
        this.client = client;
    }

    /**
     * Asynchronously creates connections to all configured databases and waits for initial health checks.
     *
     * @param databaseConfigs the database configurations
     * @param codec the Redis codec to use
     * @param multiDbConnectionFactory function to create the final multi-db connection wrapper
     * @return a CompletableFuture that completes with the multi-db connection
     */
    CompletableFuture<StatefulRedisMultiDbConnection<K, V>> connectAsync(Map<RedisURI, DatabaseConfig> databaseConfigs,
            RedisCodec<K, V> codec, MultiDbConnectionFactory<K, V> multiDbConnectionFactory) {

        CompletableFuture<StatefulRedisMultiDbConnection<K, V>> connectionFuture = new CompletableFuture<>();

        // Create async database connections for all configured endpoints
        Map<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> databaseFutures = new ConcurrentHashMap<>(
                databaseConfigs.size());

        for (Map.Entry<RedisURI, DatabaseConfig> entry : databaseConfigs.entrySet()) {
            RedisURI uri = entry.getKey();
            DatabaseConfig config = entry.getValue();

            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> dbFuture = createRedisDatabaseAsync(config,
                    codec);
            databaseFutures.put(uri, dbFuture);
        }

        // Wait for all database connections to complete (both successful and failed)
        CompletableFuture<Void> allDatabasesFuture = CompletableFuture
                .allOf(databaseFutures.values().toArray(new CompletableFuture[0]));

        // Handle completion with partial success support
        allDatabasesFuture.handle((v, throwable) -> {

            CompletableFuture<Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> result = handleDatabaseFutures(
                    databaseFutures);

            result.handle((healthyDatabaseMap, exc) -> {
                if (exc != null) {
                    connectionFuture.completeExceptionally(exc);
                } else if (healthyDatabaseMap != null && !healthyDatabaseMap.isEmpty()) {
                    // Create the multi-db connection wrapper with only healthy and open databases
                    connectionFuture.complete(multiDbConnectionFactory.create(healthyDatabaseMap, codec, healthStatusManager));
                } else {
                    // this should not happen ever
                    connectionFuture.completeExceptionally(new IllegalStateException(
                            "Healthy database map is empty without any errors propagated! This should be investigated."));
                }
                return null;
            });
            return null;
        });

        return connectionFuture;
    }

    /**
     * Asynchronously creates a Redis database connection with circuit breaker and health check support.
     *
     * @param config the database configuration
     * @param codec the codec to use for encoding/decoding
     * @return a CompletableFuture that completes with the created database
     */
    CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> createRedisDatabaseAsync(DatabaseConfig config,
            RedisCodec<K, V> codec) {

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
     * Handles the completion of individual database futures, collecting successful connections and closing failed ones. Also
     * waits for initial health checks to complete and filters out unhealthy databases and closed connections.
     *
     * @param databaseFutures the map of database futures
     * @return a CompletableFuture that completes with the map of healthy databases
     */
    CompletableFuture<Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> handleDatabaseFutures(
            Map<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> databaseFutures) {

        CompletableFuture<Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> healthyDatabasesFuture = new CompletableFuture<>();
        try {

            // Collect successfully created databases, skip failed ones
            Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> databases = collectDatabasesWithEstablishedConnections(
                    databaseFutures);

            // Wait for initial health checks asynchronously
            CompletableFuture<Map<RedisURI, HealthStatus>> healthCheckFuture = collectHealthStatuses(databases);

            healthCheckFuture.whenComplete((healthStatuses, ht) -> {
                if (ht != null) {
                    // Health check failed, close all connections
                    databases.values().forEach(db -> db.getConnection().closeAsync());
                    healthyDatabasesFuture.completeExceptionally(ht);
                    return;
                }

                healthyDatabasesFuture.complete(databases);
            });

        } catch (Exception e) {
            // Unexpected error during connection creation
            logger.error("Unexpected error during async connection", e);
            databaseFutures.values().forEach(dbFuture -> {
                if (dbFuture.isDone() && !dbFuture.isCompletedExceptionally()) {
                    dbFuture.thenAccept(db -> db.getConnection().closeAsync());
                }
            });
            healthyDatabasesFuture.completeExceptionally(e);

        }

        return healthyDatabasesFuture;
    }

    /**
     * Collects all databases that have successfully established connections. If no database has successfully connected, throws
     * a {@link RedisConnectionException} exception with all failures as suppressed exceptions. If at least one database has
     * successfully connected, logs a warning for each failed connection and proceeds.
     *
     * @param databaseFutures the map of database futures
     * @return a map of successfully connected databases
     */
    Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> collectDatabasesWithEstablishedConnections(
            Map<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> databaseFutures) {
        // Collect successfully created databases, skip failed ones
        Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> databases = new ConcurrentHashMap<>();
        List<Throwable> failures = new ArrayList<>();

        for (Map.Entry<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> entry : databaseFutures
                .entrySet()) {
            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> dbFuture = entry.getValue();

            try {
                RedisDatabaseImpl<StatefulRedisConnection<K, V>> database = dbFuture.get();
                databases.put(entry.getKey(), database);
            } catch (Exception e) {
                // Failed to get database, record the failure
                failures.add(e);
                logger.warn("Failed to create database connection for {}: {}", entry.getKey(), e.getMessage());
            }
        }

        // Check if we have at least one successful connection
        if (databases.isEmpty()) {
            // All connections failed - fail the entire operation
            logger.error("All database connections failed. Total failures: {}", failures.size());

            // Create a composite exception with all failures
            RedisConnectionException compositeException = new RedisConnectionException(
                    "Failed to connect to any database. All " + databaseFutures.size() + " connection(s) failed.");
            failures.forEach(compositeException::addSuppressed);
            throw compositeException;
        } else {
            // We have at least one successful connection - proceed
            logger.info("Successfully connected to {} out of {} database(s)", databases.size(), databaseFutures.size());
            if (!failures.isEmpty()) {
                logger.info("{} database connection(s) failed but proceeding with successful ones", failures.size());
            }

        }
        return databases;
    }

    /**
     * Asynchronously waits for all successfully connected databases to have their health status determined. Once all health
     * statuses are known, selects the most weighted healthy database.
     *
     * @param databases the map of databases to check
     * @return a CompletableFuture that completes with a map of health statuses when all are determined
     */
    CompletableFuture<Map<RedisURI, HealthStatus>> collectHealthStatuses(
            Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> databases) {

        logger.info("Waiting for health status of {} successfully connected databases", databases.size());

        StatusTracker statusTracker = new StatusTracker(healthStatusManager, resources);

        // Create a list of futures, one for each database's health status
        List<CompletableFuture<Map.Entry<RedisURI, HealthStatus>>> healthCheckFutures = new ArrayList<>();

        for (Map.Entry<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> entry : databases.entrySet()) {
            RedisURI endpoint = entry.getKey();
            RedisDatabaseImpl<StatefulRedisConnection<K, V>> database = entry.getValue();

            CompletableFuture<HealthStatus> healthFuture;

            // Check if health checks are enabled for this database
            if (database.getHealthCheck() != null) {
                logger.info("Health checks enabled for {}, waiting for result", endpoint);
                // Wait asynchronously for this database's health status to be determined
                healthFuture = statusTracker.waitForHealthStatusAsync(endpoint);
            } else {
                // No health check configured - assume healthy
                logger.info("No health check configured for database {}, defaulting to HEALTHY", endpoint);
                healthFuture = CompletableFuture.completedFuture(HealthStatus.HEALTHY);
            }

            // Transform to include the endpoint with the health status
            CompletableFuture<Map.Entry<RedisURI, HealthStatus>> entryFuture = healthFuture
                    .thenApply(status -> new AbstractMap.SimpleEntry<>(endpoint, status));

            healthCheckFutures.add(entryFuture);
        }

        // Wait for all health checks to complete
        CompletableFuture<Void> allHealthChecksFuture = CompletableFuture
                .allOf(healthCheckFutures.toArray(new CompletableFuture[0]));

        // Once all health checks are done, collect and return the health statuses
        return allHealthChecksFuture.thenApply(v -> {
            // Collect all health statuses
            Map<RedisURI, HealthStatus> healthStatuses = new HashMap<>();
            for (CompletableFuture<Map.Entry<RedisURI, HealthStatus>> future : healthCheckFutures) {
                Map.Entry<RedisURI, HealthStatus> entry = future.join();
                healthStatuses.put(entry.getKey(), entry.getValue());
                logger.info("Database {} health status: {}", entry.getKey(), entry.getValue());
            }

            // Find the most weighted healthy database
            RedisDatabaseImpl<StatefulRedisConnection<K, V>> selectedDatabase = databases.entrySet().stream()
                    .filter(entry -> healthStatuses.get(entry.getKey()).isHealthy())
                    .max(Comparator.comparing(entry -> entry.getValue().getWeight())).map(Map.Entry::getValue).orElse(null);

            if (selectedDatabase == null) {
                throw new RedisConnectionException(
                        "All configured databases are unhealthy. Health statuses: " + healthStatuses);
            }

            logger.info("Selected database with weight {} as initial active database", selectedDatabase.getWeight());
            return healthStatuses;
        });
    }

    /**
     * Extracts the database endpoint from a Redis connection.
     *
     * @param connection the Redis connection
     * @return the database endpoint
     */
    private DatabaseEndpoint extractDatabaseEndpoint(StatefulRedisConnection<?, ?> connection) {
        RedisChannelWriter writer = ((StatefulRedisConnectionImpl<?, ?>) connection).getChannelWriter();
        if (writer instanceof Delegating) {
            writer = (RedisChannelWriter) ((Delegating<?>) writer).unwrap();
        }
        return (DatabaseEndpoint) writer;
    }

    /**
     * Factory interface for creating multi-database connections.
     *
     * @param <K> Key type
     * @param <V> Value type
     */
    @FunctionalInterface
    interface MultiDbConnectionFactory<K, V> {

        StatefulRedisMultiDbConnection<K, V> create(
                Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> healthyDatabaseMap, RedisCodec<K, V> codec,
                HealthStatusManager healthStatusManager);

    }

    /**
     * Implementation of DatabaseRawConnectionFactory that uses the MultiDbClient.
     */
    private static class DatabaseRawConnectionFactoryImpl implements DatabaseRawConnectionFactory {

        private final io.lettuce.core.ClientOptions clientOptions;

        private final MultiDbClientImpl client;

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

}
