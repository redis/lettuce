package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursor providing a list of keys.
 * 
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 20.05.14 14:40
 */
public class KeyScanCursor<K> extends ScanCursor {

    private List<K> keys = new ArrayList<K>();

    public List<K> getKeys() {
        return keys;
    }
}
