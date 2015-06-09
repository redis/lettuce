// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.pubsub;

import static com.lambdaworks.redis.protocol.CommandType.PSUBSCRIBE;
import static com.lambdaworks.redis.protocol.CommandType.PUNSUBSCRIBE;
import static com.lambdaworks.redis.protocol.CommandType.SUBSCRIBE;
import static com.lambdaworks.redis.protocol.CommandType.UNSUBSCRIBE;

import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisFuture;
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
public class RedisPubSubAsyncConnectionImpl<K, V> extends RedisAsyncConnectionImpl<K, V> implements
        RedisPubSubAsyncConnection<K, V> {

    /**
     * Initialize a new connection.
     * 
     * @param connection the connection .
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisPubSubAsyncConnectionImpl(StatefulRedisPubSubConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.connection = connection;
    }

    /**
     * Add a new listener.
     * 
     * @param listener Listener.
     */
    @Override
    public void addListener(RedisPubSubListener<K, V> listener) {
        getStatefulConnection().addListener(listener);
    }

    /**
     * Remove an existing listener.
     * 
     * @param listener Listener.
     */
    @Override
    public void removeListener(RedisPubSubListener<K, V> listener) {
        getStatefulConnection().removeListener(listener);
    }

    @Override
    public RedisFuture<Void> psubscribe(K... patterns) {
        return (RedisFuture<Void>) dispatch(PSUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(patterns));
    }

    @Override
    public RedisFuture<Void> punsubscribe(K... patterns) {
        return (RedisFuture<Void>) dispatch(PUNSUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(patterns));
    }

    @Override
    public RedisFuture<Void> subscribe(K... channels) {
        return (RedisFuture<Void>) dispatch(SUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(channels));
    }

    @Override
    public RedisFuture<Void> unsubscribe(K... channels) {
        return (RedisFuture<Void>) dispatch(UNSUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(channels));
    }

    private CommandArgs<K, V> args(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKeys(keys);
        return args;
    }

    @Override
    public StatefulRedisPubSubConnection<K, V> getStatefulConnection() {
        return (StatefulRedisPubSubConnection<K, V>) super.getStatefulConnection();
    }
}
