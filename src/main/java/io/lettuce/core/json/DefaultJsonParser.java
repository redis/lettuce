/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.lettuce.core.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

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
        return new DefaultJsonObject<K, V>(codec);
    }

    @Override
    public JsonArray<K, V> createEmptyJsonArray() {
        return new DefaultJsonArray<K, V>(codec);
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

            if (root.isObject()) {
                return new DefaultJsonObject<>(root, codec);
            } else if (root.isArray()) {
                return new DefaultJsonArray<>(root, codec);
            }
            return new DefaultJsonValue<>(root, codec);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonValue<K, V> parse(ByteBuffer byteBuffer) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            JsonNode root = mapper.readTree(bytes);

            if (root.isObject()) {
                return new DefaultJsonObject<>(root, codec);
            } else if (root.isArray()) {
                return new DefaultJsonArray<>(root, codec);
            }
            return new DefaultJsonValue<>(root, codec);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
