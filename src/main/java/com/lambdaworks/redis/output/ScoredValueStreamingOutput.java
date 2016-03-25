package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Streaming-Output of of values and their associated scores. Returns the count of all values (including null).
 * 
 * @author Mark Paluch
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class ScoredValueStreamingOutput<K, V> extends CommandOutput<K, V, Long> {
    private V value;
    private final ScoredValueStreamingChannel<V> channel;

    public ScoredValueStreamingOutput(RedisCodec<K, V> codec, ScoredValueStreamingChannel<V> channel) {
        super(codec, Long.valueOf(0));
        this.channel = channel;
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (value == null) {
            value = codec.decodeValue(bytes);
            return;
        }

        double score = Double.parseDouble(decodeAscii(bytes));
        channel.onValue(new ScoredValue<V>(score, value));
        value = null;
        output = output.longValue() + 1;
    }

}
