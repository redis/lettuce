package io.lettuce.test;

import java.util.*;

/**
 * Keys for testing slot-hashes.
 *
 * @author Mark Paluch
 */
public class KeysAndValues {

    /**
     * Ordered list of keys. The order corresponds with the list of {@code VALUES}.
     */
    public static final List<String> KEYS;

    /**
     * Ordered list of values. The order corresponds with the list of {@code KEYS}.
     */
    public static final List<String> VALUES;

    /**
     * Mapping between {@code KEYS} and {@code VALUES}
     */
    public static final Map<String, String> MAP;

    /**
     * Number of entries.
     */
    public static final int COUNT = 500;

    static {

        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < COUNT; i++) {

            String key = "key-" + i;
            String value = "value-" + i;

            keys.add(key);
            values.add(value);
            map.put(key, value);
        }

        KEYS = Collections.unmodifiableList(keys);
        VALUES = Collections.unmodifiableList(values);
        MAP = Collections.unmodifiableMap(map);
    }
}
