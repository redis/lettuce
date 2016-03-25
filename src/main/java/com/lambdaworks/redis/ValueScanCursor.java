package com.lambdaworks.redis;

import java.util.List;

import com.lambdaworks.redis.internal.LettuceLists;

/**
 * Cursor providing a list of values.
 * 
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class ValueScanCursor<V> extends ScanCursor {

    private final List<V> values = LettuceLists.newList();

    public ValueScanCursor() {
    }

    public List<V> getValues() {
        return values;
    }
}
