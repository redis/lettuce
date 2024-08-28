/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;

class UnproccessedJsonValue<V> implements JsonValue<V> {

    private JsonValue<V> jsonValue;

    private final JsonParser<V> parser;

    private boolean deserialized = false;

    private final ByteBuffer unprocessedData;

    private final RedisCodec<?, V> codec;

    UnproccessedJsonValue(ByteBuffer bytes, RedisCodec<?, V> theCodec, JsonParser<V> theParser) {
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
    public JsonArray<V> asJsonArray() {
        lazilyDeserialize();
        return jsonValue.asJsonArray();
    }

    @Override
    public boolean isJsonObject() {
        lazilyDeserialize();
        return jsonValue.isJsonObject();
    }

    @Override
    public JsonObject<V> asJsonObject() {
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

    @Override
    public boolean isNull() {
        lazilyDeserialize();
        return jsonValue.isNull();
    }

    private void lazilyDeserialize() {
        if (deserialized) {
            return;
        }

        jsonValue = parser.parse(unprocessedData);
        deserialized = true;
    }

}
