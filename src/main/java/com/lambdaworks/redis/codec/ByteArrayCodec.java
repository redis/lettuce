/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.codec;

import java.nio.ByteBuffer;

/**
 * A {@link RedisCodec} that uses plain byte arrays.
 *
 * @author Mark Paluch
 * @since 3.3
 */
public class ByteArrayCodec implements RedisCodec<byte[], byte[]> {

    public static final ByteArrayCodec INSTANCE = new ByteArrayCodec();
    private static final byte[] EMPTY = new byte[0];

    @Override
    public byte[] decodeKey(ByteBuffer bytes) {
        return getBytes(bytes);
    }

    @Override
    public byte[] decodeValue(ByteBuffer bytes) {
        return getBytes(bytes);
    }

    @Override
    public ByteBuffer encodeKey(byte[] key) {

        if (key == null) {
            return ByteBuffer.wrap(EMPTY);
        }

        return ByteBuffer.wrap(key);
    }

    @Override
    public ByteBuffer encodeValue(byte[] value) {

        if (value == null) {
            return ByteBuffer.wrap(EMPTY);
        }

        return ByteBuffer.wrap(value);
    }

    private static byte[] getBytes(ByteBuffer buffer) {

        int remaining = buffer.remaining();

        if (remaining == 0) {
            return EMPTY;
        }

        byte[] b = new byte[remaining];
        buffer.get(b);
        return b;
    }

}
