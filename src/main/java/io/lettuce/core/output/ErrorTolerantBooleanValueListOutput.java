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
 * {@link List} of {@link Value} wrapped booleans output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class ErrorTolerantBooleanValueListOutput<K, V> extends CommandOutput<K, V, List<Value<Boolean>>>
        implements StreamingOutput<Value<Boolean>> {

    private boolean initialized;

    private Subscriber<Value<Boolean>> subscriber;

    public ErrorTolerantBooleanValueListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {
        subscriber.onNext(output, Value.just(integer == 1 ? Boolean.TRUE : Boolean.FALSE));
    }

    @Override
    public void set(boolean value) {
        subscriber.onNext(output, Value.just(value));
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
        subscriber.onNext(output, (bytes == null ? Value.empty() : Value.just(Boolean.parseBoolean(decodeString(bytes)))));
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<Value<Boolean>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<Value<Boolean>> getSubscriber() {
        return subscriber;
    }

}
