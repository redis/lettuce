package io.lettuce.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scan cursor for maps.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class MapScanCursor<K, V> extends ScanCursor {

    private final Map<K, V> map = new LinkedHashMap<>();

    /**
     *
     * @return the map result.
     */
    public Map<K, V> getMap() {
        return map;
    }

}
