package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link com.lambdaworks.redis.KeyScanCursor} for scan cursor output.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class KeyScanOutput<K, V> extends ScanOutput<K, V, KeyScanCursor<K>> {

    public KeyScanOutput(RedisCodec<K, V> codec) {
        super(codec, new KeyScanCursor<K>());
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {
        output.getKeys().add(bytes == null ? null : codec.decodeKey(bytes));
    }

}
