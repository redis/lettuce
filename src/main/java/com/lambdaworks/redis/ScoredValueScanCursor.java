package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursor providing a list of {@link com.lambdaworks.redis.ScoredValue}
 * 
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 20.05.14 14:40
 */
public class ScoredValueScanCursor<V> extends ScanCursor {

    private final List<ScoredValue<V>> values = new ArrayList<ScoredValue<V>>();

    public ScoredValueScanCursor() {
    }

    public List<ScoredValue<V>> getValues() {
        return values;
    }
}
