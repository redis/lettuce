/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import java.util.Objects;

import io.lettuce.core.internal.LettuceAssert;

/**
 * A value paired with an increment for use with {@code TOPK.INCRBY}.
 *
 * @param <V> the value type
 * @author Redis
 * @since 7.7
 */
public class IncrementPair<V> {

    private final V value;

    private final long increment;

    /**
     * Creates a new {@link IncrementPair}.
     *
     * @param value the value, must not be {@code null}.
     * @param increment the increment.
     */
    public IncrementPair(V value, long increment) {
        LettuceAssert.notNull(value, "Value must not be null");
        this.value = value;
        this.increment = increment;
    }

    /**
     * Creates a new {@link IncrementPair}.
     *
     * @param value the value, must not be {@code null}.
     * @param increment the increment.
     * @param <V> the value type.
     * @return a new {@link IncrementPair}.
     */
    public static <V> IncrementPair<V> of(V value, long increment) {
        return new IncrementPair<>(value, increment);
    }

    /**
     * Returns the value.
     *
     * @return the value.
     */
    public V getValue() {
        return value;
    }

    /**
     * Returns the increment.
     *
     * @return the increment.
     */
    public long getIncrement() {
        return increment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IncrementPair<?> that = (IncrementPair<?>) o;
        return increment == that.increment && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, increment);
    }

    @Override
    public String toString() {
        return "IncrementPair{" + value + " -> " + increment + '}';
    }

}
