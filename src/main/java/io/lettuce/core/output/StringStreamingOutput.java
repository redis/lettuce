package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;

/**
 * Streaming-Output of Glob Patterns. Returns the count of all Glob Patterns (including null).
 *
 * @param <K> Param type.
 * @param <V> Value type.
 * @author Seonghwan Lee
 */
public class StringStreamingOutput<K, V> extends CommandOutput<K, V, Long> {

    private final KeyStreamingChannel<String> channel;

    public StringStreamingOutput(RedisCodec<K, V> codec, KeyStreamingChannel<String> channel) {
        super(codec, 0L);
        this.channel = channel;
    }

    @Override
    public void set(ByteBuffer bytes) {

        channel.onKey(bytes == null ? null : decodeString(bytes));
        output = output + 1;
    }

}
