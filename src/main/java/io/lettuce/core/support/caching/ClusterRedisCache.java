package io.lettuce.core.support.caching;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.RedisCodec;

/**
 * {@link RedisCache} implementation for Redis Cluster using {@code GET} and {@code SET} operations to map cache values to
 * top-level keys. Invalidation messages are consumed from all cluster node connections through the cluster push listener.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Julien Ruaux
 * @since 7.7
 */
class ClusterRedisCache<K, V> implements RedisCache<K, V> {

    private final StatefulRedisClusterConnection<K, V> connection;

    private final RedisCodec<K, V> codec;

    private final List<Runnable> clearListeners = new CopyOnWriteArrayList<>();

    public ClusterRedisCache(StatefulRedisClusterConnection<K, V> connection, RedisCodec<K, V> codec) {
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

        connection.addListener((node, message) -> {
            if (message.getType().equals("invalidate")) {

                List<Object> content = message.getContent(codec::decodeKey);
                List<K> keys = (List<K>) content.get(1);

                if (keys == null) {
                    // null payload indicates a full invalidation, e.g. after FLUSHALL/FLUSHDB
                    clearListeners.forEach(Runnable::run);
                } else {
                    keys.forEach(listener);
                }
            }
        });
    }

    @Override
    public void addClearListener(Runnable listener) {
        clearListeners.add(listener);
    }

    @Override
    public void close() {
        connection.close();
    }

}
