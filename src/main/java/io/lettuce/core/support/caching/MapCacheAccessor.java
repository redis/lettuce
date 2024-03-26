package io.lettuce.core.support.caching;

import java.util.Map;

/**
 * {@link CacheAccessor} implementation for {@link Map}-based cache implementations.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.0
 */
class MapCacheAccessor<K, V> implements CacheAccessor<K, V> {

    private final Map<K, V> map;

    MapCacheAccessor(Map<K, V> map) {
        this.map = map;
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    public void evict(K key) {
        map.remove(key);
    }

}
