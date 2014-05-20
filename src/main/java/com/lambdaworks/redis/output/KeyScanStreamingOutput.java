package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.ScanCursor;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Streaming API for multiple Keys. You can implement this interface in order to receive a call to <code>onKey</code> on every
 * key. Key uniqueness is not guaranteed.
 * 
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 */
public class KeyScanStreamingOutput<K, V> extends ScanOutput<K, V, ScanCursor<Long>> {

    private KeyStreamingChannel<K> channel;

    public KeyScanStreamingOutput(RedisCodec<K, V> codec, KeyStreamingChannel<K> channel) {
        super(codec, new ScanCursor<Long>());
        this.channel = channel;
        output.setResult(Long.valueOf(0));
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {
        channel.onKey(bytes == null ? null : codec.decodeKey(bytes));
        output.setResult(output.getResult() + 1);
    }

}
