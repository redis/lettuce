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
import rx.Observable;

import java.util.List;
import java.util.Map;

/**
 * An asynchronous and thread-safe API for a Redis pub/sub connection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class RedisPubSubAsyncCommandsImpl<K, V> extends RedisAsyncCommandsImpl<K, V> implements RedisPubSubConnection<K, V>,
        RedisPubSubAsyncCommands<K, V> {

    private PubSubCommandBuilder<K, V> commandBuilder;

    /**
     * Initialize a new connection.
     * 
     * @param connection the connection .
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisPubSubAsyncCommandsImpl(StatefulRedisPubSubConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.connection = connection;
        this.commandBuilder = new PubSubCommandBuilder<>(codec);
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
        return (RedisFuture<Void>) dispatch(commandBuilder.psubscribe(patterns));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> punsubscribe(K... patterns) {
        return (RedisFuture<Void>) dispatch(commandBuilder.punsubscribe(patterns));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> subscribe(K... channels) {
        return (RedisFuture<Void>) dispatch(commandBuilder.subscribe(channels));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> unsubscribe(K... channels) {
        return (RedisFuture<Void>) dispatch(commandBuilder.unsubscribe(channels));
    }

    @Override
    public RedisFuture<Long> publish(K channel, V message) {
        return dispatch(commandBuilder.publish(channel, message));
    }

    @Override
    public RedisFuture<List<K>> pubsubChannels(K channel) {
        return dispatch(commandBuilder.pubsubChannels(channel));
    }

    @Override
    public RedisFuture<Map<K, Long>> pubsubNumsub(K... channels) {
        return dispatch(commandBuilder.pubsubNumsub(channels));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StatefulRedisPubSubConnection<K, V> getStatefulConnection() {
        return (StatefulRedisPubSubConnection<K, V>) super.getStatefulConnection();
    }
}
