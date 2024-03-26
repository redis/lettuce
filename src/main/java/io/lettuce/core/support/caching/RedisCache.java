package io.lettuce.core.support.caching;

/**
 * Interface defining common Redis Cache operations.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.0
 */
public interface RedisCache<K, V> {

    /**
     * Retrieve a {@code value} from Redis for the given cache {@code key}.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value to which this Redis cache value maps the specified key (which may be {@code null} itself), or also
     *         {@code null} if the Redis cache contains no mapping for this key.
     */
    V get(K key);

    /**
     * Associate the specified value with the specified key in this Redis cache.
     *
     * @param key the key with which the specified value is to be associated.
     * @param value the value to be associated with the specified key.
     */
    void put(K key, V value);

    /**
     * Register a invalidation {@code listener} that is notified if a key in this Redis cache expires or gets modified.
     *
     * @param listener the listener to notify.
     */
    void addInvalidationListener(java.util.function.Consumer<? super K> listener);

    /**
     * Closes this Redis cache and releases any connections associated with it. If the cache is already closed then invoking
     * this method has no effect.
     */
    void close();

}
