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
 * boundaries with the range mode (by index, or by score using {@code BYSCORE}). Instances are created through the static
 * factory methods {@link #byIndex(long, long)} and {@link #byScore(Range)}. Use the {@code zrangeWithLex} methods with a plain
 * {@link Range} to select members by lexicographical order ({@code BYLEX}).
 * <p>
 * Ranges are always specified from lower to upper boundary, also for reverse traversal with {@link ZRangeArgs#rev()}.
 *
 * @author Yordan Tsintsov
 * @since 7.8
 */
public final class ZRange {

    private final RangeType rangeType;

    private final long start;

    private final long stop;

    private final Range<? extends Number> range;

    private ZRange(RangeType rangeType, long start, long stop, Range<? extends Number> range) {
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
     * @return the {@link ZRange} selecting members between {@code start} and {@code stop}.
     * @since 7.8
     */
    public static ZRange byIndex(long start, long stop) {
        return new ZRange(RangeType.INDEX, start, stop, null);
    }

    /**
     * Create a {@link ZRange} selecting members by score, corresponding to {@code BYSCORE}. Boundaries can be
     * {@link Range.Boundary#excluding(Object) excluding} or {@link Range.Boundary#unbounded() unbounded}.
     *
     * @param range the score range, must not be {@code null}.
     * @return the {@link ZRange} selecting members with a score within {@code range}.
     * @throws IllegalArgumentException if {@code range} is {@code null}.
     * @since 7.8
     */
    public static ZRange byScore(Range<? extends Number> range) {
        LettuceAssert.notNull(range, "Range must not be null");
        return new ZRange(RangeType.SCORE, 0, 0, range);
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

    Range<? extends Number> getRange() {
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
        SCORE
    }

}
