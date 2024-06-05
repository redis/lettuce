/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

package io.lettuce.core.output.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of the {@link DynamicAggregateData} that handles arrays.
 * <p>
 * For RESP2 calling the {@link DynamicAggregateData#getDynamicMap()} would heuristically go over the list of elements assuming
 * every odd element is a key and every even object is the value and then adding them to an {@link Map}. The logic would follow
 * the same order that was used when the elements were added to the {@link ArrayAggregateData}. Similarly calling the
 * {@link DynamicAggregateData#getDynamicSet()} would return a set of all the elements, adding them in the same order. If - for
 * some reason - duplicate elements exist they would be overwritten.
 * <p>
 * All data structures that the implementation returns are unmodifiable
 *
 * @see DynamicAggregateData
 * @author Tihomir Mateev
 * @since 7.0
 */
public class ArrayAggregateData extends DynamicAggregateData {

    private final List<Object> data;

    public ArrayAggregateData(int count) {
        data = new ArrayList<>(count);
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

}
