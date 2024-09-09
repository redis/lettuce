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
 * Implementation of the {@link JsonValue} that delegates most of it's dunctionality to the Jackson {@link JsonNode}.
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
        return false;
    }

    @Override
    public JsonArray asJsonArray() {
        throw new UnsupportedOperationException("The JSON value is not an array");
    }

    @Override
    public boolean isJsonObject() {
        return false;
    }

    @Override
    public JsonObject asJsonObject() {
        throw new UnsupportedOperationException("The JSON value is not an object");
    }

    @Override
    public boolean isString() {
        return node.isTextual();
    }

    @Override
    public String asString() {
        return node.asText();
    }

    @Override
    public boolean isNumber() {
        return node.isNumber();
    }

    public boolean isNull() {
        return node.isNull();
    }

    @Override
    public Number asNumber() {
        if (node.isInt()) {
            return node.asInt();
        } else if (node.isLong()) {
            return node.asLong();
        }
        return node.asDouble();
    }

    protected JsonNode getNode() {
        return node;
    }

}
