/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.lettuce.core.failover;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionFuture;
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
        return new StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<K, V>, K, V>(databases, getResources(), codec,
                this::createRedisDatabase, healthStatusManager);
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
     * Asynchronously creates a Redis database connection with circuit breaker and health check support.
     *
     * @param config the database configuration
     * @param codec the codec to use for encoding/decoding
     * @param healthStatusManager the health status manager
     * @return a CompletableFuture that completes with the created database
     */
    private <K, V> CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> createRedisDatabaseAsync(
            DatabaseConfig config, RedisCodec<K, V> codec, HealthStatusManager healthStatusManager) {

        RedisURI uri = config.getRedisURI();
        setOptions(config.getClientOptions());

        try {
            // Use the async connect method from RedisClient
            ConnectionFuture<StatefulRedisConnection<K, V>> connectionFuture = connectAsync(codec, uri);
            resetOptions();
            return connectionFuture.toCompletableFuture().thenApply(connection -> {
                try {

                    HealthCheck healthCheck = null;
                    if (HealthCheckStrategySupplier.NO_HEALTH_CHECK != config.getHealthCheckStrategySupplier()) {
                        HealthCheckStrategy hcStrategy = config.getHealthCheckStrategySupplier().get(config.getRedisURI(),
                                new DatabaseRawConnectionFactoryImpl(config.getClientOptions()));
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
            resetOptions();
            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    /**
     * Asynchronously waits for all successfully connected databases to have their health status determined. Once all health
     * statuses are known, selects the most weighted healthy database.
     *
     * @param healthStatusManager the health status manager
     * @param databases the map of databases to check
     * @return a CompletableFuture that completes with a map of health statuses when all are determined
     */
    private <K, V> CompletableFuture<Map<RedisURI, HealthStatus>> waitForInitialHealthyDatabaseAsync(
            HealthStatusManager healthStatusManager,
            Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> databases) {

        logger.info("Waiting for health status of {} successfully connected databases", databases.size());

        StatusTracker statusTracker = new StatusTracker(healthStatusManager, getResources());

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

    @Override
    public <K, V> ConnectionFuture<StatefulRedisMultiDbConnection<K, V>> connectAsync(RedisCodec<K, V> codec) {

        if (codec == null) {
            throw new IllegalArgumentException("codec must not be null");
        }

        HealthStatusManager healthStatusManager = createHealthStatusManager();

        // Create a CompletableFuture to track the overall connection process
        CompletableFuture<StatefulRedisMultiDbConnection<K, V>> connectionFuture = new CompletableFuture<>();

        // Create async database connections for all configured endpoints
        Map<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> databaseFutures = new ConcurrentHashMap<>(
                databaseConfigs.size());

        for (Map.Entry<RedisURI, DatabaseConfig> entry : databaseConfigs.entrySet()) {
            RedisURI uri = entry.getKey();
            DatabaseConfig config = entry.getValue();

            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> dbFuture = createRedisDatabaseAsync(config,
                    codec, healthStatusManager);
            databaseFutures.put(uri, dbFuture);
        }

        // Wait for all database connections to complete (both successful and failed)
        // Use allOf() which waits for all futures to complete, regardless of success or failure
        CompletableFuture<Void> allDatabasesFuture = CompletableFuture
                .allOf(databaseFutures.values().toArray(new CompletableFuture[0]));

        // Handle completion with partial success support
        allDatabasesFuture.handle((v, throwable) -> {
            // proceed with successful connections

            try {
                // Collect successfully created databases, skip failed ones
                Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> databases = new ConcurrentHashMap<>();
                List<Throwable> failures = new ArrayList<>();

                for (Map.Entry<RedisURI, CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>>> entry : databaseFutures
                        .entrySet()) {
                    CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<K, V>>> dbFuture = entry.getValue();

                    if (dbFuture.isDone() && !dbFuture.isCompletedExceptionally()) {
                        try {
                            RedisDatabaseImpl<StatefulRedisConnection<K, V>> database = dbFuture.get();
                            databases.put(entry.getKey(), database);
                        } catch (Exception e) {
                            // Failed to get database, record the failure
                            failures.add(e);
                            if (logger.isWarnEnabled()) {
                                logger.warn("Failed to create database connection for {}: {}", entry.getKey(), e.getMessage());
                            }
                        }
                    } else if (dbFuture.isCompletedExceptionally()) {
                        // Record the failure
                        try {
                            dbFuture.get(); // This will throw the exception
                        } catch (Exception e) {
                            failures.add(e);
                            if (logger.isWarnEnabled()) {
                                logger.warn("Database connection failed for {}: {}", entry.getKey(), e.getMessage());
                            }
                        }
                    }
                }

                // Check if we have at least one successful connection
                if (databases.isEmpty()) {
                    // All connections failed - fail the entire operation
                    if (logger.isErrorEnabled()) {
                        logger.error("All database connections failed. Total failures: {}", failures.size());
                    }

                    // Create a composite exception with all failures
                    RedisConnectionException compositeException = new RedisConnectionException(
                            "Failed to connect to any database. All " + databaseConfigs.size() + " connection(s) failed.");
                    failures.forEach(compositeException::addSuppressed);

                    connectionFuture.completeExceptionally(compositeException);
                    return null;
                }

                // We have at least one successful connection - proceed
                if (logger.isInfoEnabled()) {
                    logger.info("Successfully connected to {} out of {} database(s)", databases.size(), databaseConfigs.size());
                    if (!failures.isEmpty()) {
                        logger.info("{} database connection(s) failed but proceeding with successful ones", failures.size());
                    }
                }

                // Wait for initial health checks asynchronously
                CompletableFuture<Map<RedisURI, HealthStatus>> healthCheckFuture = waitForInitialHealthyDatabaseAsync(
                        healthStatusManager, databases);

                healthCheckFuture.whenComplete((healthStatuses, ht) -> {
                    if (ht != null) {
                        // Health check failed, close all connections
                        databases.values().forEach(db -> db.getConnection().closeAsync());
                        connectionFuture.completeExceptionally(ht);
                        return;
                    }

                    // Filter out unhealthy databases and closed connections - only include healthy and open ones
                    Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> healthyDatabases = new ConcurrentHashMap<>();
                    List<RedisURI> excludedEndpoints = new ArrayList<>();

                    for (Map.Entry<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> entry : databases.entrySet()) {
                        HealthStatus status = healthStatuses.get(entry.getKey());
                        boolean isHealthy = status != null && status.isHealthy();
                        boolean isOpen = entry.getValue().getConnection().isOpen();

                        if (isHealthy && isOpen) {
                            healthyDatabases.put(entry.getKey(), entry.getValue());
                        } else {
                            excludedEndpoints.add(entry.getKey());
                            // Close excluded database connections
                            entry.getValue().getConnection().closeAsync();
                            if (!isHealthy) {
                                logger.info("Excluding database {} due to unhealthy status: {}", entry.getKey(), status);
                            } else if (!isOpen) {
                                logger.info("Excluding database {} because connection is closed", entry.getKey());
                            }
                        }
                    }

                    if (!excludedEndpoints.isEmpty() && logger.isInfoEnabled()) {
                        logger.info("Excluded {} database(s) from connection: {}", excludedEndpoints.size(), excludedEndpoints);
                    }

                    // Create the multi-db connection wrapper with only healthy and open databases
                    StatefulRedisMultiDbConnection<K, V> multiDbConnection = new StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<K, V>, K, V>(
                            healthyDatabases, getResources(), codec, this::createRedisDatabase, healthStatusManager);

                    connectionFuture.complete(multiDbConnection);
                });

            } catch (Exception e) {
                // Unexpected error during connection creation
                logger.error("Unexpected error during async connection", e);
                databaseFutures.values().forEach(dbFuture -> {
                    if (dbFuture.isDone() && !dbFuture.isCompletedExceptionally()) {
                        dbFuture.thenAccept(db -> db.getConnection().closeAsync());
                    }
                });
                connectionFuture.completeExceptionally(e);
            }

            return null;
        });

        // Return a ConnectionFuture with null remote address (multi-endpoint scenario)
        return ConnectionFuture.from(null, connectionFuture);
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
