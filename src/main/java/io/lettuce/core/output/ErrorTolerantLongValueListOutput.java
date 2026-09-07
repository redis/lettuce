/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import io.lettuce.core.Value;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

/**
 * {@link List} of {@link Value} wrapped 64-bit integer output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 7.7
 */
public class ErrorTolerantLongValueListOutput<K, V> extends CommandOutput<K, V, List<Value<Long>>>
        implements StreamingOutput<Value<Long>> {

    private boolean initialized;

    private Subscriber<Value<Long>> subscriber;

    public ErrorTolerantLongValueListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {
        subscriber.onNext(output, Value.just(integer));
    }

    @Override
    public void setError(ByteBuffer error) {

        if (initialized) {
            subscriber.onNext(output, Value.empty());
            return;
        }
        super.setError(error);
    }

    @Override
    public void set(ByteBuffer bytes) {

        if (initialized) {
            subscriber.onNext(output, Value.empty());
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
    public void setSubscriber(Subscriber<Value<Long>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<Value<Long>> getSubscriber() {
        return subscriber;
    }

}
