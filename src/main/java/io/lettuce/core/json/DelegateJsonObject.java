/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.lettuce.core.codec.RedisCodec;

/**
 * Implementation of the {@link DelegateJsonObject} that delegates most of it's dunctionality to the Jackson {@link ObjectNode}.
 *
 * @author Tihomir Mateev
 */
class DelegateJsonObject extends DelegateJsonValue implements JsonObject {

    DelegateJsonObject() {
        super(new ObjectNode(JsonNodeFactory.instance));
    }

    DelegateJsonObject(JsonNode node) {
        super(node);
    }

    @Override
    public JsonObject put(String key, JsonValue element) {
        JsonNode newNode = ((DelegateJsonValue) element).getNode();

        ((ObjectNode) node).replace(key, newNode);
        return this;
    }

    @Override
    public JsonValue get(String key) {
        JsonNode value = node.get(key);

        return new DelegateJsonValue(value);
    }

    @Override
    public JsonValue remove(String key) {
        JsonNode value = ((ObjectNode) node).remove(key);

        return new DelegateJsonValue(value);
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
    public JsonObject asJsonObject() {
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
