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

class DefaultJsonParser<K, V> implements JsonParser<K, V> {

    private final RedisCodec<K, V> codec;

    DefaultJsonParser(RedisCodec<K, V> codec) {
        this.codec = codec;
    }

    @Override
    public JsonValue<K, V> createJsonValue(ByteBuffer bytes) {
        return new UnproccessedJsonValue<>(bytes, codec, this);
    }

    @Override
    public JsonValue<K, V> createJsonValue(V value) {
        return parse(value);
    }

    @Override
    public JsonObject<K, V> createEmptyJsonObject() {
        return new DelegateJsonObject<K, V>(codec);
    }

    @Override
    public JsonArray<K, V> createEmptyJsonArray() {
        return new DelegateJsonArray<K, V>(codec);
    }

    protected JsonValue<K, V> parse(V value) {
        if (value instanceof String) {
            return parse((String) value);
        } else if (value instanceof ByteBuffer) {
            return parse((ByteBuffer) value);
        }

        return parse(codec.encodeValue(value));
    }

    private JsonValue<K, V> parse(String value) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(value);
            return wrap(root);
        } catch (JsonProcessingException e) {
            throw new RedisJsonException(
                    "Failed to process the provided value as JSON: " + String.format("%.50s", value) + "...", e);
        }
    }

    @Override
    public JsonValue<K, V> parse(ByteBuffer byteBuffer) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            JsonNode root = mapper.readTree(bytes);

            return wrap(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonValue<K, V> wrap(JsonNode root) {
        if (root.isObject()) {
            return new DelegateJsonObject<>(root, codec);
        } else if (root.isArray()) {
            return new DelegateJsonArray<>(root, codec);
        }

        return new DelegateJsonValue<>(root, codec);
    }

}
