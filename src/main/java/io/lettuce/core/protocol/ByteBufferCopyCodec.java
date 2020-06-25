/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.protocol;

import java.nio.ByteBuffer;

import io.lettuce.core.codec.RedisCodec;

/**
 * {@link RedisCodec} that leaves response data as {@link ByteBuffer} by copying buffers.
 *
 * @author Mark Paluch
 */
enum ByteBufferCopyCodec implements RedisCodec<ByteBuffer, ByteBuffer> {

    INSTANCE;

    @Override
    public ByteBuffer decodeKey(ByteBuffer bytes) {
        return copy(bytes);
    }

    @Override
    public ByteBuffer decodeValue(ByteBuffer bytes) {
        return copy(bytes);
    }

    @Override
    public ByteBuffer encodeKey(ByteBuffer key) {
        return copy(key);
    }

    @Override
    public ByteBuffer encodeValue(ByteBuffer value) {
        return copy(value);
    }

    private static ByteBuffer copy(ByteBuffer source) {
        ByteBuffer copy = ByteBuffer.allocate(source.remaining());
        copy.put(source);
        copy.flip();
        return copy;
    }

}
