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

class DefaultJsonValue<K, V> implements JsonValue<K, V> {

    protected final RedisCodec<K, V> codec;

    protected final ByteBuffer unprocessedData;

    protected boolean deserialized = false;

    DefaultJsonValue(ByteBuffer bytes, RedisCodec<K, V> codec) {
        unprocessedData = bytes;
        this.codec = codec;
    }

    @Override
    public V toValue() {
        return codec.decodeValue(unprocessedData);
    }

    @Override
    public ByteBuffer asByteBuffer() {
        if (unprocessedData == null) {
            // TODO serialize the value to the buffer
            throw new RuntimeException("Not implemented");
        }

        return unprocessedData;
    }

    @Override
    public boolean isJsonArray() {
        lazilyDeserialize();

        return false;
    }

    @Override
    public JsonArray<K, V> asJsonArray() {
        lazilyDeserialize();

        throw new UnsupportedOperationException("The JSON value is not an array");
    }

    @Override
    public boolean isJsonObject() {
        lazilyDeserialize();

        return false;
    }

    @Override
    public JsonObject<K, V> asJsonObject() {
        lazilyDeserialize();

        throw new UnsupportedOperationException("The JSON value is not an object");
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public String asString() {
        return "";
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public Number asNumber() {
        return null;
    }

    private void lazilyDeserialize() {
    }

}
