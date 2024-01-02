/*
 * Copyright 2011-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core;

import java.nio.ByteBuffer;

/**
 * A Lua script returns one of the following types:
 *
 * <ul>
 * <li>{@link #BOOLEAN} boolean</li>
 * <li>{@link #INTEGER} 64-bit integer</li>
 * <li>{@link #STATUS} status string</li>
 * <li>{@link #VALUE} value</li>
 * <li>{@link #MULTI} of these types</li>
 * <li>{@link #OBJECT} result object defined by the RESP3 response</li>
 * </ul>
 *
 * <strong>Redis to Lua</strong> conversion table.
 * <ul>
 * <li>Redis integer reply -&gt; Lua number</li>
 * <li>Redis bulk reply -&gt; Lua string</li>
 * <li>Redis multi bulk reply -&gt; Lua table (may have other Redis data types nested)</li>
 * <li>Redis status reply -&gt; Lua table with a single {@code ok} field containing the status</li>
 * <li>Redis error reply -&gt; Lua table with a single {@code err} field containing the error</li>
 * <li>Redis Nil bulk reply and Nil multi bulk reply -&gt; Lua false boolean type</li>
 * </ul>
 *
 * <strong>Lua to Redis</strong> conversion table.
 * <ul>
 * <li>Lua number -&gt; Redis integer reply (the number is converted into an integer)</li>
 * <li>Lua string -&gt; Redis bulk reply</li>
 * <li>Lua table (array) -&gt; Redis multi bulk reply (truncated to the first {@code null} inside the Lua array if any)</li>
 * <li>Lua table with a single {@code ok} field -&gt; Redis status reply</li>
 * <li>Lua table with a single {@code err} field -&gt; Redis error reply</li>
 * <li>Lua boolean false -&gt; Redis Nil bulk reply.</li>
 * </ul>
 *
 * @author Will Glozer
 */
public enum ScriptOutputType {

    /**
     * Boolean output (expects a number {@code 0} or {@code 1} to be converted to a boolean value).
     */
    BOOLEAN,

    /**
     * {@link Long integer} output.
     */
    INTEGER,

    /**
     * List of flat arrays.
     */
    MULTI,

    /**
     * Simple status value such as {@code OK}. The Redis response is parsed as ASCII.
     */
    STATUS,

    /**
     * Value return type decoded through {@link io.lettuce.core.codec.RedisCodec#decodeValue(ByteBuffer)}.
     */
    VALUE,

    /**
     * RESP3-defined object output supporting all Redis response structures.
     */
    OBJECT
}
