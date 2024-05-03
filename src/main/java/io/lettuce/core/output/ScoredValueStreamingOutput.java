package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceStrings;

/**
 * Streaming-Output of of values and their associated scores. Returns the count of all values (including null).
 *
 * @author Mark Paluch
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class ScoredValueStreamingOutput<K, V> extends CommandOutput<K, V, Long> {

    private V value;

    private boolean hasValue;

    private final ScoredValueStreamingChannel<V> channel;

    public ScoredValueStreamingOutput(RedisCodec<K, V> codec, ScoredValueStreamingChannel<V> channel) {
        super(codec, Long.valueOf(0));
        this.channel = channel;
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (!hasValue) {
            value = codec.decodeValue(bytes);
            hasValue = true;
            return;
        }

        double score = LettuceStrings.toDouble(decodeAscii(bytes));
        set(score);
    }

    @Override
    public void set(double number) {

        channel.onValue(ScoredValue.just(number, value));
        output = output.longValue() + 1;
        value = null;
        hasValue = false;
    }

}
