package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursor providing a list of {@link com.lambdaworks.redis.ScoredValue}
 * 
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public class ScoredValueScanCursor<V> extends ScanCursor {

    private final List<ScoredValue<V>> values = new ArrayList<>();

    public ScoredValueScanCursor() {
    }

    public List<ScoredValue<V>> getValues() {
        return values;
    }
}
