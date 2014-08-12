package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursor providing a list of keys.
 * 
 * @param <K> Key type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class KeyScanCursor<K> extends ScanCursor {

    private final List<K> keys = new ArrayList<K>();

    public List<K> getKeys() {
        return keys;
    }
}
