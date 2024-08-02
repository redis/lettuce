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

/**
 * Representation of a JSON array as per <a href="https://datatracker.ietf.org/doc/html/rfc8259#section-5"> </a>RFC 8259 - The
 * JavaScript Object Notation (JSON) Data Interchange Format, Section 5. Arrays</a>
 * <p>
 *
 *
 * @param <K> Key type based on the {@link io.lettuce.core.codec.RedisCodec} used.
 * @param <V> Value type based on the {@link io.lettuce.core.codec.RedisCodec} used.
 * @see JsonValue
 * @author Tihomir Mateev
 * @since 6.5
 */
public interface JsonArray<K, V> extends JsonValue<K, V> {

    /**
     * Add a new {@link JsonValue} to the array. Supports chaining of calls.
     *
     * @param element the value to add
     * @return the updated {@link JsonArray} to allow call chaining
     */
    JsonArray<K, V> add(JsonValue<K, V> element);

    /**
     * Add all elements from the provided {@link JsonArray} to this array.
     *
     * @param element the array to add all elements from
     */
    void addAll(JsonArray<K, V> element);

    /**
     * Get all the {@link JsonValue}s in the array as a {@link List}.
     *
     * @return the {@link List} of {@link JsonValue}s in the array
     */
    List<JsonValue<K, V>> asList();

    /**
     * Get the {@link JsonValue} at the provided index.
     *
     * @param index the index to get the value for
     * @return the {@link JsonValue} at the provided index or {@code null} if no value is found
     */
    JsonValue<K, V> get(int index);

    /**
     * Get the first {@link JsonValue} in the array.
     *
     * @return the first {@link JsonValue} in the array or {@code null} if the array is empty
     */
    JsonValue<K, V> getFirst();

    /**
     * Get an {@link Iterator} allowing access to all the  {@link JsonValue}s in the array.
     *
     * @return the last {@link JsonValue} in the array or {@code null} if the array is empty
     */
    Iterator<JsonValue<K, V>> iterator();

    /**
     * Remove the {@link JsonValue} at the provided index.
     *
     * @param index the index to remove the value for
     * @return the removed {@link JsonValue} or {@code null} if no value is found
     */
    JsonValue<K, V> remove(int index);

    /**
     * Replace the {@link JsonValue} at the provided index with the provided new {@link JsonValue}.
     *
     * @param index the index to replace the value for
     * @param newElement the new value to replace the old one with
     * @return the updated {@link JsonArray} to allow call chaining
     */
    JsonValue<K, V> replace(int index, JsonValue<K, V> newElement);

    /**
     * @return the number of elements in this {@link JsonArray}
     */
    int size();

}
