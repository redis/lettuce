package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceStrings;

/**
 * A single {@link ScoredValue}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.1
 */
public class ScoredValueOutput<K, V> extends CommandOutput<K, V, ScoredValue<V>> {

    private V value;

    private boolean hasValue;

    public ScoredValueOutput(RedisCodec<K, V> codec) {
        super(codec, ScoredValue.empty());
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes == null) {
            return;
        }

        if (!hasValue) {
            value = codec.decodeValue(bytes);
            hasValue = true;
            return;
        }

        double score = LettuceStrings.toDouble(decodeString(bytes));
        set(score);
    }

    @Override
    public void set(long integer) {
        value = (V) (Long) integer;
    }

    @Override
    public void set(double number) {
        output = ScoredValue.just(number, value);
        value = null;
        hasValue = false;
    }

}
