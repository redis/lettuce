package com.lambdaworks.redis;

import java.util.Map;

import com.lambdaworks.redis.internal.LettuceMaps;

/**
 * Scan cursor for maps.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class MapScanCursor<K, V> extends ScanCursor {

    private final Map<K, V> map = LettuceMaps.newLinkedHashMap();

    /**
     * 
     * @return the map result.
     */
    public Map<K, V> getMap() {
        return map;
    }
}
