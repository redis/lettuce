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
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

import java.nio.ByteBuffer;

/**
 * Implementation of the {@link JsonValue} that delegates most of it's dunctionality to the Jackson {@link JsonNode}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 */
class DelegateJsonValue<K, V> implements JsonValue<K, V> {

    protected final RedisCodec<K, V> codec;

    protected JsonNode node;

    DelegateJsonValue(JsonNode node, RedisCodec<K, V> codec) {
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
    public JsonArray<K, V> asJsonArray() {
        throw new UnsupportedOperationException("The JSON value is not an array");
    }

    @Override
    public boolean isJsonObject() {
        return false;
    }

    @Override
    public JsonObject<K, V> asJsonObject() {
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

    protected String getStringValue(K key) {
        if (key instanceof String) {
            return (String) key;
        }

        return new StringCodec().decodeKey(codec.encodeKey(key));
    }

}
