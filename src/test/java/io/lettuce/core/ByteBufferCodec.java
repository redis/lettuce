/*
 * Copyright 2018-2020 the original author or authors.
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

import io.lettuce.core.codec.RedisCodec;

/**
 * @author Mark Paluch
 */
public class ByteBufferCodec implements RedisCodec<ByteBuffer, ByteBuffer> {

    @Override
    public ByteBuffer decodeKey(ByteBuffer bytes) {

        ByteBuffer decoupled = ByteBuffer.allocate(bytes.remaining());
        decoupled.put(bytes);
        return (ByteBuffer) decoupled.flip();
    }

    @Override
    public ByteBuffer decodeValue(ByteBuffer bytes) {

        ByteBuffer decoupled = ByteBuffer.allocate(bytes.remaining());
        decoupled.put(bytes);
        return (ByteBuffer) decoupled.flip();
    }

    @Override
    public ByteBuffer encodeKey(ByteBuffer key) {
        return key.asReadOnlyBuffer();
    }

    @Override
    public ByteBuffer encodeValue(ByteBuffer value) {
        return value.asReadOnlyBuffer();
    }

}
