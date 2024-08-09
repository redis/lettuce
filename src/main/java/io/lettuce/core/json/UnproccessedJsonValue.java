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

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;

class UnproccessedJsonValue<K, V> implements JsonValue<K, V> {

    private JsonValue<K, V> jsonValue;

    private final JsonParser<K, V> parser;

    private boolean deserialized = false;

    private final ByteBuffer unprocessedData;

    private final RedisCodec<K, V> codec;

    UnproccessedJsonValue(ByteBuffer bytes, RedisCodec<K, V> theCodec, JsonParser<K, V> theParser) {
        unprocessedData = bytes;
        parser = theParser;
        codec = theCodec;
    }

    @Override
    public V toValue() {
        if (deserialized) {
            return jsonValue.toValue();
        }

        // if no deserialization took place, so no modification took place
        // in this case we can decode the source data
        return codec.decodeValue(unprocessedData);
    }

    @Override
    public ByteBuffer asByteBuffer() {
        if (deserialized) {
            return jsonValue.asByteBuffer();
        }

        // if no deserialization took place, so no modification took place
        // in this case we can decode the source data
        return unprocessedData;
    }

    @Override
    public boolean isJsonArray() {
        lazilyDeserialize();
        return jsonValue.isJsonArray();
    }

    @Override
    public JsonArray<K, V> asJsonArray() {
        lazilyDeserialize();
        return jsonValue.asJsonArray();
    }

    @Override
    public boolean isJsonObject() {
        lazilyDeserialize();
        return jsonValue.isJsonObject();
    }

    @Override
    public JsonObject<K, V> asJsonObject() {
        lazilyDeserialize();
        return jsonValue.asJsonObject();
    }

    @Override
    public boolean isString() {
        lazilyDeserialize();
        return jsonValue.isString();
    }

    @Override
    public String asString() {
        lazilyDeserialize();
        return jsonValue.asString();
    }

    @Override
    public boolean isNumber() {
        lazilyDeserialize();
        return jsonValue.isNumber();
    }

    @Override
    public Number asNumber() {
        lazilyDeserialize();
        return jsonValue.asNumber();
    }

    private void lazilyDeserialize() {
        if (deserialized) {
            return;
        }

        jsonValue = parser.parse(unprocessedData);
        deserialized = true;
    }

}
