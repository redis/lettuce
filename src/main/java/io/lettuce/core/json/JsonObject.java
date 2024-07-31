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

import java.util.Map;

/**
 * Representation of a JSON object as per <a href="https://datatracker.ietf.org/doc/html/rfc8259#section-4"> </a>RFC 8259 - The
 * JavaScript Object Notation (JSON) Data Interchange Format, Section 4. Objects</a>
 * <p>
 *
 *
 * @param <K> Key type based on the {@link io.lettuce.core.codec.RedisCodec} used.
 * @param <V> Value type based on the {@link io.lettuce.core.codec.RedisCodec} used.
 * @see JsonValue
 * @since 6.5
 */
public interface JsonObject<K, V> extends JsonValue<K, V> {

    void add(K key, JsonValue<K, V> element);

    Map<K, JsonValue<K, V>> asMap();

    JsonValue<K, V> get(K key);

    JsonValue<K, V> remove(K key);

    int size();

}
