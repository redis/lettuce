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

/**
 * {@link List} of {@link Long} output that tolerates errors inside the array. An error element of the array becomes a
 * {@code null} element, and the remaining elements are read to the end. An error reply instead of the array fails the command.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
public class ErrorTolerantIntegerListOutput<K, V> extends CommandOutput<K, V, List<Long>> implements StreamingOutput<Long> {

    private boolean initialized;

    private Subscriber<Long> subscriber;

    /**
     * Create a new output.
     *
     * @param codec the codec, must not be {@code null}.
     */
    public ErrorTolerantIntegerListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {
        subscriber.onNext(output, integer);
    }

    @Override
    public void setError(ByteBuffer error) {

        if (initialized) {
            subscriber.onNext(output, null);
            return;
        }

        super.setError(error);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (initialized) {
            subscriber.onNext(output, null);
            return;
        }

        super.set(bytes);
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<Long> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<Long> getSubscriber() {
        return subscriber;
    }

}
