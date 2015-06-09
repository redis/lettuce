// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * An asynchronous thread-safe API to a redis connection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class RedisAsyncConnectionImpl<K, V> extends BaseRedisAsyncCommands<K, V> implements RedisAsyncConnection<K, V>,
        RedisClusterAsyncConnection<K, V> {

    /**
     * Initialize a new instance.
     * 
     */
    public RedisAsyncConnectionImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    public StatefulRedisConnection<K, V> getStatefulConnection() {
        return (StatefulRedisConnection) connection;
    }
}
