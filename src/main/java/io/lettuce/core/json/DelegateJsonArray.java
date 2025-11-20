/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.lettuce.core.internal.LettuceAssert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of the {@link DelegateJsonArray} that delegates most of its' functionality to the Jackson {@link ArrayNode}.
 *
 * @author Tihomir Mateev
 * @author Steffen Kreutz
 * @author Ko Su
 */
class DelegateJsonArray extends DelegateJsonValue implements JsonArray {

    DelegateJsonArray(ObjectMapper objectMapper) {
        super(new ArrayNode(JsonNodeFactory.instance), objectMapper);
    }

    DelegateJsonArray(JsonNode node, ObjectMapper objectMapper) {
        super(node, objectMapper);
    }

    @Override
    public JsonArray add(JsonValue element) {
        JsonNode newNode = null;

        if (element != null) {
            newNode = ((DelegateJsonValue) element).getNode();
        }

        ((ArrayNode) node).add(newNode);

        return this;
    }

    @Override
    public void addAll(JsonArray element) {
        LettuceAssert.notNull(element, "Element must not be null");

        ArrayNode otherArray = (ArrayNode) ((DelegateJsonValue) element).getNode();
        ((ArrayNode) node).addAll(otherArray);
    }

    @Override
    public List<JsonValue> asList() {
        List<JsonValue> result = new ArrayList<>();

        for (JsonNode jsonNode : node) {
            result.add(wrap(jsonNode, objectMapper));
        }

        return result;
    }

    @Override
    public JsonValue get(int index) {
        JsonNode jsonNode = node.get(index);

        return jsonNode == null ? null : wrap(jsonNode, objectMapper);
    }

    @Override
    public JsonValue getFirst() {
        return get(0);
    }

    @Override
    public Iterator<JsonValue> iterator() {
        return asList().iterator();
    }

    @Override
    public JsonValue remove(int index) {
        JsonNode jsonNode = ((ArrayNode) node).remove(index);

        return wrap(jsonNode, objectMapper);
    }

    @Override
    public JsonValue replace(int index, JsonValue newElement) {
        JsonNode replaceWith = ((DelegateJsonValue) newElement).getNode();
        JsonNode replaced = ((ArrayNode) node).set(index, replaceWith);

        return wrap(replaced, objectMapper);
    }

    @Override
    public JsonArray swap(int index, JsonValue newElement) {
        JsonNode replaceWith = ((DelegateJsonValue) newElement).getNode();
        ((ArrayNode) node).set(index, replaceWith);

        return this;
    }

    @Override
    public int size() {
        return node.size();
    }

    @Override
    public JsonArray asJsonArray() {
        return this;
    }

}
