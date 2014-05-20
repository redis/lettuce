package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 20.05.14 14:40
 */
public class KeyScanCursor<K> extends ScanCursor<List<K>> {

    public KeyScanCursor() {
        this(new ArrayList<K>());
    }

    public KeyScanCursor(List<K> list) {
        setResult(list);
    }

    public List<K> getKeys() {
        return getResult();
    }
}
