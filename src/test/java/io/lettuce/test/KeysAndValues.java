/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
