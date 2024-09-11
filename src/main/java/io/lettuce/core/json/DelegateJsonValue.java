/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import com.fasterxml.jackson.databind.JsonNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

import java.nio.ByteBuffer;

/**
 * Implementation of the {@link JsonValue} that delegates most of its functionality to the Jackson {@link JsonNode}.
 *
 * @author Tihomir Mateev
 */
class DelegateJsonValue implements JsonValue {

    protected JsonNode node;

    DelegateJsonValue(JsonNode node) {
        this.node = node;
    }

    @Override
    public String toValue() {
        return node.toString();
    }

    @Override
    public ByteBuffer asByteBuffer() {
        byte[] result = node.toString().getBytes();
        return ByteBuffer.wrap(result);
    }

    @Override
    public boolean isJsonArray() {
        return node.isArray();
    }

    @Override
    public JsonArray asJsonArray() {
        return null;
    }

    @Override
    public boolean isJsonObject() {
        return node.isObject();
    }

    @Override
    public JsonObject asJsonObject() {
        return null;
    }

    @Override
    public boolean isString() {
        return node.isTextual();
    }

    @Override
    public String asString() {
        return node.isTextual() ? node.asText() : null;
    }

    @Override
    public boolean isNumber() {
        return node.isNumber();
    }

    @Override
    public Boolean asBoolean() {

        return node.isBoolean() ? node.asBoolean() : null;
    }

    @Override
    public boolean isBoolean() {
        return node.isBoolean();
    }

    public boolean isNull() {
        return node.isNull();
    }

    @Override
    public Number asNumber() {
        if (node.isNull()) {
            return null;
        }
        return node.numberValue();
    }

    protected JsonNode getNode() {
        return node;
    }

}
