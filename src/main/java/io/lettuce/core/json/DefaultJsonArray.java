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

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

class DefaultJsonArray<K, V> extends DefaultJsonValue<K, V> implements JsonArray<K, V> {

    List<JsonValue<K, V>> elements = new ArrayList<>();

    DefaultJsonArray(RedisCodec<K, V> codec) {
        super(ByteBuffer.allocate(0), codec);
    }

    @Override
    public void add(JsonValue<K, V> element) {
        elements.add(element);
    }

    @Override
    public void addAll(JsonArray<K, V> element) {
        elements.addAll(element.asList());
    }

    @Override
    public List<JsonValue<K, V>> asList() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public JsonValue<K, V> get(int index) {
        return elements.get(index);
    }

    @Override
    public JsonValue<K, V> getFirst() {
        return elements.get(0);
    }

    @Override
    public Iterator<JsonValue<K, V>> iterator() {
        return elements.iterator();
    }

    @Override
    public JsonValue<K, V> remove(int index) {
        return elements.remove(index);
    }

    @Override
    public boolean remove(JsonValue<K, V> element) {
        return elements.remove(element);
    }

    @Override
    public JsonValue<K, V> replace(int index, JsonValue<K, V> newElement) {
        return elements.set(index, newElement);
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isJsonArray() {
        return true;
    }

    @Override
    public JsonArray<K, V> asJsonArray() {
        return this;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public String asString() {
        throw new UnsupportedOperationException("The JSON value is not a string");
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public Number asNumber() {
        throw new UnsupportedOperationException("The JSON value is not a number");
    }

}
