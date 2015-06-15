// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.pubsub;

import static com.lambdaworks.redis.protocol.CommandType.PSUBSCRIBE;
import static com.lambdaworks.redis.protocol.CommandType.PUNSUBSCRIBE;
import static com.lambdaworks.redis.protocol.CommandType.SUBSCRIBE;
import static com.lambdaworks.redis.protocol.CommandType.UNSUBSCRIBE;

import com.lambdaworks.redis.RedisAsyncCommandsImpl;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * An asynchronous thread-safe API to a redis connection
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class RedisPubSubAsyncCommandsImpl<K, V> extends RedisAsyncCommandsImpl<K, V> implements RedisPubSubConnection<K, V>,
        RedisPubSubAsyncCommands<K, V> {

    /**
     * Initialize a new connection.
     * 
     * @param connection the connection .
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisPubSubAsyncCommandsImpl(StatefulRedisPubSubConnection<K, V> connection, RedisCodec<K, V> codec) {
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
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> psubscribe(K... patterns) {
        return (RedisFuture<Void>) dispatch(PSUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(patterns));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> punsubscribe(K... patterns) {
        return (RedisFuture<Void>) dispatch(PUNSUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(patterns));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> subscribe(K... channels) {
        return (RedisFuture<Void>) dispatch(SUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(channels));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> unsubscribe(K... channels) {
        return (RedisFuture<Void>) dispatch(UNSUBSCRIBE, new PubSubOutput<K, V, K>(codec), args(channels));
    }

    private CommandArgs<K, V> args(K... keys) {
        CommandArgs<K, V> args = new CommandArgs<K, V>(codec);
        args.addKeys(keys);
        return args;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StatefulRedisPubSubConnection<K, V> getStatefulConnection() {
        return (StatefulRedisPubSubConnection<K, V>) super.getStatefulConnection();
    }
}
