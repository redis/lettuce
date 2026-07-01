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

import io.lettuce.core.Value;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link List} of {@link Value}-wrapped boolean output for reactive CF.INSERT / CF.INSERTNX commands.
 *
 * <p>
 * Maps the per-item integer response to a 3-state {@link Value}:
 * <ul>
 * <li>{@code 1} &rarr; {@code Value.just(Boolean.TRUE)} &ndash; item was added</li>
 * <li>{@code 0} &rarr; {@code Value.just(Boolean.FALSE)} &ndash; item already exists (INSERTNX only)</li>
 * <li>negative (e.g. {@code -1}) &rarr; {@code Value.empty()} &ndash; filter is full</li>
 * </ul>
 *
 * <p>
 * Unlike {@link ErrorTolerantBooleanValueListOutput} (used by BF commands), this output distinguishes "already exists"
 * ({@code Value.just(false)}) from "filter full" ({@code Value.empty()}), which is required by the CF.INSERT / CF.INSERTNX
 * semantics.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Gyumin Hwang
 * @since 7.7
 */
public class CuckooInsertBooleanValueListOutput<K, V> extends CommandOutput<K, V, List<Value<Boolean>>>
        implements StreamingOutput<Value<Boolean>> {

    private boolean initialized;

    private Subscriber<Value<Boolean>> subscriber;

    public CuckooInsertBooleanValueListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {
        Value<Boolean> value = integer == 1 ? Value.just(Boolean.TRUE)
                : (integer == 0 ? Value.just(Boolean.FALSE) : Value.empty());
        subscriber.onNext(output, value);
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

        if (initialized) {
            subscriber.onNext(output, (bytes == null ? Value.empty() : Value.just(Boolean.parseBoolean(decodeString(bytes)))));
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
    public void setSubscriber(Subscriber<Value<Boolean>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<Value<Boolean>> getSubscriber() {
        return subscriber;
    }

}
