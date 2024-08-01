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

class DefaultJsonObject<K, V> extends DefaultJsonValue<K, V> implements JsonObject<K, V> {

    DefaultJsonObject(RedisCodec<K, V> codec) {
        super(new ObjectNode(JsonNodeFactory.instance), codec);
    }

    DefaultJsonObject(JsonNode node, RedisCodec<K, V> codec) {
        super(node, codec);
    }

    @Override
    public JsonObject<K, V> add(K key, JsonValue<K, V> element) {
        String keyString = getStringValue(key);
        JsonNode newNode = ((DefaultJsonValue<K, V>) element).getNode();

        ((ObjectNode) node).replace(keyString, newNode);
        return this;
    }

    @Override
    public JsonValue<K, V> get(K key) {
        String keyString = getStringValue(key);
        JsonNode value = node.get(keyString);

        return new DefaultJsonValue<>(value, codec);
    }

    @Override
    public JsonValue<K, V> remove(K key) {
        String keyString = getStringValue(key);
        JsonNode value = ((ObjectNode) node).remove(keyString);

        return new DefaultJsonValue<>(value, codec);
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

}
