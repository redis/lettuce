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

package io.lettuce.core.json;

import java.util.Iterator;
import java.util.List;

public interface JsonArray<V> extends JsonValue<V> {

    void add(JsonValue<V> element);

    void addAll(JsonArray<V> element);

    List<JsonValue<V>> asList();

    Iterator<JsonValue<V>> iterator();

    JsonValue<V> remove(int index);

    boolean remove(JsonValue<V> element);

    JsonValue<V> replace(int index, JsonValue<V> newElement);

    int size();

}
