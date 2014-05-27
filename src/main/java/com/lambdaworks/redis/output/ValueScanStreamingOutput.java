package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.StreamScanCursor;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Streaming API for multiple Values. You can implement this interface in order to receive a call to <code>onValue</code> on
 * every key.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ValueScanStreamingOutput<K, V> extends ScanOutput<K, V, StreamScanCursor> {

    private ValueStreamingChannel<V> channel;

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
