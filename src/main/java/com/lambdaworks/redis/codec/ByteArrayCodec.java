package com.lambdaworks.redis.codec;

import java.nio.ByteBuffer;

/**
 * A {@link RedisCodec} that uses plain byte arrays.
 * 
 * @author Mark Paluch
 * @since 3.3
 */
public class ByteArrayCodec extends RedisCodec<byte[], byte[]> {

    /**
     * Static held instance ready to use. The {@link ByteArrayCodec} is thread-safe.
     */
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
    public byte[] encodeKey(byte[] key) {

        if (key == null){
            return EMPTY;
        }

        return key;
    }

    @Override
    public byte[] encodeValue(byte[] value) {

        if (value == null){
            return EMPTY;
        }

        return value;
    }

    private static byte[] getBytes(ByteBuffer buffer) {
        byte[] b = new byte[buffer.remaining()];
        buffer.get(b);
        return b;
    }
}
