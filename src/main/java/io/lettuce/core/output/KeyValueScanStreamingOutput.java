package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.codec.RedisCodec;

/**
 * Streaming-Output of Key Value Pairs. Returns the count of all Key-Value pairs (including null).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Mark Paluch
 */
public class KeyValueScanStreamingOutput<K, V> extends ScanOutput<K, V, StreamScanCursor> {

    private K key;

    private boolean hasKey;

    private KeyValueStreamingChannel<K, V> channel;

    public KeyValueScanStreamingOutput(RedisCodec<K, V> codec, KeyValueStreamingChannel<K, V> channel) {
        super(codec, new StreamScanCursor());
        this.channel = channel;
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {

        if (!hasKey) {
            key = codec.decodeKey(bytes);
            hasKey = true;
            return;
        }

        V value = (bytes == null) ? null : codec.decodeValue(bytes);

        channel.onKeyValue(key, value);
        output.setCount(output.getCount() + 1);
        key = null;
        hasKey = false;
    }

}
