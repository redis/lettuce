package com.lambdaworks.redis;

import com.lambdaworks.redis.output.KeyValueStreamingChannel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 17:22
 */
public class KeyValueStreamingAdapter<K, V> implements KeyValueStreamingChannel<K, V> {

    private Map<K, V> map = new HashMap<K, V>();

    @Override
    public void onKeyValue(K key, V value) {
        map.put(key, value);
    }

    public Map<K, V> getMap() {
        return map;
    }
}
