// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * An asynchronous thread-safe API to a redis connection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class RedisAsyncConnectionCommandsImpl<K, V> extends AbstractRedisAsyncCommands<K, V> implements
        RedisAsyncConnection<K, V>, RedisClusterAsyncConnection<K, V>, RedisAsyncCommands<K, V>,
        RedisClusterAsyncCommands<K, V> {

    /**
     * Initialize a new instance.
     * 
     */
    public RedisAsyncConnectionCommandsImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    public StatefulRedisConnection<K, V> getStatefulConnection() {
        return (StatefulRedisConnection) connection;
    }
}
