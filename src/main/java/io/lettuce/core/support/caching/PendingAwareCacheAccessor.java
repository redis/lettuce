package io.lettuce.core.support.caching;

import java.util.Map;

/**
 * {@link CacheAccessor} implementation for {@link Map}-based cache implementations.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch, Ivar Henckel
 * @since 6.0
 */
class PendingAwareCacheAccessor<K, V> implements CacheAccessor<K, V> {

    private static final Object REDIS_IN_PROGRESS = new Object();

    private final Map<K, V> map;

    PendingAwareCacheAccessor(Map<K, V> map) {
        this.map = map;
    }

    @Override
    public V get(K key) {
        V value = map.get(key);
        if (value == REDIS_IN_PROGRESS) {
            return null;
        }
        return value;
    }

    @Override
    public void put(K key, V value) {
        map.replace(key, (V) REDIS_IN_PROGRESS, value);
    }

    @Override
    public void evict(K key) {
        map.remove(key);
    }

    @Override
    public void setPending(K key) {
        map.put(key, (V) REDIS_IN_PROGRESS);
    }

}
