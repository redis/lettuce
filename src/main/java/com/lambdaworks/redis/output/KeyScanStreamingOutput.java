package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.StreamScanCursor;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Streaming API for multiple Keys. You can implement this interface in order to receive a call to {@code onKey} on every key.
 * Key uniqueness is not guaranteed.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class KeyScanStreamingOutput<K, V> extends ScanOutput<K, V, StreamScanCursor> {

    private final KeyStreamingChannel<K> channel;

    public KeyScanStreamingOutput(RedisCodec<K, V> codec, KeyStreamingChannel<K> channel) {
        super(codec, new StreamScanCursor());
        this.channel = channel;
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {
        channel.onKey(bytes == null ? null : codec.decodeKey(bytes));
        output.setCount(output.getCount() + 1);
    }

}
