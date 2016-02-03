package com.lambdaworks.redis;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Scan cursor for maps.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class MapScanCursor<K, V> extends ScanCursor {

    private final Map<K, V> map = Maps.newLinkedHashMap();

    /**
     * 
     * @return the map result.
     */
    public Map<K, V> getMap() {
        return map;
    }
}
