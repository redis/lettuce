/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * {@link Charset}-related utilities.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @deprecated since 5.2, use {@link StandardCharsets} instead. Will be removed with 6.0.
 */
@Deprecated
public class LettuceCharsets {

    /**
     * US-ASCII charset.
     */
    public static final Charset ASCII = StandardCharsets.US_ASCII;

    /**
     * UTF-8 charset.
     */
    public static final Charset UTF8 = StandardCharsets.UTF_8;

    /**
     * Utility constructor.
     */
    private LettuceCharsets() {

    }

    /**
     * Create a ByteBuffer from a string using ASCII encoding.
     *
     * @param s the string.
     * @return ByteBuffer.
     */
    public static ByteBuffer buffer(String s) {
        return ByteBuffer.wrap(s.getBytes(ASCII));
    }

}
