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
 * Implementation of the {@link DelegateJsonArray} that delegates most of its' functionality to the Jackson {@link ArrayNode}.
 *
 * @author Tihomir Mateev
 */
class DelegateJsonArray extends DelegateJsonValue implements JsonArray {

    DelegateJsonArray() {
        super(new ArrayNode(JsonNodeFactory.instance));
    }

    DelegateJsonArray(JsonNode node) {
        super(node);
    }

    @Override
    public JsonArray add(JsonValue element) {
        JsonNode newNode = ((DelegateJsonValue) element).getNode();
        ((ArrayNode) node).add(newNode);

        return this;
    }

    @Override
    public void addAll(JsonArray element) {
        ArrayNode otherArray = (ArrayNode) ((DelegateJsonValue) element).getNode();
        ((ArrayNode) node).addAll(otherArray);
    }

    @Override
    public List<JsonValue> asList() {
        List<JsonValue> result = new ArrayList<>();

        for (JsonNode jsonNode : node) {
            result.add(new DelegateJsonValue(jsonNode));
        }

        return result;
    }

    @Override
    public JsonValue get(int index) {
        JsonNode jsonNode = node.get(index);

        return new DelegateJsonValue(jsonNode);
    }

    @Override
    public JsonValue getFirst() {
        return get(0);
    }

    @Override
    public Iterator<JsonValue> iterator() {
        List<JsonValue> result = new ArrayList<>();
        while (node.iterator().hasNext()) {
            JsonNode jsonNode = node.iterator().next();
            result.add(new DelegateJsonValue(jsonNode));
        }

        return result.iterator();
    }

    @Override
    public JsonValue remove(int index) {
        JsonNode jsonNode = ((ArrayNode) node).remove(index);

        return new DelegateJsonValue(jsonNode);
    }

    @Override
    public JsonValue replace(int index, JsonValue newElement) {
        JsonNode replaceWith = ((DelegateJsonValue) newElement).getNode();
        JsonNode replaced = ((ArrayNode) node).set(index, replaceWith);

        return new DelegateJsonValue(replaced);
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
    public JsonArray asJsonArray() {
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
