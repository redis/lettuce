/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.internal.LettuceAssert;

import java.nio.ByteBuffer;

/**
 * Implementation of the {@link JsonValue} that delegates most of its functionality to the Jackson {@link JsonNode}.
 *
 * @author Tihomir Mateev
 * @author Steffen Kreutz
 */
class DelegateJsonValue implements JsonValue {

    protected JsonNode node;

    protected final ObjectMapper objectMapper;

    DelegateJsonValue(JsonNode node, ObjectMapper objectMapper) {
        this.node = node;
        this.objectMapper = objectMapper;
    }

    @Override
    public String toString() {
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

    @Override
    public <T> T toObject(Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            throw new RedisJsonException("Unable to map the provided JsonValue to " + type.getName(), e);
        }
    }

    static JsonValue wrap(JsonNode root, ObjectMapper objectMapper) {
        LettuceAssert.notNull(root, "Root must not be null");

        if (root.isObject()) {
            return new DelegateJsonObject(root, objectMapper);
        } else if (root.isArray()) {
            return new DelegateJsonArray(root, objectMapper);
        }

        return new DelegateJsonValue(root, objectMapper);
    }

}
