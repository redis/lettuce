package com.lambdaworks.redis;

import java.util.Map;

import com.lambdaworks.redis.internal.LettuceMaps;
import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.KeyValueStreamingChannel;

/**
 * Adapter for a {@link KeyStreamingChannel}. Stores the output in a map.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class KeyValueStreamingAdapter<K, V> implements KeyValueStreamingChannel<K, V> {

    private final Map<K, V> map = LettuceMaps.newLinkedHashMap();

    @Override
    public void onKeyValue(K key, V value) {
        map.put(key, value);
    }

    public Map<K, V> getMap() {
        return map;
    }
}
