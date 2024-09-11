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
import io.lettuce.core.codec.RedisCodec;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Default implementation of the {@link JsonParser} that should fit most use cases. Utilizes the Jackson library for maintaining
 * the JSON tree model and provides the ability to create new instances of the {@link JsonValue}, {@link JsonArray} and
 * {@link JsonObject}.
 *
 * @since 6.5
 * @author Tihomir Mateev
 */
public class DefaultJsonParser implements JsonParser {

    public static final DefaultJsonParser INSTANCE = new DefaultJsonParser();

    private DefaultJsonParser() {
    }

    @Override
    public JsonValue loadJsonValue(ByteBuffer bytes) {
        return new UnproccessedJsonValue(bytes, this);
    }

    @Override
    public JsonValue createJsonValue(ByteBuffer bytes) {
        return parse(bytes);
    }

    @Override
    public JsonValue createJsonValue(String value) {
        return parse(value);
    }

    @Override
    public JsonObject createJsonObject() {
        return new DelegateJsonObject();
    }

    @Override
    public JsonArray createJsonArray() {
        return new DelegateJsonArray();
    }

    @Override
    public JsonValue fromObject(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode root = objectMapper.valueToTree(object);
            return DelegateJsonValue.wrap(root);
        } catch (IllegalArgumentException e) {
            throw new RedisJsonException("Failed to process the provided object as JSON", e);
        }
    }

    private JsonValue parse(String value) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(value);
            return DelegateJsonValue.wrap(root);
        } catch (JsonProcessingException e) {
            throw new RedisJsonException(
                    "Failed to process the provided value as JSON: " + String.format("%.50s", value) + "...", e);
        }
    }

    private JsonValue parse(ByteBuffer byteBuffer) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            JsonNode root = mapper.readTree(bytes);
            return DelegateJsonValue.wrap(root);
        } catch (IOException e) {
            throw new RedisJsonException("Failed to process the provided value as JSON", e);
        }
    }

}
