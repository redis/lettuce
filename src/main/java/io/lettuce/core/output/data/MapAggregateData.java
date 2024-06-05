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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the {@link DynamicAggregateData} that handles maps.
 * <p>
 * All data structures that the implementation returns are unmodifiable
 *
 * @see DynamicAggregateData
 * @author Tihomir Mateev
 * @since 7.0
 */
public class MapAggregateData extends DynamicAggregateData {

    private final Map<Object, Object> data;

    private Object key;

    public MapAggregateData(int count) {
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

}
