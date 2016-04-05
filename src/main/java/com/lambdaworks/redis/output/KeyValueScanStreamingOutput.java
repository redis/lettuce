package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.StreamScanCursor;
import com.lambdaworks.redis.codec.RedisCodec;

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
    private KeyValueStreamingChannel<K, V> channel;

    public KeyValueScanStreamingOutput(RedisCodec<K, V> codec, KeyValueStreamingChannel<K, V> channel) {
        super(codec, new StreamScanCursor());
        this.channel = channel;
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {

        if (key == null) {
            key = codec.decodeKey(bytes);
            return;
        }

        V value = (bytes == null) ? null : codec.decodeValue(bytes);

        channel.onKeyValue(key, value);
        output.setCount(output.getCount() + 1);
        key = null;
    }

}
