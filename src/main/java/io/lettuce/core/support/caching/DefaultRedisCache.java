/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.support.caching;

import java.util.List;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;

/**
 * Default {@link RedisCache} implementation using {@code GET} and {@code SET} operations to map cache values to top-level keys.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
class DefaultRedisCache<K, V> implements RedisCache<K, V> {

    private final StatefulRedisConnection<K, V> connection;

    private final RedisCodec<K, V> codec;

    public DefaultRedisCache(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
        this.connection = connection;
        this.codec = codec;
    }

    @Override
    public V get(K key) {
        return connection.sync().get(key);
    }

    @Override
    public void put(K key, V value) {
        connection.sync().set(key, value);
    }

    @Override
    public void addInvalidationListener(java.util.function.Consumer<? super K> listener) {

        connection.addListener(message -> {
            if (message.getType().equals("invalidate")) {

                List<Object> content = message.getContent(codec::decodeKey);
                List<K> keys = (List<K>) content.get(1);
                keys.forEach(listener);
            }
        });
    }

    @Override
    public void close() {
        connection.close();
    }

}
