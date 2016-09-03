package com.lambdaworks;

import java.util.*;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * Random keys for testing slot-hashes.
 * 
 * @author Mark Paluch
 */
public class RandomKeys {

    /**
     * Ordered list of random keys. The order corresponds with the list of {@code VALUES}.
     */
    public static final List<String> KEYS;

    /**
     * Ordered list of random values. The order corresponds with the list of {@code KEYS}.
     */
    public static final List<String> VALUES;

    /**
     * Mapping between {@code KEYS} and {@code VALUES}
     */
    public static final Map<String, String> MAP;

    /**
     * Number of entries.
     */
    public final static int COUNT = 500;

    static {

        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        Set<String> uniqueKeys = new HashSet<>();

        while (map.size() < COUNT) {

            String key = RandomStringUtils.random(10, true, true);
            String value = RandomStringUtils.random(10, true, true);

            if (uniqueKeys.add(key)) {

                keys.add(key);
                values.add(value);
                map.put(key, value);
            }
        }

        KEYS = Collections.unmodifiableList(keys);
        VALUES = Collections.unmodifiableList(values);
        MAP = Collections.unmodifiableMap(map);
    }

}
