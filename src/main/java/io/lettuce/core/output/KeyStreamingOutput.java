package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.codec.RedisCodec;

/**
 * Streaming-Output of Keys. Returns the count of all keys (including null).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class KeyStreamingOutput<K, V> extends CommandOutput<K, V, Long> {

    private final KeyStreamingChannel<K> channel;

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
