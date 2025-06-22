package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;

/**
 * Streaming-Output of Glob Patterns. Returns the count of all Glob Patterns (including null).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Seonghwan Lee
 */
public class GlobPatternStreamingOutput<K, V> extends CommandOutput<K, V, Long> {

    private final KeyStreamingChannel<K> channel;

    public GlobPatternStreamingOutput(RedisCodec<K, V> codec, KeyStreamingChannel<K> channel) {
        super(codec, Long.valueOf(0));
        this.channel = channel;
    }

    @Override
    public void set(ByteBuffer bytes) {

        channel.onKey(bytes == null ? null : codec.decodeKey(bytes));
        output = output.longValue() + 1;
    }

}
