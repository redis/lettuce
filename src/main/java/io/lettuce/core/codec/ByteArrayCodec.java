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
package io.lettuce.core.codec;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;

/**
 * A {@link RedisCodec} that uses plain byte arrays without further transformations.
 *
 * @author Mark Paluch
 * @since 3.3
 */
public class ByteArrayCodec implements RedisCodec<byte[], byte[]>, ToByteBufEncoder<byte[], byte[]> {

    public static final ByteArrayCodec INSTANCE = new ByteArrayCodec();

    private static final byte[] EMPTY = new byte[0];

    @Override
    public void encodeKey(byte[] key, ByteBuf target) {

        if (key != null) {
            target.writeBytes(key);
        }
    }

    @Override
    public void encodeValue(byte[] value, ByteBuf target) {
        encodeKey(value, target);
    }

    @Override
    public int estimateSize(Object keyOrValue) {

        if (keyOrValue == null) {
            return 0;
        }

        return ((byte[]) keyOrValue).length;
    }

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
        return encodeKey(value);
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
