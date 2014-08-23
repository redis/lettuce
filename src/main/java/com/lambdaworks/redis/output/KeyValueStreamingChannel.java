package com.lambdaworks.redis.output;

/**
 * Streaming API for multiple Key-Values. You can implement this interface in order to receive a call to <code>onKeyValue</code>
 * on every key-value pair. Key uniqueness is not guaranteed.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public interface KeyValueStreamingChannel<K, V> {
    /**
     * 
     * Called on every incoming key/value pair.
     * 
     * @param key
     * @param value
     */
    void onKeyValue(K key, V value);
}
