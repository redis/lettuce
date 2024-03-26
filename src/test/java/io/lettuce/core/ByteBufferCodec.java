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
