/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.timeseries.TsSample;

/**
 * {@link List} of {@link TsSample} output for an array of sample rows {@code [timestamp, value1, …, valueN]}. Each row is
 * emitted to the subscriber as soon as it is decoded.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
public class TsSampleListOutput<K, V> extends CommandOutput<K, V, List<TsSample>> implements StreamingOutput<TsSample> {

    private final TsSampleRowDecoder row = new TsSampleRowDecoder();

    private boolean initialized;

    private Subscriber<TsSample> subscriber;

    /**
     * Create a new output.
     *
     * @param codec the codec, must not be {@code null}.
     */
    public TsSampleListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
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
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void complete(int depth) {

        if (depth == 1) {
            TsSample sample = row.complete();
            if (sample != null) {
                subscriber.onNext(output, sample);
            }
        }
    }

    @Override
    public void setSubscriber(Subscriber<TsSample> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<TsSample> getSubscriber() {
        return subscriber;
    }

}
