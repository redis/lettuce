package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceStrings;

/**
 * {@link io.lettuce.core.ScoredValueScanCursor} for scan cursor output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class ScoredValueScanOutput<K, V> extends ScanOutput<K, V, ScoredValueScanCursor<V>> {

    private V value;

    private boolean hasValue;

    public ScoredValueScanOutput(RedisCodec<K, V> codec) {
        super(codec, new ScoredValueScanCursor<V>());
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {

        if (!hasValue) {
            value = codec.decodeValue(bytes);
            hasValue = true;
            return;
        }

        double score = LettuceStrings.toDouble(decodeString(bytes));
        set(score);
    }

    @Override
    public void set(double number) {
        if (hasValue) {
            output.getValues().add(ScoredValue.just(number, value));
        }
        value = null;
        hasValue = false;
    }

}
