package io.lettuce.core.failover;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.health.HealthCheck;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.HealthStatusChangeEvent;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.internal.AbstractInvocationHandler;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Stateful connection wrapper that holds multiple underlying connections and delegates to the currently active one. Command
 * interfaces (sync/async/reactive) are dynamic proxies that always target the current active connection at invocation time so
 * they remain valid across switches.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public class StatefulRedisMultiDbConnectionImpl<C extends StatefulRedisConnection<K, V>, K, V>
        implements StatefulRedisMultiDbConnection<K, V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(StatefulRedisMultiDbConnectionImpl.class);

    protected final Map<RedisURI, RedisDatabase<C>> databases;

    protected final HealthStatusManager healthStatusManager;

    // this should not be null ever after succesfull initialization
    protected volatile RedisDatabase<C> current;

    protected final RedisCommands<K, V> sync;

    protected final RedisAsyncCommandsImpl<K, V> async;

    protected final RedisReactiveCommandsImpl<K, V> reactive;

    protected final RedisCodec<K, V> codec;

    protected final Set<PushListener> pushListeners = ConcurrentHashMap.newKeySet();

    protected final Set<RedisConnectionStateListener> connectionStateListeners = ConcurrentHashMap.newKeySet();

    protected final DatabaseConnectionFactory<C, K, V> connectionFactory;

    private final ReadWriteLock multiDbLock = new ReentrantReadWriteLock();

    private final Lock readLock = multiDbLock.readLock();

    private final Lock writeLock = multiDbLock.writeLock();

    private final ClientResources clientResources;

    public StatefulRedisMultiDbConnectionImpl(Map<RedisURI, RedisDatabase<C>> connections, ClientResources resources,
            RedisCodec<K, V> codec, DatabaseConnectionFactory<C, K, V> connectionFactory,
            HealthStatusManager healthStatusManager) {
        if (connections == null || connections.isEmpty()) {
            throw new IllegalArgumentException("connections must not be empty");
        }
        LettuceAssert.notNull(healthStatusManager, "healthStatusManager must not be null");

        this.databases = new ConcurrentHashMap<>(connections);
        this.clientResources = resources;
        this.codec = codec;
        this.connectionFactory = connectionFactory;
        this.healthStatusManager = healthStatusManager;

        databases.values().forEach(db -> db.getCircuitBreaker().addListener(this::onCircuitBreakerStateChange));
        databases.values().forEach(db -> healthStatusManager.registerListener(db.getRedisURI(), this::onHealthStatusChange));

        // TODO: Current implementation forces all database connections to be created and established (at least once before this
        // constructor called).
        // This is suboptimal and should be replaced with a logic that uses async connection creation and state management,
        // which safely starts with at least one healthy connection.

        // TODO: It is error prone to leave the possibility of setting null as current connection.
        // Either we should identify and wait for healthy connection right here or pass it from outside as already identified as
        // healthy
        this.current = getNextHealthyDatabase(null);

        this.async = newRedisAsyncCommandsImpl();
        this.sync = newRedisSyncCommandsImpl();
        this.reactive = newRedisReactiveCommandsImpl();
    }

    private void onCircuitBreakerStateChange(CircuitBreakerStateChangeEvent event) {
        if (logger.isInfoEnabled()) {
            logger.info("Circuit breaker id {} status changed from {} to {}", event.getCircuitBreaker().getId(),
                    event.getPreviousState(), event.getNewState());
        }
        RedisDatabase<C> database = databases.values().stream()
                .filter(db -> db.getCircuitBreaker() == event.getCircuitBreaker()).findAny().orElse(null);

        if (database == null) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info("Circuit breaker for {} changed state from {} to {}", database.getId(), event.getPreviousState(),
                    event.getNewState());
        }
        if (!event.getNewState().isClosed() && isCurrent(database)) {
            failoverFrom(database);
        }
    }

    private void onHealthStatusChange(HealthStatusChangeEvent event) {
        if (logger.isInfoEnabled()) {
            logger.info("Health status changed for {} from {} to {}", event.getEndpoint(), event.getOldStatus(),
                    event.getNewStatus());
        }
        RedisDatabase<C> database = databases.get(event.getEndpoint());

        if (database == null) {
            logger.warn("Health status changed for unknown database: {}", event.getEndpoint());
            return;
        }

        if (event.getNewStatus() == HealthStatus.UNHEALTHY && isCurrent(database)) {
            logger.info("Database {} is unhealthy, failing over if current", database.getId());
            failoverFrom(database);
        }
    }

    private boolean isCurrent(RedisDatabase<C> database) {
        return database == current;
    }

    private void failoverFrom(RedisDatabase<C> fromDb) {
        RedisDatabase<C> selectedDatabase = getNextHealthyDatabase(fromDb);

        if (selectedDatabase != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Initiating failover from {} to {}", fromDb.getId(), selectedDatabase.getId());
            }
            if (safeSwitch(selectedDatabase)) {
                if (logger.isInfoEnabled()) {
                    logger.info("Failover successful from {} to {}", fromDb.getId(), selectedDatabase.getId());
                }
                // check if we missed any events during the switch
                if (!DatabasePredicates.isHealthyAndCbClosed.test(selectedDatabase)) {
                    failoverFrom(selectedDatabase);
                }
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("Failover attempt from {} to {} has failed!! Retrying failover one more time...",
                            fromDb.getId(), selectedDatabase.getId());
                }
                failoverFrom(fromDb);
            }
        } else {
            // No healthy database found, stay on the current one
            // TODO: manage max attempts to failover
            logger.info("No healthy database found, staying on current database {}", fromDb.getId());
        }
    }

    private RedisDatabase<C> getNextHealthyDatabase(RedisDatabase<C> dbToExclude) {
        return databases.values().stream().filter(DatabasePredicates.isHealthyAndCbClosed)
                .filter(DatabasePredicates.isNot(dbToExclude)).max(DatabaseComparators.byWeight).orElse(null);
    }

    static class DatabaseComparators {

        public static final Comparator<RedisDatabase<?>> byWeight = Comparator.comparingDouble(RedisDatabase::getWeight);

    }

    static class DatabasePredicates {

        public static final Predicate<RedisDatabase<?>> isHealthCheckHealthy = db -> {
            HealthCheck healthCheck = db.getHealthCheck();
            // If no health check configured, assume healthy
            if (healthCheck == null) {
                return true;
            }
            return healthCheck.getStatus() == HealthStatus.HEALTHY;
        };

        public static final Predicate<RedisDatabase<?>> isCbClosed = db -> db.getCircuitBreaker()
                .getCurrentState() == CircuitBreaker.State.CLOSED;

        public static final Predicate<RedisDatabase<?>> isHealthyAndCbClosed = isHealthCheckHealthy.and(isCbClosed);

        public static Predicate<RedisDatabase<?>> isNot(RedisDatabase<?> dbInstance) {
            return db -> !db.equals(dbInstance);
        }

    }

    @Override
    public RedisAsyncCommands<K, V> async() {
        return async;
    }

    /**
     * Create a new instance of {@link RedisCommands}. Can be overriden to extend.
     *
     * @return a new instance
     */
    protected RedisCommands<K, V> newRedisSyncCommandsImpl() {
        return syncHandler(async(), RedisCommands.class, RedisClusterCommands.class);
    }

    @SuppressWarnings("unchecked")
    protected <T> T syncHandler(Object asyncApi, Class<?>... interfaces) {
        AbstractInvocationHandler h = new MultiDbFutureSyncInvocationHandler(this, asyncApi, interfaces);
        return (T) Proxy.newProxyInstance(AbstractRedisClient.class.getClassLoader(), interfaces, h);
    }

    /**
     * Create a new instance of {@link RedisAsyncCommandsImpl}. Can be overriden to extend.
     *
     * @return a new instance
     */
    protected RedisAsyncCommandsImpl<K, V> newRedisAsyncCommandsImpl() {
        return new RedisAsyncCommandsImpl<>(this, codec, () -> this.getOptions().getJsonParser().get());
    }

    @Override
    public RedisReactiveCommands<K, V> reactive() {
        return reactive;
    }

    /**
     * Create a new instance of {@link RedisReactiveCommandsImpl}. Can be overriden to extend.
     *
     * @return a new instance
     */
    protected RedisReactiveCommandsImpl<K, V> newRedisReactiveCommandsImpl() {
        return new RedisReactiveCommandsImpl<>(this, codec, () -> this.getOptions().getJsonParser().get());
    }

    @Override
    public RedisCommands<K, V> sync() {
        return sync;
    }

    @Override
    public void addListener(RedisConnectionStateListener listener) {
        doBySharedLock(() -> {
            connectionStateListeners.add(listener);
            current.getConnection().addListener(listener);
        });
    }

    @Override
    public void removeListener(RedisConnectionStateListener listener) {
        doBySharedLock(() -> {
            connectionStateListeners.remove(listener);
            current.getConnection().removeListener(listener);
        });
    }

    @Override
    public void setTimeout(Duration timeout) {
        databases.values().forEach(db -> db.getConnection().setTimeout(timeout));
    }

    @Override
    public Duration getTimeout() {
        return current.getConnection().getTimeout();
    }

    @Override
    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> command) {
        return current.getConnection().dispatch(command);
    }

    @Override
    public Collection<RedisCommand<K, V, ?>> dispatch(Collection<? extends RedisCommand<K, V, ?>> commands) {
        return current.getConnection().dispatch(commands);
    }

    @Override
    public void close() {
        healthStatusManager.close();
        databases.values().forEach(db -> db.getConnection().close());
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return CompletableFuture.allOf(databases.values().stream().map(db -> db.getConnection())
                .map(StatefulConnection::closeAsync).toArray(CompletableFuture[]::new));
    }

    @Override
    public boolean isOpen() {
        return current.getConnection().isOpen();
    }

    @Override
    public ClientOptions getOptions() {
        return current.getConnection().getOptions();
    }

    @Override
    public ClientResources getResources() {
        return clientResources;
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        databases.values().forEach(db -> db.getConnection().setAutoFlushCommands(autoFlush));
    }

    @Override
    public void flushCommands() {
        current.getConnection().flushCommands();
    }

    @Override
    public boolean isMulti() {
        return current.getConnection().isMulti();
    }

    @Override
    public void addListener(PushListener listener) {
        doBySharedLock(() -> {
            pushListeners.add(listener);
            current.getConnection().addListener(listener);
        });
    }

    @Override
    public void removeListener(PushListener listener) {
        doBySharedLock(() -> {
            pushListeners.remove(listener);
            current.getConnection().removeListener(listener);
        });
    }

    @Override
    public RedisCodec<K, V> getCodec() {
        return codec;
    }

    @Override
    public RedisURI getCurrentEndpoint() {
        return current.getRedisURI();
    }

    @Override
    public Iterable<RedisURI> getEndpoints() {
        return databases.keySet();
    }

    @Override
    public boolean switchTo(RedisURI redisURI) {
        RedisDatabase<C> target = databases.get(redisURI);
        if (target == null) {
            throw new IllegalArgumentException("Unknown endpoint: " + redisURI);
        }
        if (safeSwitch(target)) {
            logger.info("Switched to database {}", target.getId());
            // if target got unhealthy along the way, failover from it
            if (!DatabasePredicates.isHealthyAndCbClosed.test(target)) {
                failoverFrom(target);
            }
            // this will return true no matter if it failed over or not,
            // since there is a possible window that commands could be issued before the failover kicks in
            return true;
        } else {
            logger.info("Failed to switch to database {}", target.getId());
            return false;
        }
    }

    /**
     * Switch to the given database. This method is thread-safe and can be called from multiple threads.
     * <p>
     * Beyond thread safety it also handles the cases where;
     * <p>
     * - the requested database is the same as the current one
     * <p>
     * OR
     * <p>
     * - the requested database is a different instance than registered in connection map but with the same target endpoint/uri.
     * 
     * @param database the database to switch to
     * @return true if the provided database is now the selected database after executing this method ( even if it was already
     *         the current one), false otherwise
     */
    boolean safeSwitch(RedisDatabase<?> database) {
        logger.info("Starting switching to database {}", database.getId());
        AtomicBoolean switched = new AtomicBoolean(false);
        doByExclusiveLock(() -> {
            RedisDatabase<C> fromDb = current;
            RedisDatabase<C> toDb = databases.get(database.getRedisURI());

            if (database != toDb) {
                logger.error(
                        "Same URI with different database, this should never happen in the driver. Requested database: {}, found database: {} , endpoint: {}",
                        database.getId(), toDb.getId(), toDb.getRedisURI());
                throw new IllegalStateException(
                        "Same URI with different database, this should never happen in the driver. Requested database: "
                                + database.getId() + ", found database: " + toDb.getId() + " , endpoint: "
                                + toDb.getRedisURI());
            }

            if (fromDb == null || toDb == null) {
                throw new UnsupportedOperationException(
                        "Unable to switch between endpoints - the driver was not able to locate the source or destination endpoint.");
            }

            if (fromDb == toDb) {
                // Nothing to do, already on the right database
                switched.set(true);
                return;
            }

            if (!DatabasePredicates.isHealthyAndCbClosed.test(toDb)) {
                logger.warn("Requested database ({}) is unhealthy or circuit breaker is open. Skipping switch request.",
                        toDb.getId());
                return;
            }

            switched.set(true);
            current = toDb;
            logger.info("Switched to database {}", toDb.getId());

            connectionStateListeners.forEach(listener -> {
                toDb.getConnection().addListener(listener);
                fromDb.getConnection().removeListener(listener);
                if (logger.isDebugEnabled()) {
                    logger.debug("Moved connection state listener {} from {} to {}", listener, fromDb.getRedisURI(),
                            toDb.getRedisURI());
                }
            });
            pushListeners.forEach(listener -> {
                toDb.getConnection().addListener(listener);
                fromDb.getConnection().removeListener(listener);
                if (logger.isDebugEnabled()) {
                    logger.debug("Moved push listener {} from {} to {}", listener, fromDb.getRedisURI(), toDb.getRedisURI());
                }
            });

            fromDb.getDatabaseEndpoint().handOverCommandQueue(toDb.getDatabaseEndpoint());
        });
        return switched.get();
    }

    protected void doBySharedLock(Runnable operation) {
        readLock.lock();
        try {
            operation.run();
        } finally {
            readLock.unlock();
        }
    }

    protected void doByExclusiveLock(Runnable operation) {
        writeLock.lock();
        try {
            operation.run();
        } finally {
            writeLock.unlock();
        }
    }

    RedisDatabase<C> getCurrentDatabase() {
        return current;
    }

    @Override
    public RedisDatabase<C> getDatabase(RedisURI redisURI) {
        RedisDatabase<C> database = databases.get(redisURI);
        if (database == null) {
            throw new IllegalArgumentException("Unknown endpoint: " + redisURI);
        }
        return database;
    }

    @Override
    public boolean isHealthy(RedisURI endpoint) {
        RedisDatabase<C> database = databases.get(endpoint);
        if (database == null) {
            throw new IllegalArgumentException("Unknown endpoint: " + endpoint);
        }

        return DatabasePredicates.isHealthyAndCbClosed.test(database);
    }

    @Override
    public void addDatabase(RedisURI redisURI, float weight) {
        addDatabase(DatabaseConfig.builder(redisURI).weight(weight).build());
    }

    @Override
    public void addDatabase(DatabaseConfig databaseConfig) {
        if (databaseConfig == null) {
            throw new IllegalArgumentException("DatabaseConfig must not be null");
        }

        if (connectionFactory == null) {
            throw new UnsupportedOperationException(
                    "Adding databases dynamically is not supported. Connection was created without a DatabaseConnectionFactory.");
        }

        RedisURI redisURI = databaseConfig.getRedisURI();

        doByExclusiveLock(() -> {
            if (databases.containsKey(redisURI)) {
                throw new IllegalArgumentException("Database already exists: " + redisURI);
            }

            healthStatusManager.registerListener(redisURI, this::onHealthStatusChange);

            // Create new database connection using the factory
            RedisDatabase<C> database = connectionFactory.createDatabase(databaseConfig, codec, healthStatusManager);

            // Add listeners to the new connection if it's the current one
            // (though it won't be current initially since we're just adding it)
            databases.put(redisURI, database);

            database.getCircuitBreaker().addListener(this::onCircuitBreakerStateChange);

        });

    }

    @Override
    public void removeDatabase(RedisURI redisURI) {
        if (redisURI == null) {
            throw new IllegalArgumentException("RedisURI must not be null");
        }
        doByExclusiveLock(() -> {
            RedisDatabase<C> database = null;
            database = databases.get(redisURI);
            if (database == null) {
                throw new IllegalArgumentException("Database not found: " + redisURI);
            }

            if (current.getRedisURI().equals(redisURI)) {
                throw new UnsupportedOperationException("Cannot remove the currently active database: " + redisURI);
            }

            healthStatusManager.unregisterListener(redisURI, this::onHealthStatusChange);
            healthStatusManager.remove(redisURI);

            // Remove the database and close its connection
            databases.remove(redisURI);
            database.close();
        });
    }

}
