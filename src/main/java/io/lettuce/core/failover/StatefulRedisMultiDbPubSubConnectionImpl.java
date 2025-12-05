package io.lettuce.core.failover;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.json.JsonParser;
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

/**
 * @author Ali Takavci
 * @since 7.1
 */
public class StatefulRedisMultiDbPubSubConnectionImpl<K, V>
        extends StatefulRedisMultiDbConnectionImpl<StatefulRedisPubSubConnection<K, V>, K, V>
        implements StatefulRedisMultiDbPubSubConnection<K, V> {

    private final Set<RedisPubSubListener<K, V>> pubSubListeners = ConcurrentHashMap.newKeySet();

    public StatefulRedisMultiDbPubSubConnectionImpl(
            Map<RedisURI, RedisDatabase<StatefulRedisPubSubConnection<K, V>>> connections, ClientResources resources,
            RedisCodec<K, V> codec, Supplier<JsonParser> parser,
            DatabaseConnectionFactory<StatefulRedisPubSubConnection<K, V>, K, V> connectionFactory,
            HealthStatusManager healthStatusManager) {
        super(connections, resources, codec, parser, connectionFactory, healthStatusManager);
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
    public RedisPubSubReactiveCommands<K, V> reactive() {
        return (RedisPubSubReactiveCommands<K, V>) reactive;
    }

    @Override
    protected RedisPubSubReactiveCommandsImpl<K, V> newRedisReactiveCommandsImpl() {
        return new RedisPubSubReactiveCommandsImpl<>(this, codec);
    }

    @Override
    public void switchToDatabase(RedisURI redisURI) {

        RedisDatabase<StatefulRedisPubSubConnection<K, V>> fromDb = current;
        doByExclusiveLock(() -> {
            super.switchToDatabase(redisURI);
            pubSubListeners.forEach(listener -> {
                current.getConnection().addListener(listener);
                fromDb.getConnection().removeListener(listener);
            });

            moveSubscriptions(fromDb, current);
        });
    }

    @SuppressWarnings("unchecked")
    public void moveSubscriptions(RedisDatabase<StatefulRedisPubSubConnection<K, V>> fromDb,
            RedisDatabase<StatefulRedisPubSubConnection<K, V>> toDb) {

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
