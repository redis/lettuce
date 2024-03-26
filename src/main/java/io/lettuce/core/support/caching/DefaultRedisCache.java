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
