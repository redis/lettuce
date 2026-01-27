/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the {@link ComplexData} that handles maps.
 * <p>
 * All data structures that the implementation returns are unmodifiable
 *
 * @see ComplexData
 * @author Tihomir Mateev
 * @since 6.5
 */
class MapComplexData extends ComplexData {

    private final Map<Object, Object> data;

    private Object key;

    public MapComplexData(int count) {
        data = new HashMap<>(count);
    }

    @Override
    public void storeObject(Object value) {
        if (key == null) {
            key = value;
        } else {
            data.put(key, value);
            key = null;
        }
    }

    @Override
    public Map<Object, Object> getDynamicMap() {
        return Collections.unmodifiableMap(data);
    }

    @Override
    public boolean isMap() {
        return true;
    }

}
