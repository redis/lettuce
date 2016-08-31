package com.lambdaworks.redis.pubsub;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.StatefulRedisConnectionImpl;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.pubsub.api.reactive.RedisPubSubReactiveCommands;
import com.lambdaworks.redis.pubsub.api.sync.RedisPubSubCommands;

import io.netty.channel.ChannelHandler;
import io.netty.util.internal.ConcurrentSet;

/**
 * An thread-safe pub/sub connection to a Redis server. Multiple threads may share one {@link StatefulRedisPubSubConnectionImpl}
 *
 * A {@link ConnectionWatchdog} monitors each connection and reconnects automatically until {@link #close} is called. All
 * pending commands will be (re)sent after successful reconnection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class StatefulRedisPubSubConnectionImpl<K, V> extends StatefulRedisConnectionImpl<K, V> implements
        StatefulRedisPubSubConnection<K, V> {

    private final PubSubEndpoint<K, V> endpoint;

    /**
     * Initialize a new connection.
     *
     * @param endpoint the {@link PubSubEndpoint}
     * @param writer the writer used to write commands
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param unit Unit of time for the timeout.
     */
    public StatefulRedisPubSubConnectionImpl(PubSubEndpoint<K, V> endpoint, RedisChannelWriter writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, codec, timeout, unit);

        this.endpoint = endpoint;
    }

    /**
     * Add a new listener.
     *
     * @param listener Listener.
     */
    @Override
    public void addListener(RedisPubSubListener<K, V> listener) {
        endpoint.addListener(listener);
    }

    /**
     * Remove an existing listener.
     *
     * @param listener Listener.
     */
    @Override
    public void removeListener(RedisPubSubListener<K, V> listener) {
        endpoint.removeListener(listener);
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

    /**
     * Re-subscribe to all previously subscribed channels and patterns.
     * 
     * @return list of the futures of the {@literal subscribe} and {@literal psubscribe} commands.
     */
    protected List<RedisFuture<Void>> resubscribe() {

        List<RedisFuture<Void>> result = new ArrayList<>();

        if (!endpoint.getChannels().isEmpty()) {
            result.add(async().subscribe(toArray(endpoint.getChannels())));
        }

        if (!endpoint.getPatterns().isEmpty()) {
            result.add(async().psubscribe(toArray(endpoint.getPatterns())));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> T[] toArray(Collection<T> c) {
        Class<T> cls = (Class<T>) c.iterator().next().getClass();
        T[] array = (T[]) Array.newInstance(cls, c.size());
        return c.toArray(array);
    }



    @Override
    public void activated() {
        super.activated();
        resubscribe();
    }
}
