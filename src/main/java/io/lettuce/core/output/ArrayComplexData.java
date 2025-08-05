/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of the {@link ComplexData} that handles arrays.
 * <p>
 * For RESP2 calling the {@link ComplexData#getDynamicMap()} would heuristically go over the list of elements assuming every odd
 * element is a key and every even object is the value and then adding them to an {@link Map}. The logic would follow the same
 * order that was used when the elements were added to the {@link ArrayComplexData}. Similarly calling the
 * {@link ComplexData#getDynamicSet()} would return a set of all the elements, adding them in the same order. If - for some
 * reason - duplicate elements exist they would be overwritten.
 * <p>
 * All data structures that the implementation returns are unmodifiable
 *
 * @see ComplexData
 * @author Tihomir Mateev
 * @since 6.5
 */
class ArrayComplexData extends ComplexData {

    private final List<Object> data;

    public ArrayComplexData(int count) {
        // RESP2 response for array data might end up returning -1 here if there are no results to process
        if (count > 0) {
            data = new ArrayList<>(count);
        } else {
            data = new ArrayList<>(0);
        }
    }

    @Override
    public void storeObject(Object value) {
        data.add(value);
    }

    @Override
    public List<Object> getDynamicList() {
        return Collections.unmodifiableList(data);
    }

    @Override
    public Set<Object> getDynamicSet() {
        // RESP2 compatibility mode - assuming the caller is aware that the array really contains a set (because in RESP2 we
        // lack support for this data type) we make the conversion here
        Set<Object> set = new LinkedHashSet<>(data);
        return Collections.unmodifiableSet(set);
    }

    @Override
    public Map<Object, Object> getDynamicMap() {
        // RESP2 compatibility mode - assuming the caller is aware that the array really contains a map (because in RESP2 we
        // lack support for this data type) we make the conversion here
        Map<Object, Object> map = new LinkedHashMap<>();
        final Boolean[] isKey = { true };
        final Object[] key = new Object[1];

        data.forEach(element -> {
            if (isKey[0]) {
                key[0] = element;
                isKey[0] = false;
            } else {
                map.put(key[0], element);
                isKey[0] = true;
            }
        });

        return Collections.unmodifiableMap(map);
    }

    @Override
    public boolean isList() {
        return true;
    }

}
