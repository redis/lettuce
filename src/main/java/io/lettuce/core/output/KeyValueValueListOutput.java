package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.KeyValue;
import io.lettuce.core.codec.RedisCodec;

/**
 * Key-value pair output holding a list of values.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.2
 */
public class KeyValueValueListOutput<K, V> extends CommandOutput<K, V, KeyValue<K, List<V>>> {

    private K key;

    private boolean hasKey;

    private List<V> values = Collections.emptyList();

    public KeyValueValueListOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes != null) {
            if (!hasKey) {
                key = codec.decodeKey(bytes);
                hasKey = true;
            } else {
                V value = codec.decodeValue(bytes);
                values.add(value);
            }
        }
    }

    @Override
    public void multi(int count) {
        values = OutputFactory.newList(count);
    }

    @Override
    public void complete(int depth) {
        if (depth == 0 && hasKey) {
            output = KeyValue.just(key, values);
        }
    }

}
