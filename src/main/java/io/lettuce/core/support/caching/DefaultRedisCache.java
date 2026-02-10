package io.lettuce.core.support.caching;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.push.PushListener;
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

    private final boolean closeConnection;

    private final List<PushListener> listeners = new CopyOnWriteArrayList<>();

    public DefaultRedisCache(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
        this(connection, codec, true);
    }

    public DefaultRedisCache(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec, boolean closeConnection) {
        this.connection = connection;
        this.codec = codec;
        this.closeConnection = closeConnection;
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

        PushListener pushListener = message -> {
            if (message.getType().equals("invalidate")) {

                List<Object> content = message.getContent(codec::decodeKey);
                List<K> keys = (List<K>) content.get(1);
                keys.forEach(listener);
            }
        };

        listeners.add(pushListener);
        connection.addListener(pushListener);
    }

    @Override
    public void close() {
        for (PushListener listener : listeners) {
            connection.removeListener(listener);
        }
        listeners.clear();

        if (closeConnection) {
            connection.close();
        }
    }

}
