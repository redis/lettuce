package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.codec.RedisCodec;

/**
 * Streaming API for multiple Values. You can implement this interface in order to receive a call to {@code onValue} on every
 * key.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class ValueScanStreamingOutput<K, V> extends ScanOutput<K, V, StreamScanCursor> {

    private final ValueStreamingChannel<V> channel;

    public ValueScanStreamingOutput(RedisCodec<K, V> codec, ValueStreamingChannel<V> channel) {
        super(codec, new StreamScanCursor());
        this.channel = channel;
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {
        channel.onValue(bytes == null ? null : codec.decodeValue(bytes));
        output.setCount(output.getCount() + 1);
    }

}
