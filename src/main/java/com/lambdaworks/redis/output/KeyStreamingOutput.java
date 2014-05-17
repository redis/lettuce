package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

/**
 * Streaming-Output of Keys. Returns the count of all keys (including null).
 * 
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @param <V> Value type.
 */
public class KeyStreamingOutput<K, V> extends CommandOutput<K, V, Long> {
    private KeyStreamingChannel<K> channel;

    public KeyStreamingOutput(RedisCodec<K, V> codec, KeyStreamingChannel<K> channel) {
        super(codec, Long.valueOf(0));
        this.channel = channel;
    }

    @Override
    public void set(ByteBuffer bytes) {

        channel.onKey(bytes == null ? null : codec.decodeKey(bytes));
        output = output.longValue() + 1;
    }

}
