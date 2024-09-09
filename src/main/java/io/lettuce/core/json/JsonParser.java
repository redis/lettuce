/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import java.nio.ByteBuffer;

/**
 * The JsonParser is an abstraction that allows transforming a JSON document from a {@link ByteBuffer} to implementations of the
 * {@link JsonValue} interface and vice versa. Underneath there could be different implementations that use different JSON
 * parser libraries or custom implementations. Respectively the {@link JsonParser} is responsible for building new instances of
 * the {@link JsonArray} and {@link JsonObject} interfaces, as they are ultimately tightly coupled with the specific JSON parser
 * that is being used.
 * <p/>
 * A custom implementation of the {@link JsonParser} can be provided to the {@link io.lettuce.core.ClientOptions} in case the
 * default implementation does not fit the requirements.
 *
 * @since 6.5
 * @author Tihomir Mateev
 */
public interface JsonParser {

    /**
     * Loads the provided {@link ByteBuffer} in a new {@link JsonValue}. Does not start the actual processing of the
     * {@link ByteBuffer} until a method of the {@link JsonValue} is called.
     *
     * @param bytes the {@link ByteBuffer} to create the {@link JsonValue} from
     * @return the created {@link JsonValue}
     * @throws RedisJsonException if the provided {@link ByteBuffer} is not a valid JSON document
     */
    JsonValue loadJsonValue(ByteBuffer bytes);

    /**
     * Create a new {@link JsonValue} from the provided {@link ByteBuffer}.
     *
     * @param bytes the {@link ByteBuffer} to create the {@link JsonValue} from
     * @return the created {@link JsonValue}
     * @throws RedisJsonException if the provided {@link ByteBuffer} is not a valid JSON document
     */
    JsonValue createJsonValue(ByteBuffer bytes);

    /**
     * Create a new {@link JsonValue} from the provided value.
     *
     * @param value the value to create the {@link JsonValue} from
     * @return the created {@link JsonValue}
     * @throws RedisJsonException if the provided value is not a valid JSON document
     */
    JsonValue createJsonValue(String value);

    /**
     * Create a new empty {@link JsonObject}.
     *
     * @return the created {@link JsonObject}
     */
    JsonObject createJsonObject();

    /**
     * Create a new empty {@link JsonArray}.
     *
     * @return the created {@link JsonArray}
     */
    JsonArray createJsonArray();

}
