package io.lettuce.core.support.caching;

import java.util.Map;

/**
 * {@link CacheAccessor} implementation for {@link Map}-based cache implementations.
 * <p>
 * This accessor tracks in-flight Redis reads using a private sentinel value so that read-through writes can be rejected when a
 * concurrent invalidation arrives while a value is being loaded from Redis (see {@link #setPending(Object)} and
 * {@link #putIfPending(Object, Object)}). To get atomic guarantees the backing {@link Map} should support atomic compound
 * operations (e.g. {@link java.util.concurrent.ConcurrentHashMap}).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @author Ivar Henckel
 * @since 6.0
 */
class PendingAwareCacheAccessor<K, V> implements CacheAccessor<K, V> {

    /**
     * Sentinel marking a key whose value is currently being loaded from Redis. Stored in the backing map so that an
     * invalidation (which removes the entry) can be detected by a concurrent read-through write.
     */
    private static final Object REDIS_IN_PROGRESS = new Object();

    private final Map<K, V> map;

    PendingAwareCacheAccessor(Map<K, V> map) {
        this.map = map;
    }

    @Override
    public V get(K key) {

        V value = map.get(key);

        // hide the internal sentinel from callers; a pending key has no resolved value yet
        if (value == REDIS_IN_PROGRESS) {
            return null;
        }

        return value;
    }

    @Override
    public void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putIfPending(K key, V value) {
        // only store the value if the key is still pending, i.e. no invalidation removed the sentinel in the meantime
        map.replace(key, (V) REDIS_IN_PROGRESS, value);
    }

    @Override
    public void evict(K key) {
        map.remove(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setPending(K key) {
        map.put(key, (V) REDIS_IN_PROGRESS);
    }

}
