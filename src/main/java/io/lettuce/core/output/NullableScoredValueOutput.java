/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceStrings;

/**
 * A single value and its associated score, read from an array that holds one {@code [member, score]} pair. A null reply (a
 * RESP2 null array or a RESP3 null) leaves the result {@code null}, not {@link ScoredValue#empty()}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
public class NullableScoredValueOutput<K, V> extends CommandOutput<K, V, ScoredValue<V>> {

    private boolean initialized;

    private V value;

    private boolean hasValue;

    public NullableScoredValueOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (!initialized) {
            // a null reply before any array: nothing to read, the result stays null
            initialized = true;
            return;
        }

        if (!hasValue) {
            value = codec.decodeValue(bytes);
            hasValue = true;
            return;
        }

        set(LettuceStrings.toDouble(decodeString(bytes)));
    }

    @Override
    public void set(double number) {

        if (output == null) {
            output = ScoredValue.just(number, value);
        }
        value = null;
        hasValue = false;
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            initialized = true;
        }
    }

}
