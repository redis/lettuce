package io.lettuce.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursor providing a list of keys.
 *
 * @param <K> Key type.
 * @author Mark Paluch
 * @since 3.0
 */
public class KeyScanCursor<K> extends ScanCursor {

    private final List<K> keys = new ArrayList<>();

    public List<K> getKeys() {
        return keys;
    }

}
