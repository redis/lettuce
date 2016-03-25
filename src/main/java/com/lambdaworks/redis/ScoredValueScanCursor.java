package com.lambdaworks.redis;

import java.util.List;

import com.lambdaworks.redis.internal.LettuceLists;

/**
 * Cursor providing a list of {@link com.lambdaworks.redis.ScoredValue}
 * 
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class ScoredValueScanCursor<V> extends ScanCursor {

    private final List<ScoredValue<V>> values = LettuceLists.newList();

    public ScoredValueScanCursor() {
    }

    public List<ScoredValue<V>> getValues() {
        return values;
    }
}
