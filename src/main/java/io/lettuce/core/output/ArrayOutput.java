package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;

/**
 * {@link java.util.List} of objects and lists to support dynamic nested structures (List with mixed content of values and
 * sublists).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class ArrayOutput<K, V> extends NestedMultiOutput<K, V> {

    public ArrayOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

}
