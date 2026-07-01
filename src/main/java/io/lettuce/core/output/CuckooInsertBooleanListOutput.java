/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;

/**
 * {@link java.util.List} of boolean output for the CF.INSERTNX command.
 *
 * <p>
 * Extends {@link ErrorTolerantBooleanListOutput} to add a 3-state mapping for the per-item integer response:
 * <ul>
 * <li>{@code 1} &rarr; {@link Boolean#TRUE} &ndash; item was added</li>
 * <li>{@code 0} &rarr; {@link Boolean#FALSE} &ndash; item already exists</li>
 * <li>negative (e.g. {@code -1}) &rarr; {@code null} &ndash; filter is full</li>
 * </ul>
 *
 * <p>
 * The base class maps any non-{@code 1} integer to {@code false}, which is correct for BF and CF.INSERT (where {@code -1}
 * simply means "not added"). CF.INSERTNX additionally returns {@code 0} for "already exists", so this subclass overrides
 * {@link #set(long)} to distinguish {@code 0} ({@code false}) from negative ({@code null}).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Gyumin Hwang
 * @since 7.7
 */
public class CuckooInsertBooleanListOutput<K, V> extends ErrorTolerantBooleanListOutput<K, V> {

    public CuckooInsertBooleanListOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

    @Override
    public void set(long integer) {
        Boolean value = integer == 1 ? Boolean.TRUE : (integer == 0 ? Boolean.FALSE : null);
        getSubscriber().onNext(output, value);
    }

}
