/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Range selectors for the Redis <a href="https://redis.io/docs/latest/commands/zrange/">ZRANGE</a> command. A selector couples
 * the range boundaries with the range kind and carries the options that are legal for that kind:
 * <ul>
 * <li>{@link #byIndex(long, long)} selects members by position and supports {@code REV}.</li>
 * <li>{@link #byScore(Range)} selects members by score ({@code BYSCORE}) and supports {@code REV} and {@code LIMIT}.</li>
 * <li>{@link #byLex(Range)} selects members by lexicographical order ({@code BYLEX}) and supports {@code REV} and
 * {@code LIMIT}. Lexicographical ranges cannot be combined with {@code WITHSCORES}.</li>
 * </ul>
 * Selectors copy the boundaries of the {@link Range} they are created from, so later modifications of that {@link Range} do not
 * affect the selector. Selectors are mutable objects and instances should be used only once to avoid shared mutable state.
 *
 * @author Yordan Tsintsov
 * @since 7.8
 */
public final class ZRange {

    private ZRange() {
    }

    /**
     * Create a selector for members by index.
     *
     * @param start the start index.
     * @param stop the stop index, inclusive.
     * @return the {@link ByIndex} selector for members between {@code start} and {@code stop}.
     * @since 7.8
     */
    public static ByIndex byIndex(long start, long stop) {
        return new ByIndex(start, stop);
    }

    /**
     * Create a selector for members by score.
     *
     * @param range the score range, must not be {@code null} and must not contain {@code NaN} boundaries.
     * @return the {@link ByScore} selector for members with a score within {@code range}.
     * @since 7.8
     */
    public static ByScore byScore(Range<? extends Number> range) {

        LettuceAssert.notNull(range, "Range must not be null");
        assertNotNaN(range.getLower());
        assertNotNaN(range.getUpper());

        return new ByScore(copy(range));
    }

    /**
     * Create a selector for members by score.
     *
     * @param min the lower score boundary, inclusive.
     * @param max the upper score boundary, inclusive.
     * @return the {@link ByScore} selector for members with a score between {@code min} and {@code max}.
     * @since 7.8
     */
    public static ByScore byScore(double min, double max) {
        return byScore(Range.create(min, max));
    }

    /**
     * Create a selector for members by lexicographical order.
     *
     * @param range the lexicographical range, must not be {@code null}.
     * @param <V> value type of the range boundaries.
     * @return the {@link ByLex} selector for members within {@code range}.
     * @since 7.8
     */
    public static <V> ByLex<V> byLex(Range<V> range) {

        LettuceAssert.notNull(range, "Range must not be null");

        return new ByLex<>(copy(range));
    }

    private static <T> Range<T> copy(Range<T> range) {
        return Range.from(range.getLower(), range.getUpper());
    }

    private static void assertNotNaN(Range.Boundary<? extends Number> boundary) {

        Number value = boundary.getValue();
        LettuceAssert.isTrue(value == null || !Double.isNaN(value.doubleValue()), "Score boundary must not be NaN");
    }

    /**
     * Selector for members by index.
     *
     * @since 7.8
     */
    public static final class ByIndex {

        private final long start;

        private final long stop;

        private boolean rev;

        private ByIndex(long start, long stop) {
            this.start = start;
            this.stop = stop;
        }

        /**
         * Traverse the sorted set in reverse order ({@code REV}).
         *
         * @return {@code this}.
         */
        public ByIndex rev() {
            this.rev = true;
            return this;
        }

        long getStart() {
            return start;
        }

        long getStop() {
            return stop;
        }

        boolean isRev() {
            return rev;
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();
            sb.append(getClass().getSimpleName()).append(" [").append(start).append(" to ").append(stop);

            if (rev) {
                sb.append(", REV");
            }

            return sb.append(']').toString();
        }

    }

    /**
     * Selector for members by score ({@code BYSCORE}).
     *
     * @since 7.8
     */
    public static final class ByScore {

        private final Range<? extends Number> range;

        private boolean rev;

        private Limit limit = Limit.unlimited();

        private ByScore(Range<? extends Number> range) {
            this.range = range;
        }

        /**
         * Traverse the sorted set in reverse order ({@code REV}).
         *
         * @return {@code this}.
         */
        public ByScore rev() {
            this.rev = true;
            return this;
        }

        /**
         * Limit the result to {@code count} members starting at {@code offset} ({@code LIMIT}).
         *
         * @param offset the offset.
         * @param count the count.
         * @return {@code this}.
         */
        public ByScore limit(long offset, long count) {
            return limit(Limit.create(offset, count));
        }

        /**
         * Limit the result ({@code LIMIT}).
         *
         * @param limit the limit, must not be {@code null}.
         * @return {@code this}.
         */
        public ByScore limit(Limit limit) {

            LettuceAssert.notNull(limit, "Limit must not be null");

            this.limit = limit;
            return this;
        }

        Range<? extends Number> getRange() {
            return range;
        }

        boolean isRev() {
            return rev;
        }

        Limit getLimit() {
            return limit;
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();
            sb.append(getClass().getSimpleName()).append(" [").append(range);

            if (rev) {
                sb.append(", REV");
            }

            if (limit.isLimited()) {
                sb.append(", ").append(limit);
            }

            return sb.append(']').toString();
        }

    }

    /**
     * Selector for members by lexicographical order ({@code BYLEX}).
     *
     * @param <V> value type of the range boundaries.
     * @since 7.8
     */
    public static final class ByLex<V> {

        private final Range<V> range;

        private boolean rev;

        private Limit limit = Limit.unlimited();

        private ByLex(Range<V> range) {
            this.range = range;
        }

        /**
         * Traverse the sorted set in reverse order ({@code REV}).
         * 
         * @return {@code this}.
         */
        public ByLex<V> rev() {
            this.rev = true;
            return this;
        }

        /**
         * Limit the result to {@code count} members starting at {@code offset} ({@code LIMIT}).
         *
         * @param offset the offset.
         * @param count the count.
         * @return {@code this}.
         */
        public ByLex<V> limit(long offset, long count) {
            return limit(Limit.create(offset, count));
        }

        /**
         * Limit the result ({@code LIMIT}).
         *
         * @param limit the limit, must not be {@code null}.
         * @return {@code this}.
         */
        public ByLex<V> limit(Limit limit) {

            LettuceAssert.notNull(limit, "Limit must not be null");

            this.limit = limit;
            return this;
        }

        Range<V> getRange() {
            return range;
        }

        boolean isRev() {
            return rev;
        }

        Limit getLimit() {
            return limit;
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();
            sb.append(getClass().getSimpleName()).append(" [").append(range);

            if (rev) {
                sb.append(", REV");
            }

            if (limit.isLimited()) {
                sb.append(", ").append(limit);
            }

            return sb.append(']').toString();
        }

    }

}
