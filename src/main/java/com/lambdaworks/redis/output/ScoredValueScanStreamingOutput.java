package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.StreamScanCursor;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Streaming-Output of of values and their associated scores. Returns the count of all values (including null).
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class ScoredValueScanStreamingOutput<K, V> extends ScanOutput<K, V, StreamScanCursor> {

    private V value;
    private final ScoredValueStreamingChannel<V> channel;

    public ScoredValueScanStreamingOutput(RedisCodec<K, V> codec, ScoredValueStreamingChannel<V> channel) {
        super(codec, new StreamScanCursor());
        this.channel = channel;
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {
        if (value == null) {
            value = codec.decodeValue(bytes);
            return;
        }

        double score = Double.parseDouble(decodeAscii(bytes));
        channel.onValue(new ScoredValue<V>(score, value));
        value = null;
        output.setCount(output.getCount() + 1);
    }

}
