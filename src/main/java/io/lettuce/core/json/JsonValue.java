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

import java.nio.ByteBuffer;

/**
 * Representation of a JSON text as per the <a href="https://datatracker.ietf.org/doc/html/rfc8259#section-3"> </a>RFC 8259 -
 * The JavaScript Object Notation (JSON) Data Interchange Format, Section 3. Values</a>
 * <p>
 * Implementations of this interface need to make sure parsing of the JSON is not done inside the event loop thread, used to
 * process the data coming from the Redis server, otherwise larger JSON documents might cause performance degradation that spans
 * across all threads using the driver.
 *
 * @param <V> the type of data this {@link JsonValue} can output as value, depending on the
 *        {@link io.lettuce.core.codec.RedisCodec} used
 *
 * @see JsonObject
 * @see JsonArray
 * @see io.lettuce.core.codec.RedisCodec
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259">RFC 8259 - The JavaScript Object Notation (JSON) Data
 *      Interchange Format</a>
 */
public interface JsonValue<K, V> {

    /**
     * Execute any {@link io.lettuce.core.codec.RedisCodec} decoding and fetch the result.
     * 
     * @return the value representation of this {@link JsonValue} based on the codec used
     */
    V toValue();

    /**
     * @return the raw JSON text as a {@link ByteBuffer}
     */
    ByteBuffer asByteBuffer();

    boolean isJsonArray();

    JsonArray<K, V> asJsonArray();

    boolean isJsonObject();

    JsonObject<K, V> asJsonObject();

    boolean isString();

    String asString();

    boolean isNumber();

    Number asNumber();

}
