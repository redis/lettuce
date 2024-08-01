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

public interface JsonArray<K, V> extends JsonValue<K, V> {

    JsonArray<K, V> add(JsonValue<K, V> element);

    void addAll(JsonArray<K, V> element);

    List<JsonValue<K, V>> asList();

    JsonValue<K, V> get(int index);

    JsonValue<K, V> getFirst();

    Iterator<JsonValue<K, V>> iterator();

    JsonValue<K, V> remove(int index);

    JsonValue<K, V> replace(int index, JsonValue<K, V> newElement);

    int size();

}
