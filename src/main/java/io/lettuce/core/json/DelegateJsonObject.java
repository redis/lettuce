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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.lettuce.core.codec.RedisCodec;

/**
 * Implementation of the {@link DelegateJsonObject} that delegates most of it's dunctionality to the Jackson {@link ObjectNode}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 */
class DelegateJsonObject<K, V> extends DelegateJsonValue<K, V> implements JsonObject<K, V> {

    DelegateJsonObject(RedisCodec<K, V> codec) {
        super(new ObjectNode(JsonNodeFactory.instance), codec);
    }

    DelegateJsonObject(JsonNode node, RedisCodec<K, V> codec) {
        super(node, codec);
    }

    @Override
    public JsonObject<K, V> put(K key, JsonValue<K, V> element) {
        String keyString = getStringValue(key);
        JsonNode newNode = ((DelegateJsonValue<K, V>) element).getNode();

        ((ObjectNode) node).replace(keyString, newNode);
        return this;
    }

    @Override
    public JsonValue<K, V> get(K key) {
        String keyString = getStringValue(key);
        JsonNode value = node.get(keyString);

        return new DelegateJsonValue<>(value, codec);
    }

    @Override
    public JsonValue<K, V> remove(K key) {
        String keyString = getStringValue(key);
        JsonNode value = ((ObjectNode) node).remove(keyString);

        return new DelegateJsonValue<>(value, codec);
    }

    @Override
    public int size() {
        return node.size();
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

    @Override
    public boolean isNull() {
        return false;
    }

}
