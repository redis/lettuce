package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.codec.RedisCodec;

/**
 * {@link io.lettuce.core.ValueScanCursor} for scan cursor output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class ValueScanOutput<K, V> extends ScanOutput<K, V, ValueScanCursor<V>> {

    public ValueScanOutput(RedisCodec<K, V> codec) {
        super(codec, new ValueScanCursor<V>());
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {
        output.getValues().add(bytes == null ? null : codec.decodeValue(bytes));
    }

}
