package com.lambdaworks.redis.codec;

import java.nio.ByteBuffer;

/**
 * A {@link RedisCodec} that uses plain byte arrays.
 * 
 * @author Mark Paluch
 * @since 3.3
 */
public class ByteArrayCodec implements RedisCodec<byte[], byte[]> {

    public final static ByteArrayCodec INSTANCE = new ByteArrayCodec();
    private final static byte[] EMPTY = new byte[0];

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

        if(key == null){
            return ByteBuffer.wrap(EMPTY);
        }

        return ByteBuffer.wrap(key);
    }

    @Override
    public ByteBuffer encodeValue(byte[] value) {

        if(value == null){
            return ByteBuffer.wrap(EMPTY);
        }

        return ByteBuffer.wrap(value);
    }

    private static byte[] getBytes(ByteBuffer buffer) {
        byte[] b = new byte[buffer.remaining()];
        buffer.get(b);
        return b;
    }

}
