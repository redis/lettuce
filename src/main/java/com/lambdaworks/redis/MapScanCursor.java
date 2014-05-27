package com.lambdaworks.redis;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 20.05.14 14:40
 */
public class MapScanCursor<K, V> extends ScanCursor {

    private Map<K, V> map = new HashMap<K, V>();

    public Map<K, V> getMap() {
        return map;
    }
}
