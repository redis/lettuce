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
 * {@link List} of boolean output for CF.INSERT / CF.INSERTNX commands.
 *
 * <p>
 * Maps the per-item integer response to a 3-state {@link Boolean}:
 * <ul>
 * <li>{@code 1} &rarr; {@link Boolean#TRUE} &ndash; item was added</li>
 * <li>{@code 0} &rarr; {@link Boolean#FALSE} &ndash; item already exists (INSERTNX only)</li>
 * <li>negative (e.g. {@code -1}) &rarr; {@code null} &ndash; filter is full</li>
 * </ul>
 *
 * <p>
 * Unlike {@link ErrorTolerantBooleanListOutput} (used by BF commands), this output distinguishes "already exists"
 * ({@code false}) from "filter full" ({@code null}), which is required by the CF.INSERT / CF.INSERTNX semantics.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class CuckooInsertBooleanListOutput<K, V> extends CommandOutput<K, V, List<Boolean>>
        implements StreamingOutput<Boolean> {

    private boolean initialized;

    private Subscriber<Boolean> subscriber;

    public CuckooInsertBooleanListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {
        Boolean value = integer == 1 ? Boolean.TRUE : (integer == 0 ? Boolean.FALSE : null);
        subscriber.onNext(output, value);
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

        if (initialized && bytes == null) {
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
