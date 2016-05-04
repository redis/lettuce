package com.lambdaworks.redis.pubsub;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.pubsub.api.rx.RedisPubSubReactiveCommands;
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
@ChannelHandler.Sharable
public class StatefulRedisPubSubConnectionImpl<K, V> extends StatefulRedisConnectionImpl<K, V> implements
        StatefulRedisPubSubConnection<K, V> {

    protected RedisPubSubAsyncCommands<K, V> async;
    protected RedisPubSubCommands<K, V> sync;
    protected RedisPubSubReactiveCommands<K, V> reactive;
    protected final List<RedisPubSubListener<K, V>> listeners;
    protected final Set<K> channels;
    protected final Set<K> patterns;

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param unit Unit of time for the timeout.
     */
    public StatefulRedisPubSubConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, codec, timeout, unit);

        listeners = new CopyOnWriteArrayList<>();
        channels = new ConcurrentSet<>();
        patterns = new ConcurrentSet<>();
    }

    /**
     * Add a new listener.
     *
     * @param listener Listener.
     */
    @Override
    public void addListener(RedisPubSubListener<K, V> listener) {
        listeners.add(listener);
    }

    /**
     * Remove an existing listener.
     *
     * @param listener Listener.
     */
    @Override
    public void removeListener(RedisPubSubListener<K, V> listener) {
        listeners.remove(listener);
    }

    public RedisPubSubAsyncCommands<K, V> async() {
        if (async == null) {
            async = newRedisPubSubAsyncCommandsImpl();
        }

        return async;
    }

    @Override
    public RedisPubSubCommands<K, V> sync() {
        if (sync == null) {
            sync = syncHandler(async(), RedisConnection.class, RedisClusterConnection.class, RedisPubSubCommands.class);
        }

        return sync;
    }

    protected RedisPubSubAsyncCommandsImpl<K, V> newRedisPubSubAsyncCommandsImpl() {
        return new RedisPubSubAsyncCommandsImpl<>(this, codec);
    }

    @Override
    public RedisPubSubReactiveCommands<K, V> reactive() {
        if (reactive == null) {
            reactive = newRedisPubSubReactiveCommandsImpl();
        }

        return reactive;
    }

    protected RedisPubSubReactiveCommandsImpl<K, V> newRedisPubSubReactiveCommandsImpl() {
        return new RedisPubSubReactiveCommandsImpl<>(this, codec);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(Object msg) {
        PubSubOutput<K, V, V> output = (PubSubOutput<K, V, V>) msg;

        // drop empty messages
        if (output.type() == null || (output.pattern() == null && output.channel() == null && output.get() == null)) {
            return;
        }

        updateInternalState(output);
        notifyListeners(output);
    }

    private void notifyListeners(PubSubOutput<K, V, V> output) {
        // update listeners
        for (RedisPubSubListener<K, V> listener : listeners) {
            switch (output.type()) {
                case message:
                    listener.message(output.channel(), output.get());
                    break;
                case pmessage:
                    listener.message(output.pattern(), output.channel(), output.get());
                    break;
                case psubscribe:
                    listener.psubscribed(output.pattern(), output.count());
                    break;
                case punsubscribe:
                    listener.punsubscribed(output.pattern(), output.count());
                    break;
                case subscribe:
                    listener.subscribed(output.channel(), output.count());
                    break;
                case unsubscribe:
                    listener.unsubscribed(output.channel(), output.count());
                    break;
                default:
                    throw new UnsupportedOperationException("Operation " + output.type() + " not supported");
            }
        }
    }

    /**
     * Re-subscribe to all previously subscribed channels and patterns.
     * 
     * @return list of the futures of the {@literal subscribe} and {@literal psubscribe} commands.
     */
    protected List<RedisFuture<Void>> resubscribe() {

        List<RedisFuture<Void>> result = new ArrayList<>();

        if (!channels.isEmpty()) {
            result.add(async().subscribe(toArray(channels)));
        }

        if (!patterns.isEmpty()) {
            result.add(async().psubscribe(toArray(patterns)));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> T[] toArray(Collection<T> c) {
        Class<T> cls = (Class<T>) c.iterator().next().getClass();
        T[] array = (T[]) Array.newInstance(cls, c.size());
        return c.toArray(array);
    }

    private void updateInternalState(PubSubOutput<K, V, V> output) {
        // update internal state
        switch (output.type()) {
            case psubscribe:
                patterns.add(output.pattern());
                break;
            case punsubscribe:
                patterns.remove(output.pattern());
                break;
            case subscribe:
                channels.add(output.channel());
                break;
            case unsubscribe:
                channels.remove(output.channel());
                break;
            default:
                break;
        }
    }

    @Override
    public void activated() {
        super.activated();
        resubscribe();
    }
}
