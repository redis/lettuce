package com.lambdaworks;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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

        List<String> keys = Lists.newArrayList();
        List<String> values = Lists.newArrayList();
        Map<String, String> map = Maps.newHashMap();

        for (int i = 0; i < COUNT; i++) {

            String key = RandomStringUtils.random(10, true, true);
            String value = RandomStringUtils.random(10, true, true);

            keys.add(key);
            values.add(value);
            map.put(key, value);
        }

        KEYS = ImmutableList.copyOf(keys);
        VALUES = ImmutableList.copyOf(values);
        MAP = ImmutableMap.copyOf(map);
    }

}
