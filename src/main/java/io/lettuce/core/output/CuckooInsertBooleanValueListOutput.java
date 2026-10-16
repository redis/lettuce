/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import io.lettuce.core.Value;
import io.lettuce.core.codec.RedisCodec;

/**
 * {@link java.util.List} of {@link Value}-wrapped boolean output for the reactive CF.INSERTNX command.
 *
 * <p>
 * Extends {@link ErrorTolerantBooleanValueListOutput} to add a 3-state mapping for the per-item integer response:
 * <ul>
 * <li>{@code 1} &rarr; {@code Value.just(Boolean.TRUE)} &ndash; item was added</li>
 * <li>{@code 0} &rarr; {@code Value.just(Boolean.FALSE)} &ndash; item already exists</li>
 * <li>negative (e.g. {@code -1}) &rarr; {@code Value.empty()} &ndash; filter is full</li>
 * </ul>
 *
 * <p>
 * The base class maps any non-{@code 1} integer to {@code Value.just(false)}, which is correct for BF and CF.INSERT.
 * CF.INSERTNX additionally returns {@code 0} for "already exists", so this subclass overrides {@link #set(long)} to distinguish
 * {@code 0} ({@code Value.just(false)}) from negative ({@code Value.empty()}).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Gyumin Hwang
 * @since 7.7
 */
public class CuckooInsertBooleanValueListOutput<K, V> extends ErrorTolerantBooleanValueListOutput<K, V> {

    public CuckooInsertBooleanValueListOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

    @Override
    public void set(long integer) {
        Value<Boolean> value = integer == 1 ? Value.just(Boolean.TRUE)
                : (integer == 0 ? Value.just(Boolean.FALSE) : Value.empty());
        getSubscriber().onNext(output, value);
    }

}
