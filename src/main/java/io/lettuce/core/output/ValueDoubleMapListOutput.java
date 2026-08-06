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
import java.util.Map;

/**
 * {@link List} of value-to-double maps output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author hutiefang76
 * @since 7.7
 */
public class ValueDoubleMapListOutput<K, V> extends CommandOutput<K, V, List<Map<V, Double>>> {

    private boolean initialized;

    private ValueDoubleMapOutput<K, V> nested;

    public ValueDoubleMapListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (nested != null) {
            nested.set(bytes);
        }
    }

    @Override
    public void set(double number) {
        if (nested != null) {
            nested.set(number);
        }
    }

    @Override
    public void complete(int depth) {
        if (nested != null && !output.isEmpty()) {
            output.set(output.size() - 1, nested.get());
        }
    }

    @Override
    public void multi(int count) {
        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
            return;
        }

        nested = new ValueDoubleMapOutput<>(codec);
        nested.multi(count);
        output.add(nested.get());
    }

}
