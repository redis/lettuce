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
 * {@link List} of 64-bit integer output that tolerates per-element errors, such as {@code TS.MADD}'s reply: an array mixing
 * successful (integer timestamp) and failed (error) elements. An error encountered once the array has started is mapped to
 * {@code null} instead of failing the whole command, so callers still get the timestamps of the entries that succeeded.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Gyumin Hwang
 * @since 7.8
 */
public class ErrorTolerantLongListOutput<K, V> extends CommandOutput<K, V, List<Long>> implements StreamingOutput<Long> {

    private boolean initialized;

    private Subscriber<Long> subscriber;

    public ErrorTolerantLongListOutput(RedisCodec<K, V> codec) {
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
