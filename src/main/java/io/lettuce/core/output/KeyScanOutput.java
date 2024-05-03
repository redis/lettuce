package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.codec.RedisCodec;

/**
 * {@link io.lettuce.core.KeyScanCursor} for scan cursor output.
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
