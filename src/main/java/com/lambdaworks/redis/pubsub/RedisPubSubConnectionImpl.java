// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.pubsub;

import static com.lambdaworks.redis.protocol.CommandType.PSUBSCRIBE;
import static com.lambdaworks.redis.protocol.CommandType.PUNSUBSCRIBE;
import static com.lambdaworks.redis.protocol.CommandType.SUBSCRIBE;
import static com.lambdaworks.redis.protocol.CommandType.UNSUBSCRIBE;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * An asynchronous thread-safe pub/sub connection to a redis server. After one or more channels are subscribed to only pub/sub
 * related commands or {@link #quit} may be called.
 * 
 * Incoming messages and results of the {@link #subscribe}/{@link #unsubscribe} calls will be passed to all registered
 * {@link RedisPubSubListener}s.
 * 
 * A {@link com.lambdaworks.redis.protocol.ConnectionWatchdog} monitors each connection and reconnects automatically until
 * {@link #close} is called. Channel and pattern subscriptions are renewed after reconnecting.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class RedisPubSubConnectionImpl<K, V> extends RedisAsyncConnectionImpl<K, V> implements RedisPubSubConnection<K, V> {
    private final List<RedisPubSubListener<K, V>> listeners;
    private final Set<K> channels;
    private final Set<K> patterns;

    /**
     * Initialize a new connection.
     * 
     * @param writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a responses.
     * @param unit Unit of time for the timeout.
     */
    public RedisPubSubConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        super(writer, codec, timeout, unit);
        listeners = new CopyOnWriteArrayList<RedisPubSubListener<K, V>>();
        channels = new HashSet<K>();
        patterns = new HashSet<K>();
    }

    /**
     * Add a new listener.
     * 
     * @param listener Listener.
     */
    public void addListener(RedisPubSubListener<K, V> listener) {
        listeners.add(listener);
    }

    /**
     * Remove an existing listener.
     * 
     * @param listener Listener.
     */
    public void removeListener(RedisPubSubListener<K, V> listener) {
        listeners.remove(listener);
    }

    @Override
    public void psubscribe(K... patterns) {
        dispatch(PSUBSCRIBE, new PubSubOutput<K, V>(codec), args(patterns));
    }

    @Override
    public void punsubscribe(K... patterns) {
        dispatch(PUNSUBSCRIBE, new PubSubOutput<K, V>(codec), args(patterns));
    }

    @Override
    public void subscribe(K... channels) {
        dispatch(SUBSCRIBE, new PubSubOutput<K, V>(codec), args(channels));
    }

    @Override
    public void unsubscribe(K... channels) {
        dispatch(UNSUBSCRIBE, new PubSubOutput<K, V>(codec), args(channels));
    }

    @Override
    public void activated() {

        if (!channels.isEmpty()) {
            subscribe(toArray(channels));
            channels.clear();
        }

        if (!patterns.isEmpty()) {
            psubscribe(toArray(patterns));
            patterns.clear();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(Object msg) {
        PubSubOutput<K, V> output = (PubSubOutput<K, V>) msg;
        for (RedisPubSubListener<K, V> listener : listeners) {
            switch (output.type()) {
                case message:
                    listener.message(output.channel(), output.get());
                    break;
                case pmessage:
                    listener.message(output.pattern(), output.channel(), output.get());
                    break;
                case psubscribe:
                    patterns.add(output.pattern());
                    listener.psubscribed(output.pattern(), output.count());
                    break;
                case punsubscribe:
                    patterns.remove(output.pattern());
                    listener.punsubscribed(output.pattern(), output.count());
                    break;
                case subscribe:
                    channels.add(output.channel());
                    listener.subscribed(output.channel(), output.count());
                    break;
                case unsubscribe:
                    channels.remove(output.channel());
                    listener.unsubscribed(output.channel(), output.count());
                    break;
                default:
                    throw new UnsupportedOperationException("Operation " + output.type() + " not supported");
            }
        }
    }

    private CommandArgs<K, V> args(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKeys(keys);
        return args;
    }

    @SuppressWarnings("unchecked")
    private <T> T[] toArray(Collection<T> c) {
        Class<T> cls = (Class<T>) c.iterator().next().getClass();
        T[] array = (T[]) Array.newInstance(cls, c.size());
        return c.toArray(array);
    }
}
