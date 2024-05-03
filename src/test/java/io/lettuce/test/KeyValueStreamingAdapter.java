package io.lettuce.test;

import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.KeyValueStreamingChannel;

/**
 * Adapter for a {@link KeyStreamingChannel}. Stores the output in a map.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class KeyValueStreamingAdapter<K, V> implements KeyValueStreamingChannel<K, V> {

    private final Map<K, V> map = new LinkedHashMap<>();

    @Override
    public void onKeyValue(K key, V value) {

        synchronized (map) {
            map.put(key, value);
        }
    }

    public Map<K, V> getMap() {
        return map;
    }

}
