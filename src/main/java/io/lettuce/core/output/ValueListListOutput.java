/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * {@link List} of value lists output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author hutiefang76
 * @since 7.7
 */
public class ValueListListOutput<K, V> extends CommandOutput<K, V, List<List<V>>> {

    private boolean initialized;

    private List<V> nested;

    public ValueListListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (nested != null) {
            nested.add(bytes == null ? null : codec.decodeValue(bytes));
        }
    }

    @Override
    public void multi(int count) {
        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
            return;
        }

        nested = OutputFactory.newList(count);
        output.add(nested);
    }

}
