package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursor providing a list of values.
 * 
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 20.05.14 14:40
 */
public class ValueScanCursor<V> extends ScanCursor {

    private final List<V> values = new ArrayList<V>();

    public ValueScanCursor() {
    }

    public List<V> getValues() {
        return values;
    }
}
