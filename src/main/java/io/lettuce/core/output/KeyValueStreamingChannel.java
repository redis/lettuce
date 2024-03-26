package io.lettuce.core.output;

/**
 * Streaming API for multiple keys and values (tuples). You can implement this interface in order to receive a call to
 * {@code onKeyValue} on every key-value.
 *
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.0
 */
@FunctionalInterface
public interface KeyValueStreamingChannel<K, V> extends StreamingChannel {

    /**
     * Called on every incoming key/value pair.
     *
     * @param key the key
     * @param value the value
     */
    void onKeyValue(K key, V value);

}
