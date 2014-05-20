package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link com.lambdaworks.redis.KeyScanCursor} for scan cursor output.
 * 
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 */
public class KeyScanOutput<K, V> extends ScanOutput<K, V, KeyScanCursor<K>> {

    public KeyScanOutput(RedisCodec<K, V> codec) {
        super(codec, new KeyScanCursor<K>());
        output.setResult(new ArrayList<K>());
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {
        output.getKeys().add(bytes == null ? null : codec.decodeKey(bytes));
    }

}
