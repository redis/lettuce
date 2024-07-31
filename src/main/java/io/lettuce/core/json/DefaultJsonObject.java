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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

class DefaultJsonObject<K, V> extends DefaultJsonValue<K, V> implements JsonObject<K, V> {

    Map<K, JsonValue<K, V>> fields;

    DefaultJsonObject(RedisCodec<K, V> codec) {
        super(ByteBuffer.allocate(0), codec);
    }

    @Override
    public void add(K key, JsonValue<K, V> element) {
        fields.put(key, element);
    }

    @Override
    public Map<K, JsonValue<K, V>> asMap() {
        return Collections.unmodifiableMap(fields);
    }

    @Override
    public JsonValue<K, V> get(K key) {
        return fields.get(key);
    }

    @Override
    public JsonValue<K, V> remove(K key) {
        return fields.remove(key);
    }

    @Override
    public int size() {
        return fields.size();
    }

    @Override
    public boolean isJsonObject() {
        return true;
    }

    @Override
    public JsonObject<K, V> asJsonObject() {
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
