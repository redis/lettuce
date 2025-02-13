package io.lettuce.core.output;

import static java.lang.Double.*;

import java.nio.ByteBuffer;

import io.lettuce.core.KeyValue;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;

/**
 * Output for a single [B]ZMPOP result.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 6.3
 */
public class KeyValueOfScoredValueOutput<K, V> extends CommandOutput<K, V, KeyValue<K, ScoredValue<V>>> {

    private K key;

    private V value;

    public KeyValueOfScoredValueOutput(RedisCodec<K, V> codec, K defaultKey) {
        super(codec, KeyValue.empty(defaultKey));
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (bytes != null) {
            if (key == null) {
                key = codec.decodeKey(bytes);
                return;
            }

            if (value == null) {
                value = codec.decodeValue(bytes);
                return;
            }

            output = KeyValue.just(key, ScoredValue.just(parseDouble(decodeString(bytes)), value));
        }
    }

    @Override
    public void set(double number) {

        if (key != null && value != null) {
            output = KeyValue.just(key, ScoredValue.just(number, value));
        }
    }

}
