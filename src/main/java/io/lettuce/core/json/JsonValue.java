/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import java.nio.ByteBuffer;

/**
 * Representation of a JSON text as per the <a href="https://datatracker.ietf.org/doc/html/rfc8259#section-3"> </a>RFC 8259 -
 * The JavaScript Object Notation (JSON) Data Interchange Format, Section 3. Values</a>
 * <p>
 * Implementations of this interface need to make sure parsing of the JSON is not done inside the event loop thread, used to
 * process the data coming from the Redis server, otherwise larger JSON documents might cause performance degradation that spans
 * across all threads using the driver.
 *
 * @see JsonObject
 * @see JsonArray
 * @see io.lettuce.core.codec.RedisCodec
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data
 *      Interchange Format</a>
 * @author Tihomir Mateev
 * @since 6.5
 */
public interface JsonValue {

    /**
     * Execute any {@link io.lettuce.core.codec.RedisCodec} decoding and fetch the result.
     * 
     * @return the value representation of this {@link JsonValue} based on the codec used
     */
    String toValue();

    /**
     * @return the raw JSON text as a {@link ByteBuffer}
     */
    ByteBuffer asByteBuffer();

    /**
     * @return {@code true} if this {@link JsonValue} represents a JSON array
     */
    boolean isJsonArray();

    /**
     * @return the {@link JsonArray} representation of this {@link JsonValue}
     * @see #isJsonArray()
     */
    JsonArray asJsonArray();

    /**
     * @return {@code true} if this {@link JsonValue} represents a JSON object
     */
    boolean isJsonObject();

    /**
     * @return the {@link JsonObject} representation of this {@link JsonValue}
     * @see #isJsonObject()
     */
    JsonObject asJsonObject();

    /**
     * @return {@code true} if this {@link JsonValue} represents a JSON string
     */
    boolean isString();

    /**
     * @return the {@link String} representation of this {@link JsonValue}
     * @see #isString()
     */
    String asString();

    /**
     * @return {@code true} if this {@link JsonValue} represents a JSON number
     */
    boolean isNumber();

    /**
     * @return the {@link Number} representation of this {@link JsonValue}
     * @see #isNumber()
     */
    Number asNumber();

    /**
     * @return {@code true} if this {@link JsonValue} represents the value of null
     */
    boolean isNull();

}
