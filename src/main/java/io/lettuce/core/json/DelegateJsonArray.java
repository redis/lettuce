/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.lettuce.core.codec.RedisCodec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of the {@link DelegateJsonArray} that delegates most of it's dunctionality to the Jackson {@link ArrayNode}.
 *
 * @param <V> Value type.
 * @author Tihomir Mateev
 */
class DelegateJsonArray<V> extends DelegateJsonValue<V> implements JsonArray<V> {

    DelegateJsonArray(RedisCodec<?, V> codec) {
        super(new ArrayNode(JsonNodeFactory.instance), codec);
    }

    DelegateJsonArray(JsonNode node, RedisCodec<?, V> codec) {
        super(node, codec);
    }

    @Override
    public JsonArray<V> add(JsonValue<V> element) {
        JsonNode newNode = ((DelegateJsonValue<V>) element).getNode();
        ((ArrayNode) node).add(newNode);

        return this;
    }

    @Override
    public void addAll(JsonArray<V> element) {
        ArrayNode otherArray = (ArrayNode) ((DelegateJsonValue<V>) element).getNode();
        ((ArrayNode) node).addAll(otherArray);
    }

    @Override
    public List<JsonValue<V>> asList() {
        List<JsonValue<V>> result = new ArrayList<>();

        for (JsonNode jsonNode : node) {
            result.add(new DelegateJsonValue<>(jsonNode, codec));
        }

        return result;
    }

    @Override
    public JsonValue<V> get(int index) {
        JsonNode jsonNode = node.get(index);

        return new DelegateJsonValue<>(jsonNode, codec);
    }

    @Override
    public JsonValue<V> getFirst() {
        return get(0);
    }

    @Override
    public Iterator<JsonValue<V>> iterator() {
        List<JsonValue<V>> result = new ArrayList<>();
        while (node.iterator().hasNext()) {
            JsonNode jsonNode = node.iterator().next();
            result.add(new DelegateJsonValue<>(jsonNode, codec));
        }

        return result.iterator();
    }

    @Override
    public JsonValue<V> remove(int index) {
        JsonNode jsonNode = ((ArrayNode) node).remove(index);

        return new DelegateJsonValue<>(jsonNode, codec);
    }

    @Override
    public JsonValue<V> replace(int index, JsonValue<V> newElement) {
        JsonNode replaceWith = ((DelegateJsonValue<V>) newElement).getNode();
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
    public JsonArray<V> asJsonArray() {
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
