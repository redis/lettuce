package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.MapScanCursor;
import io.lettuce.core.codec.RedisCodec;

/**
 * {@link io.lettuce.core.MapScanCursor} for scan cursor output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class MapScanOutput<K, V> extends ScanOutput<K, V, MapScanCursor<K, V>> {

    private K key;

    private boolean hasKey;

    public MapScanOutput(RedisCodec<K, V> codec) {
        super(codec, new MapScanCursor<K, V>());
    }

    @Override
    protected void setOutput(ByteBuffer bytes) {

        if (!hasKey) {
            key = codec.decodeKey(bytes);
            hasKey = true;
            return;
        }

        V value = (bytes == null) ? null : codec.decodeValue(bytes);
        output.getMap().put(key, value);
        key = null;
        hasKey = false;
    }

}
