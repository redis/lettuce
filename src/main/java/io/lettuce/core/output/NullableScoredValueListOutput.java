/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.List;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceStrings;

/**
 * {@link List} of values and their associated scores, read from an array of {@code [member, score]} pairs. Unlike
 * {@link ScoredValueListOutput}, a null reply (a RESP2 null array or a RESP3 null) leaves the result {@code null} instead of an
 * empty list.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
public class NullableScoredValueListOutput<K, V> extends CommandOutput<K, V, List<ScoredValue<V>>>
        implements StreamingOutput<ScoredValue<V>> {

    private boolean initialized;

    private Subscriber<ScoredValue<V>> subscriber;

    private V value;

    private boolean hasValue;

    public NullableScoredValueListOutput(RedisCodec<K, V> codec) {
        super(codec, null);
        setSubscriber(ListSubscriber.instance());
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

        double score = LettuceStrings.toDouble(decodeString(bytes));
        set(score);
    }

    @Override
    public void set(double number) {

        subscriber.onNext(output, ScoredValue.just(number, value));
        value = null;
        hasValue = false;
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            initialized = true;
            if (count >= 0) {
                output = OutputFactory.newList(count);
            }
        }
    }

    @Override
    public void setSubscriber(Subscriber<ScoredValue<V>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<ScoredValue<V>> getSubscriber() {
        return subscriber;
    }

}
