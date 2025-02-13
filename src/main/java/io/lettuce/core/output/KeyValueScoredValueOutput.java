package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.KeyValue;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceStrings;

/**
 * {@link KeyValue} encapsulating {@link ScoredValue}. See {@code BZPOPMIN}/{@code BZPOPMAX} commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.1
 */
public class KeyValueScoredValueOutput<K, V> extends CommandOutput<K, V, KeyValue<K, ScoredValue<V>>> {

    private K key;

    private boolean hasKey;

    private V value;

    private boolean hasValue;

    public KeyValueScoredValueOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes == null) {
            return;
        }

        if (!hasKey) {
            key = codec.decodeKey(bytes);
            hasKey = true;
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
    public void set(double number) {

        output = KeyValue.just(key, ScoredValue.just(number, value));
        key = null;
        hasKey = false;
        value = null;
        hasValue = false;
    }

}
