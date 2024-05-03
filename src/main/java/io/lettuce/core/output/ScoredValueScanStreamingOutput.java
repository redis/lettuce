package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceStrings;

/**
 * Streaming-Output of of values and their associated scores. Returns the count of all values (including null).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class ScoredValueScanStreamingOutput<K, V> extends ScanOutput<K, V, StreamScanCursor> {

    private final ScoredValueStreamingChannel<V> channel;

    private V value;

    private boolean hasValue;

    public ScoredValueScanStreamingOutput(RedisCodec<K, V> codec, ScoredValueStreamingChannel<V> channel) {
        super(codec, new StreamScanCursor());
        this.channel = channel;
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {
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

        if (hasValue) {
            channel.onValue(ScoredValue.just(number, value));
        }

        output.setCount(output.getCount() + 1);
        value = null;
        hasValue = false;
    }

}
