/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Lexicographical range selector for the Redis <a href="https://redis.io/docs/latest/commands/zrange/">ZRANGE</a> command,
 * corresponding to {@code BYLEX}. Instances are created through {@link #create(Range)}. Use {@link ZRange} to select members by
 * index or by score.
 * <p>
 * Ranges are always specified from lower to upper boundary, also for reverse traversal with {@link ZRangeArgs#rev()}.
 * Boundaries can be {@link Range.Boundary#excluding(Object) excluding} or {@link Range.Boundary#unbounded() unbounded}.
 *
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.8
 * @see ZRange
 */
public final class ZLexRange<V> {

    private final Range<V> range;

    private ZLexRange(Range<V> range) {
        this.range = range;
    }

    /**
     * Create a {@link ZLexRange} selecting members by lexicographical order within {@code range}.
     *
     * @param range the lexicographical range, must not be {@code null}.
     * @param <V> Value type.
     * @return the {@link ZLexRange} selecting members within {@code range}.
     * @throws IllegalArgumentException if {@code range} is {@code null}.
     * @since 7.8
     */
    public static <V> ZLexRange<V> create(Range<V> range) {
        LettuceAssert.notNull(range, "Range must not be null");
        return new ZLexRange<>(range);
    }

    Range<V> getRange() {
        return range;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + range + ']';
    }

}
