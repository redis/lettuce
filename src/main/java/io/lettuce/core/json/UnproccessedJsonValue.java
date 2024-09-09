/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import io.lettuce.core.codec.StringCodec;

import java.nio.ByteBuffer;

/**
 * A wrapper around any of the implementations of the {@link JsonValue} provided by the implementation of the {@link JsonParser}
 * that is currently being used. The purpose of this class is to provide a lazy initialization mechanism and avoid any
 * deserialization in the event loop that processes the data coming from the Redis server.
 *
 * @since 6.5
 * @author Tihomir Mateev
 */
class UnproccessedJsonValue implements JsonValue {

    private JsonValue jsonValue;

    private final JsonParser parser;

    private boolean deserialized = false;

    private final ByteBuffer unprocessedData;

    /**
     * Create a new instance of the {@link UnproccessedJsonValue}.
     *
     * @param bytes the raw JSON data
     * @param theParser the {@link JsonParser} that works with the current instance
     */
    public UnproccessedJsonValue(ByteBuffer bytes, JsonParser theParser) {
        unprocessedData = bytes;
        parser = theParser;
    }

    @Override
    public String toValue() {
        if (deserialized) {
            return jsonValue.toValue();
        }

        // if no deserialization took place, so no modification took place
        // in this case we can decode the source data
        return StringCodec.UTF8.decodeKey(unprocessedData);
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
    public JsonArray asJsonArray() {
        lazilyDeserialize();
        return jsonValue.asJsonArray();
    }

    @Override
    public boolean isJsonObject() {
        lazilyDeserialize();
        return jsonValue.isJsonObject();
    }

    @Override
    public JsonObject asJsonObject() {
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

        jsonValue = parser.createJsonValue(unprocessedData);
        deserialized = true;
    }

}
