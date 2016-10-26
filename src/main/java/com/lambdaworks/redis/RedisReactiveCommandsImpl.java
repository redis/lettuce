/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * A reactive and thread-safe API for a Redis Sentinel connection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class RedisReactiveCommandsImpl<K, V> extends AbstractRedisReactiveCommands<K, V> implements
        RedisReactiveCommands<K, V>, RedisClusterReactiveCommands<K, V> {

    /**
     * Initialize a new instance.
     *
     * @param connection the connection to operate on
     * @param codec the codec for command encoding
     *
     */
    public RedisReactiveCommandsImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StatefulRedisConnection<K, V> getStatefulConnection() {
        return (StatefulRedisConnection<K, V>) connection;
    }
}
