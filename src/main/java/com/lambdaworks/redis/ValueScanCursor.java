package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 20.05.14 14:40
 */
public class ValueScanCursor<V> extends ScanCursor<List<V>> {

    public ValueScanCursor() {
        this(new ArrayList<V>());
    }

    public ValueScanCursor(List<V> list) {
        setResult(list);
    }

    public List<V> getValues() {
        return getResult();
    }
}
