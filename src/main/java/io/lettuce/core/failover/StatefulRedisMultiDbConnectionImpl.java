package io.lettuce.core.failover;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.internal.AbstractInvocationHandler;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * Stateful connection wrapper that holds multiple underlying connections and delegates to the currently active one. Command
 * interfaces (sync/async/reactive) are dynamic proxies that always target the current active connection at invocation time so
 * they remain valid across switches.
 *
 * @author Ali Takavci
 * @since 7.1
 */
public class StatefulRedisMultiDbConnectionImpl<C extends StatefulRedisConnection<K, V>, K, V>
        implements StatefulRedisMultiDbConnection<K, V> {

    protected final Map<RedisURI, RedisDatabase<C>> databases;

    // this should not be null ever after succesfull initialization
    protected RedisDatabase<C> current;

    protected final RedisCommands<K, V> sync;

    protected final RedisAsyncCommandsImpl<K, V> async;

    protected final RedisReactiveCommandsImpl<K, V> reactive;

    protected final RedisCodec<K, V> codec;

    protected final Supplier<JsonParser> parser;

    protected final Set<PushListener> pushListeners = ConcurrentHashMap.newKeySet();

    protected final Set<RedisConnectionStateListener> connectionStateListeners = ConcurrentHashMap.newKeySet();

    protected final DatabaseConnectionFactory<C, K, V> connectionFactory;

    private final ReadWriteLock multiDbLock = new ReentrantReadWriteLock();

    private final Lock readLock = multiDbLock.readLock();

    private final Lock writeLock = multiDbLock.writeLock();

    public StatefulRedisMultiDbConnectionImpl(Map<RedisURI, RedisDatabase<C>> connections, ClientResources resources,
            RedisCodec<K, V> codec, Supplier<JsonParser> parser, DatabaseConnectionFactory<C, K, V> connectionFactory) {
        if (connections == null || connections.isEmpty()) {
            throw new IllegalArgumentException("connections must not be empty");
        }
        this.databases = new ConcurrentHashMap<>(connections);
        this.codec = codec;
        this.parser = parser;
        this.connectionFactory = connectionFactory;
        // TODO: Current implementation forces all database connections to be created and established (at least once before this
        // constructor called).
        // This is suboptimal and should be replaced with a logic that uses async connection creation and state management,
        // which safely starts with at least one healthy connection.
        this.current = getNextHealthyDatabase(null);

        this.async = newRedisAsyncCommandsImpl();
        this.sync = newRedisSyncCommandsImpl();
        this.reactive = newRedisReactiveCommandsImpl();

        databases.values().forEach(db -> db.getCircuitBreaker().addListener(this::onCircuitBreakerStateChange));
    }

    private void onCircuitBreakerStateChange(CircuitBreakerStateChangeEvent event) {
        if (event.getCircuitBreaker() == current.getCircuitBreaker() && event.getNewState() == CircuitBreaker.State.OPEN) {
            failoverFrom(current);
        }
    }

    private void failoverFrom(RedisDatabase<C> fromDb) {
        RedisDatabase<C> healthyDatabase = getNextHealthyDatabase(fromDb);
        if (healthyDatabase != null) {
            switchToDatabase(healthyDatabase.getRedisURI());
        } else {
            // No healthy database found, stay on the current one
            // TODO: manage max attempts to failover
        }
    }

    private RedisDatabase<C> getNextHealthyDatabase(RedisDatabase<C> dbToExclude) {
        return databases.values().stream().filter(DatabasePredicates.isHealthy).filter(DatabasePredicates.isNot(dbToExclude))
                .max(DatabaseComparators.byWeight).orElse(null);
    }

    static class DatabaseComparators {

        public static final Comparator<RedisDatabase<?>> byWeight = Comparator.comparingDouble(RedisDatabase::getWeight);

    }

    static class DatabasePredicates {

        public static final Predicate<RedisDatabase<?>> isHealthy = db -> db.getHealthStatus() == HealthStatus.HEALTHY;

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
        return new RedisAsyncCommandsImpl<>(this, codec, parser);
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
        return new RedisReactiveCommandsImpl<>(this, codec, parser);
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
        return current.getConnection().getResources();
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
    public void switchToDatabase(RedisURI redisURI) {
        doByExclusiveLock(() -> {
            RedisDatabase<C> fromDb = current;
            RedisDatabase<C> toDb = databases.get(redisURI);
            if (fromDb == null || toDb == null) {
                throw new UnsupportedOperationException(
                        "Unable to switch between endpoints - the driver was not able to locate the source or destination endpoint.");
            }
            if (fromDb.equals(toDb)) {
                return;
            }
            current = toDb;
            connectionStateListeners.forEach(listener -> {
                toDb.getConnection().addListener(listener);
                fromDb.getConnection().removeListener(listener);
            });
            pushListeners.forEach(listener -> {
                toDb.getConnection().addListener(listener);
                fromDb.getConnection().removeListener(listener);
            });
            fromDb.getDatabaseEndpoint().handOverCommandQueue(toDb.getDatabaseEndpoint());
        });
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

    @Override
    public CircuitBreaker getCircuitBreaker(RedisURI endpoint) {
        RedisDatabase<C> database = databases.get(endpoint);
        if (database == null) {
            throw new IllegalArgumentException("Unknown endpoint: " + endpoint);
        }
        return database.getCircuitBreaker();
    }

    @Override
    public void addDatabase(RedisURI redisURI, float weight) {
        addDatabase(new DatabaseConfig(redisURI, weight));
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

            // Create new database connection using the factory
            RedisDatabase<C> database = connectionFactory.createDatabase(databaseConfig, codec);

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

            // Remove the database and close its connection
            databases.remove(redisURI);
            database.close();
        });
    }

}
