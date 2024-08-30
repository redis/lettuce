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
 * @param <V> Value type.
 * @author Tihomir Mateev
 */
class DelegateJsonValue<V> implements JsonValue<V> {

    protected final RedisCodec<?, V> codec;

    protected JsonNode node;

    DelegateJsonValue(JsonNode node, RedisCodec<?, V> codec) {
        this.codec = codec;
        this.node = node;
    }

    @Override
    public V toValue() {
        byte[] result = node.toString().getBytes();
        return codec.decodeValue(ByteBuffer.wrap(result));
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
    public JsonArray<V> asJsonArray() {
        throw new UnsupportedOperationException("The JSON value is not an array");
    }

    @Override
    public boolean isJsonObject() {
        return false;
    }

    @Override
    public JsonObject<V> asJsonObject() {
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

    protected String getStringValue(V key) {
        if (key instanceof String) {
            return (String) key;
        }

        return new StringCodec().decodeKey(codec.encodeValue(key));
    }

}
