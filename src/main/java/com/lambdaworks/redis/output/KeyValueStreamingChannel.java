package com.lambdaworks.redis.output;

/**
 * Streaming API for multiple Key-Values. You can implement this interface in order to receive a call to {@code onKeyValue} on
 * every key-value pair. Key uniqueness is not guaranteed.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
@FunctionalInterface
public interface KeyValueStreamingChannel<K, V> {
    /**
     * 
     * Called on every incoming key/value pair.
     * 
     * @param key the key
     * @param value the value
     */
    void onKeyValue(K key, V value);
}
