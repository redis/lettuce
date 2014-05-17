package com.lambdaworks.redis.output;

/**
 * Streaming API for multiple Key-Values. You can implement this interface in order to receive a call to <code>onKeyValue</code>
 * on every key-value pair. Key uniqueness is not guaranteed.
 * 
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 17.05.14 16:18
 */
public interface KeyValueStreamingChannel<K, V> {
    /**
     * 
     * @param key
     * @param value
     */
    void onKeyValue(K key, V value);
}
