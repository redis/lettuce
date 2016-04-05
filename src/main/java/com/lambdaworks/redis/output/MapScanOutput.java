package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.MapScanCursor;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link com.lambdaworks.redis.MapScanCursor} for scan cursor output.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class MapScanOutput<K, V> extends ScanOutput<K, V, MapScanCursor<K, V>> {

    private K key;

    public MapScanOutput(RedisCodec<K, V> codec) {
        super(codec, new MapScanCursor<K, V>());
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {

        if (key == null) {
            key = codec.decodeKey(bytes);
            return;
        }

        V value = (bytes == null) ? null : codec.decodeValue(bytes);
        output.getMap().put(key, value);
        key = null;
    }

}
