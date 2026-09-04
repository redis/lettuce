/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Range selector for the Redis <a href="https://redis.io/docs/latest/commands/zrange/">ZRANGE</a> command, coupling the range
 * boundaries with the range mode (by index, by score using {@code BYSCORE}, or by lexicographical order using {@code BYLEX}).
 * Instances are created through the static factory methods {@link #byIndex(long, long)}, {@link #byScore(Range)} and
 * {@link #byLex(Range)}.
 * <p>
 * Ranges are always specified from lower to upper boundary, also for reverse traversal with {@link ZRangeArgs#rev()}.
 *
 * @param <T> Value type.
 * @author Yordan Tsintsov
 * @since 7.7
 */
public final class ZRange<T> {

    private final RangeType rangeType;

    private final long start;

    private final long stop;

    private final Range<?> range;

    private ZRange(RangeType rangeType, long start, long stop, Range<?> range) {
        this.rangeType = rangeType;
        this.start = start;
        this.stop = stop;
        this.range = range;
    }

    /**
     * Create a {@link ZRange} selecting members by index, with {@code start} and {@code stop} being zero-based indexes where
     * {@code 0} is the first element. Indexes can be negative to address elements starting at the end of the sorted set, with
     * {@code -1} being the last element.
     *
     * @param start the start index.
     * @param stop the stop index, inclusive.
     * @param <T> Value type.
     * @return the {@link ZRange} selecting members between {@code start} and {@code stop}.
     * @since 7.7
     */
    public static <T> ZRange<T> byIndex(long start, long stop) {
        return new ZRange<>(RangeType.INDEX, start, stop, null);
    }

    /**
     * Create a {@link ZRange} selecting members by score, corresponding to {@code BYSCORE}. Boundaries can be
     * {@link Range.Boundary#excluding(Object) excluding} or {@link Range.Boundary#unbounded() unbounded}.
     *
     * @param range the score range, must not be {@code null}.
     * @param <T> Value type.
     * @return the {@link ZRange} selecting members with a score within {@code range}.
     * @throws IllegalArgumentException if {@code range} is {@code null}.
     * @since 7.7
     */
    public static <T> ZRange<T> byScore(Range<? extends Number> range) {

        LettuceAssert.notNull(range, "Range must not be null");

        return new ZRange<>(RangeType.SCORE, 0, 0, range);
    }

    /**
     * Create a {@link ZRange} selecting members by lexicographical order, corresponding to {@code BYLEX}. Boundaries can be
     * {@link Range.Boundary#excluding(Object) excluding} or {@link Range.Boundary#unbounded() unbounded}.
     *
     * @param range the lexicographical range, must not be {@code null}.
     * @param <T> Value type.
     * @return the {@link ZRange} selecting members within {@code range}.
     * @throws IllegalArgumentException if {@code range} is {@code null}.
     * @since 7.7
     */
    public static <T> ZRange<T> byLex(Range<T> range) {

        LettuceAssert.notNull(range, "Range must not be null");

        return new ZRange<>(RangeType.LEX, 0, 0, range);
    }

    RangeType getRangeType() {
        return rangeType;
    }

    long getStart() {
        return start;
    }

    long getStop() {
        return stop;
    }

    Range<?> getRange() {
        return range;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(" [").append(rangeType).append(' ');

        if (rangeType == RangeType.INDEX) {
            sb.append(start).append(" to ").append(stop);
        } else {
            sb.append(range);
        }

        return sb.append(']').toString();
    }

    /**
     * The range mode of a {@link ZRange}.
     */
    enum RangeType {

        /**
         * Range by index.
         */
        INDEX,

        /**
         * Range by score, corresponds to {@code BYSCORE}.
         */
        SCORE,

        /**
         * Range by lexicographical order, corresponds to {@code BYLEX}.
         */
        LEX

    }

}
