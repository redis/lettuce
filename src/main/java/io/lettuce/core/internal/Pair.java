/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.internal;

/**
 * Immutable pair of two values.
 *
 * @param <T1> type of the first value.
 * @param <T2> type of the second value.
 * @since 8.0
 */
public class Pair<T1, T2> {

    final T1 t1;

    final T2 t2;

    Pair(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    /**
     * Returns the first value.
     *
     * @return the first value, can be {@code null}.
     */
    public T1 getT1() {
        return t1;
    }

    /**
     * Returns the second value.
     *
     * @return the second value, can be {@code null}.
     */
    public T2 getT2() {
        return t2;
    }

    /**
     * Creates a new {@link Pair} of the given values.
     *
     * @param t1 the first value, can be {@code null}.
     * @param t2 the second value, can be {@code null}.
     * @param <T1> type of the first value.
     * @param <T2> type of the second value.
     * @return a new {@link Pair}.
     */
    public static <T1, T2> Pair<T1, T2> of(T1 t1, T2 t2) {
        return new Pair<>(t1, t2);
    }

}
