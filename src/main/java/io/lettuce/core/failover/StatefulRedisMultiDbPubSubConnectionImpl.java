package io.lettuce.core.failover;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.pubsub.RedisPubSubAsyncCommandsImpl;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.RedisPubSubReactiveCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.lettuce.core.failover.MultiDbAsyncConnectionBuilder.RedisDatabaseAsyncCompletion;

/**
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
class StatefulRedisMultiDbPubSubConnectionImpl<K, V>
        extends StatefulRedisMultiDbConnectionImpl<StatefulRedisPubSubConnection<K, V>, K, V>
        implements StatefulRedisMultiDbPubSubConnection<K, V> {

    private final Set<RedisPubSubListener<K, V>> pubSubListeners = ConcurrentHashMap.newKeySet();

    public StatefulRedisMultiDbPubSubConnectionImpl(
            Map<RedisURI, RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>>> connections, ClientResources resources,
            RedisCodec<K, V> codec, DatabaseConnectionFactory<StatefulRedisPubSubConnection<K, V>, K, V> connectionFactory,
            HealthStatusManager healthStatusManager) {
        super(connections, resources, codec, connectionFactory, healthStatusManager);
    }

    public StatefulRedisMultiDbPubSubConnectionImpl(RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>> initialDatabase,
            Map<RedisURI, RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>>> connections, ClientResources resources,
            RedisCodec<K, V> codec, DatabaseConnectionFactory<StatefulRedisPubSubConnection<K, V>, K, V> connectionFactory,
            HealthStatusManager healthStatusManager,
            RedisDatabaseAsyncCompletion<StatefulRedisPubSubConnection<K, V>> completion) {
        super(initialDatabase, connections, resources, codec, connectionFactory, healthStatusManager, completion);
    }

    @Override
    public void addListener(RedisPubSubListener<K, V> listener) {
        doBySharedLock(() -> {
            pubSubListeners.add(listener);
            current.getConnection().addListener(listener);
        });
    }

    @Override
    public void removeListener(RedisPubSubListener<K, V> listener) {
        doBySharedLock(() -> {
            pubSubListeners.remove(listener);
            current.getConnection().removeListener(listener);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisPubSubAsyncCommands<K, V> async() {
        return (RedisPubSubAsyncCommands<K, V>) async;
    }

    @Override
    protected RedisPubSubAsyncCommandsImpl<K, V> newRedisAsyncCommandsImpl() {
        return new RedisPubSubAsyncCommandsImpl<>(this, codec);
    }

    @Override
    public RedisPubSubCommands<K, V> sync() {
        return (RedisPubSubCommands<K, V>) sync;
    }

    @Override
    protected RedisPubSubCommands<K, V> newRedisSyncCommandsImpl() {
        return syncHandler(async(), RedisPubSubCommands.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisPubSubReactiveCommands<K, V> reactive() {
        return (RedisPubSubReactiveCommands<K, V>) reactive;
    }

    @Override
    protected RedisPubSubReactiveCommandsImpl<K, V> newRedisReactiveCommandsImpl() {
        return new RedisPubSubReactiveCommandsImpl<>(this, codec);
    }

    /**
     * Switch to the given database with PubSub-specific handling. This method is thread-safe and can be called from multiple
     * threads.
     * <p>
     * In addition to the standard database switch behavior from the parent class, this method also:
     * <ul>
     * <li>Migrates all PubSub listeners from the old database connection to the new one</li>
     * <li>Re-subscribes to all active channels, shard channels, and patterns on the new database</li>
     * <li>Unsubscribes from the old database on a best-effort basis</li>
     * </ul>
     * <p>
     * The subscription migration ensures that active PubSub subscriptions are maintained across database switches, providing
     * seamless failover for PubSub operations.
     *
     * @param database the database to switch to
     * @param internalCall if {@code true}, validation failures return {@code false} and log errors; if {@code false},
     *        validation failures throw exceptions
     * @return {@code true} if the switch succeeded or the database was already current; {@code false} if validation failed and
     *         {@code internalCall} is {@code true}
     * @throws IllegalStateException if {@code internalCall} is {@code false} and validation fails
     * @throws UnsupportedOperationException if {@code internalCall} is {@code false} and the source or destination endpoint
     *         cannot be located
     * @see StatefulRedisMultiDbConnectionImpl#safeSwitch(RedisDatabaseImpl, boolean)
     */
    @Override
    boolean safeSwitch(RedisDatabaseImpl<?> database, boolean internalCall) {
        AtomicBoolean switched = new AtomicBoolean(false);
        doByExclusiveLock(() -> {
            RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>> fromDb = current;
            switched.set(super.safeSwitch(database, internalCall));
            if (fromDb == current) {
                return;
            }
            pubSubListeners.forEach(listener -> {
                current.getConnection().addListener(listener);
                fromDb.getConnection().removeListener(listener);
            });

            moveSubscriptions(fromDb, current);
        });
        return switched.get();
    }

    @SuppressWarnings("unchecked")
    public void moveSubscriptions(RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>> fromDb,
            RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>> toDb) {

        PubSubEndpoint<K, V> fromEndpoint = (PubSubEndpoint<K, V>) fromDb.getDatabaseEndpoint();
        StatefulRedisPubSubConnection<K, V> fromConn = (StatefulRedisPubSubConnection<K, V>) fromDb.getConnection();

        if (fromEndpoint.hasChannelSubscriptions()) {
            K[] channels = toArray(fromEndpoint.getChannels());
            moveSubscriptions(channels, async()::subscribe, fromConn.async()::unsubscribe);
        }

        if (fromEndpoint.hasShardChannelSubscriptions()) {
            K[] shardChannels = toArray(fromEndpoint.getShardChannels());
            moveSubscriptions(shardChannels, async()::ssubscribe, fromConn.async()::sunsubscribe);
        }

        if (fromEndpoint.hasPatternSubscriptions()) {
            K[] patterns = toArray(fromEndpoint.getPatterns());
            moveSubscriptions(patterns, async()::psubscribe, fromConn.async()::punsubscribe);
        }
    }

    private void moveSubscriptions(K[] channels, Function<K[], RedisFuture<Void>> subscribeFunc,
            Function<K[], RedisFuture<Void>> unsubscribeFunc) {
        // Re-subscribe to new endpoint
        RedisFuture<Void> subscribeFuture = subscribeFunc.apply(channels);
        handlePubSubCommandError(subscribeFuture, "Re-subscribe failed: ");

        // Unsubscribe from old endpoint on best effort basis
        RedisFuture<Void> unsubscribeFuture = unsubscribeFunc.apply(channels);
        handlePubSubCommandError(unsubscribeFuture, "Unsubscribe from old endpoint failed (best effort): ");
    }

    private void handlePubSubCommandError(RedisFuture<Void> future, String message) {
        future.exceptionally(throwable -> {
            InternalLoggerFactory.getInstance(getClass()).warn(message + future.getError());
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T[] toArray(Collection<T> c) {
        Class<T> cls = (Class<T>) c.iterator().next().getClass();
        T[] array = (T[]) Array.newInstance(cls, c.size());
        return c.toArray(array);
    }

}
