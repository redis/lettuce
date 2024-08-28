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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.lettuce.core.codec.RedisCodec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of the {@link DelegateJsonArray} that delegates most of it's dunctionality to the Jackson {@link ArrayNode}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 */
class DelegateJsonArray<K, V> extends DelegateJsonValue<K, V> implements JsonArray<K, V> {

    DelegateJsonArray(RedisCodec<K, V> codec) {
        super(new ArrayNode(JsonNodeFactory.instance), codec);
    }

    DelegateJsonArray(JsonNode node, RedisCodec<K, V> codec) {
        super(node, codec);
    }

    @Override
    public JsonArray<K, V> add(JsonValue<K, V> element) {
        JsonNode newNode = ((DelegateJsonValue<K, V>) element).getNode();
        ((ArrayNode) node).add(newNode);

        return this;
    }

    @Override
    public void addAll(JsonArray<K, V> element) {
        ArrayNode otherArray = (ArrayNode) ((DelegateJsonValue<K, V>) element).getNode();
        ((ArrayNode) node).addAll(otherArray);
    }

    @Override
    public List<JsonValue<K, V>> asList() {
        List<JsonValue<K, V>> result = new ArrayList<>();

        for (JsonNode jsonNode : node) {
            result.add(new DelegateJsonValue<>(jsonNode, codec));
        }

        return result;
    }

    @Override
    public JsonValue<K, V> get(int index) {
        JsonNode jsonNode = node.get(index);

        return new DelegateJsonValue<>(jsonNode, codec);
    }

    @Override
    public JsonValue<K, V> getFirst() {
        return get(0);
    }

    @Override
    public Iterator<JsonValue<K, V>> iterator() {
        List<JsonValue<K, V>> result = new ArrayList<>();
        while (node.iterator().hasNext()) {
            JsonNode jsonNode = node.iterator().next();
            result.add(new DelegateJsonValue<>(jsonNode, codec));
        }

        return result.iterator();
    }

    @Override
    public JsonValue<K, V> remove(int index) {
        JsonNode jsonNode = ((ArrayNode) node).remove(index);

        return new DelegateJsonValue<>(jsonNode, codec);
    }

    @Override
    public JsonValue<K, V> replace(int index, JsonValue<K, V> newElement) {
        JsonNode replaceWith = ((DelegateJsonValue<K, V>) newElement).getNode();
        JsonNode replaced = ((ArrayNode) node).set(index, replaceWith);

        return new DelegateJsonValue<>(replaced, codec);
    }

    @Override
    public int size() {
        return node.size();
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

    @Override
    public boolean isNull() {
        return false;
    }

}
