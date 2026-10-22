/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.timeseries.TsSample;

/**
 * {@link TsSample} output for a single sample reply {@code [timestamp, value]}. An empty array gives {@code null}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
public class TsSampleOutput<K, V> extends CommandOutput<K, V, TsSample> {

    private final TsSampleRowDecoder row = new TsSampleRowDecoder();

    /**
     * Create a new output.
     *
     * @param codec the codec, must not be {@code null}.
     */
    public TsSampleOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(long integer) {
        row.timestamp(integer);
    }

    @Override
    public void set(double number) {
        row.value(number);
    }

    @Override
    public void set(ByteBuffer bytes) {
        row.value(bytes);
    }

    @Override
    public void complete(int depth) {

        if (depth == 0) {
            output = row.complete();
        }
    }

}
