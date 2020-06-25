/*
 * Copyright 2017-2020 the original author or authors.
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
package io.lettuce.core.codec;

/**
 * High-performance base16 (AKA hex) codec.
 *
 * @author Will Glozer
 */
public class Base16 {

    private static final char[] upper = "0123456789ABCDEF".toCharArray();

    private static final char[] lower = "0123456789abcdef".toCharArray();

    private static final byte[] decode = new byte[128];

    static {
        for (int i = 0; i < 10; i++) {
            decode['0' + i] = (byte) i;
            decode['A' + i] = (byte) (10 + i);
            decode['a' + i] = (byte) (10 + i);
        }
    }

    /**
     * Utility constructor.
     */
    private Base16() {

    }

    /**
     * Encode bytes to base16 chars.
     *
     * @param src Bytes to encode.
     * @param upper Use upper or lowercase chars.
     * @return Encoded chars.
     */
    public static char[] encode(byte[] src, boolean upper) {
        char[] table = upper ? Base16.upper : Base16.lower;
        char[] dst = new char[src.length * 2];

        for (int si = 0, di = 0; si < src.length; si++) {
            byte b = src[si];
            dst[di++] = table[(b & 0xf0) >>> 4];
            dst[di++] = table[(b & 0x0f)];
        }

        return dst;
    }

}
