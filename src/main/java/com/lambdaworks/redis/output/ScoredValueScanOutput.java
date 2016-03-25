package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.ScoredValueScanCursor;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link com.lambdaworks.redis.ScoredValueScanCursor} for scan cursor output.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class ScoredValueScanOutput<K, V> extends ScanOutput<K, V, ScoredValueScanCursor<V>> {

    private V value;

    public ScoredValueScanOutput(RedisCodec<K, V> codec) {
        super(codec, new ScoredValueScanCursor<V>());
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {

        if (value == null) {
            value = codec.decodeValue(bytes);
            return;
        }

        double score = Double.parseDouble(decodeAscii(bytes));
        output.getValues().add(new ScoredValue<V>(score, value));
        value = null;
    }

}
