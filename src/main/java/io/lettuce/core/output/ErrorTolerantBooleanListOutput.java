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
 * {@link List} of boolean output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class ErrorTolerantBooleanListOutput<K, V> extends CommandOutput<K, V, List<Boolean>>
        implements StreamingOutput<Boolean> {

    private boolean initialized;

    private Subscriber<Boolean> subscriber;

    public ErrorTolerantBooleanListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {
        subscriber.onNext(output, integer == 1 ? Boolean.TRUE : Boolean.FALSE);
    }

    @Override
    public void set(boolean value) {
        subscriber.onNext(output, value);
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
    public void setSubscriber(Subscriber<Boolean> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<Boolean> getSubscriber() {
        return subscriber;
    }

}
