/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import io.lettuce.core.codec.StringCodec;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A wrapper around any of the implementations of the {@link JsonValue} provided by the implementation of the {@link JsonParser}
 * that is currently being used. The purpose of this class is to provide a lazy initialization mechanism and avoid any
 * deserialization in the event loop that processes the data coming from the Redis server.
 * <p>
 * This class is thread-safe and can be used in a multi-threaded environment.
 *
 * @author Tihomir Mateev
 */
class UnproccessedJsonValue implements JsonValue {

    private final Lock lock = new ReentrantLock();

    private volatile JsonValue jsonValue;

    private final JsonParser parser;

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
    public String toString() {

        if (unprocessedData == null) {
            return null;
        }

        if (isDeserialized()) {
            return jsonValue.toString();
        }

        lock.lock();
        try {
            if (isDeserialized()) {
                return jsonValue.toString();
            }

            // if no deserialization took place, so no modification took place
            // in this case we can decode the source data as is
            return StringCodec.UTF8.decodeValue(unprocessedData);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ByteBuffer asByteBuffer() {
        if (isDeserialized()) {
            return jsonValue.asByteBuffer();
        }

        lock.lock();
        try {
            if (isDeserialized()) {
                return jsonValue.asByteBuffer();
            }

            // if no deserialization took place, so no modification took place
            // in this case we can decode the source data as is
            return unprocessedData;
        } finally {
            lock.unlock();
        }
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
    public boolean isBoolean() {
        lazilyDeserialize();
        return jsonValue.isBoolean();
    }

    @Override
    public Boolean asBoolean() {
        lazilyDeserialize();
        return jsonValue.asBoolean();
    }

    @Override
    public boolean isNull() {
        lazilyDeserialize();
        return jsonValue.isNull();
    }

    @Override
    public <T> T toObject(Class<T> targetType) {
        lazilyDeserialize();
        return jsonValue.toObject(targetType);
    }

    private void lazilyDeserialize() {
        if (!isDeserialized()) {
            lock.lock();
            try {
                if (!isDeserialized()) {
                    jsonValue = parser.createJsonValue(unprocessedData);
                    if (unprocessedData != null) {
                        unprocessedData.clear();
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * @return {@code true} if the data has been deserialized
     */
    boolean isDeserialized() {
        return jsonValue != null;
    }

}
