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
