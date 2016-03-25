// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * An asynchronous and thread-safe API for a Redis connection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class RedisAsyncCommandsImpl<K, V> extends AbstractRedisAsyncCommands<K, V> implements RedisAsyncConnection<K, V>,
        RedisClusterAsyncConnection<K, V>, RedisAsyncCommands<K, V>, RedisClusterAsyncCommands<K, V> {

    /**
     * Initialize a new instance.
     * 
     * @param connection the connection to operate on
     * @param codec the codec for command encoding
     * 
     */
    public RedisAsyncCommandsImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StatefulRedisConnection<K, V> getStatefulConnection() {
        return (StatefulRedisConnection<K, V>) connection;
    }
}
