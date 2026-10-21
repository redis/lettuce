/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.time.Duration;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.timeseries.TsAggregation;
import io.lettuce.core.timeseries.TsBucketTimestamp;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.range/">TS.RANGE</a> and
 * <a href="https://redis.io/commands/ts.revrange/">TS.REVRANGE</a> commands. Options that are not set are not sent.
 * {@code ALIGN}, {@code BUCKETTIMESTAMP} and {@code EMPTY} require an aggregation; building the arguments without one throws
 * {@link IllegalArgumentException}.
 * <p>
 * {@link TsRangeArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class TsRangeArgs extends BaseTsRangeArgs<TsRangeArgs> implements CompositeArgument {

    /**
     * Builder entry points for {@link TsRangeArgs}.
     *
     * @since 8.0
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Create new {@link TsRangeArgs} with {@code LATEST} set.
         *
         * @return new {@link TsRangeArgs}.
         * @see TsRangeArgs#latest()
         * @since 8.0
         */
        public static TsRangeArgs latest() {
            return new TsRangeArgs().latest();
        }

        /**
         * Create new {@link TsRangeArgs} with a timestamp filter ({@code FILTER_BY_TS}) set.
         *
         * @param timestamps the timestamps in milliseconds, must not be {@code null} or empty.
         * @return new {@link TsRangeArgs}.
         * @throws IllegalArgumentException if {@code timestamps} is {@code null} or empty.
         * @see TsRangeArgs#filterByTimestamps(long...)
         * @since 8.0
         */
        public static TsRangeArgs filterByTimestamps(long... timestamps) {
            return new TsRangeArgs().filterByTimestamps(timestamps);
        }

        /**
         * Create new {@link TsRangeArgs} with a value filter ({@code FILTER_BY_VALUE}) set.
         *
         * @param min the lowest value.
         * @param max the highest value.
         * @return new {@link TsRangeArgs}.
         * @see TsRangeArgs#filterByValue(double, double)
         * @since 8.0
         */
        public static TsRangeArgs filterByValue(double min, double max) {
            return new TsRangeArgs().filterByValue(min, max);
        }

        /**
         * Create new {@link TsRangeArgs} with a limit ({@code COUNT}) set.
         *
         * @param count the maximum number of samples or buckets.
         * @return new {@link TsRangeArgs}.
         * @see TsRangeArgs#count(long)
         * @since 8.0
         */
        public static TsRangeArgs count(long count) {
            return new TsRangeArgs().count(count);
        }

        /**
         * Create new {@link TsRangeArgs} with an alignment timestamp ({@code ALIGN}) set.
         *
         * @param timestamp the alignment timestamp in milliseconds.
         * @return new {@link TsRangeArgs}.
         * @see TsRangeArgs#align(long)
         * @since 8.0
         */
        public static TsRangeArgs align(long timestamp) {
            return new TsRangeArgs().align(timestamp);
        }

        /**
         * Create new {@link TsRangeArgs} with {@code ALIGN start} set.
         *
         * @return new {@link TsRangeArgs}.
         * @see TsRangeArgs#alignStart()
         * @since 8.0
         */
        public static TsRangeArgs alignStart() {
            return new TsRangeArgs().alignStart();
        }

        /**
         * Create new {@link TsRangeArgs} with {@code ALIGN end} set.
         *
         * @return new {@link TsRangeArgs}.
         * @see TsRangeArgs#alignEnd()
         * @since 8.0
         */
        public static TsRangeArgs alignEnd() {
            return new TsRangeArgs().alignEnd();
        }

        /**
         * Create new {@link TsRangeArgs} with the reported bucket timestamp ({@code BUCKETTIMESTAMP}) set.
         *
         * @param bucketTimestamp the reported bucket timestamp, must not be {@code null}.
         * @return new {@link TsRangeArgs}.
         * @throws IllegalArgumentException if {@code bucketTimestamp} is {@code null}.
         * @see TsRangeArgs#bucketTimestamp(TsBucketTimestamp)
         * @since 8.0
         */
        public static TsRangeArgs bucketTimestamp(TsBucketTimestamp bucketTimestamp) {
            return new TsRangeArgs().bucketTimestamp(bucketTimestamp);
        }

        /**
         * Create new {@link TsRangeArgs} with {@code EMPTY} set.
         *
         * @return new {@link TsRangeArgs}.
         * @see TsRangeArgs#empty()
         * @since 8.0
         */
        public static TsRangeArgs empty() {
            return new TsRangeArgs().empty();
        }

        /**
         * Create new {@link TsRangeArgs} with an aggregation ({@code AGGREGATION}) set.
         *
         * @param bucketDuration the bucket duration in milliseconds, must be positive.
         * @param aggregations the aggregations, must not be {@code null} or empty and must not contain {@code null} elements.
         *        Several aggregations require Redis 8.8 or later.
         * @return new {@link TsRangeArgs}.
         * @throws IllegalArgumentException if {@code bucketDuration} is not positive, or if {@code aggregations} is
         *         {@code null}, empty or contains {@code null} elements.
         * @see TsRangeArgs#aggregation(long, TsAggregation...)
         * @since 8.0
         */
        public static TsRangeArgs aggregation(long bucketDuration, TsAggregation... aggregations) {
            return new TsRangeArgs().aggregation(bucketDuration, aggregations);
        }

        /**
         * Create new {@link TsRangeArgs} with an aggregation ({@code AGGREGATION}) set.
         *
         * @param bucketDuration the bucket duration, must not be {@code null} and must be at least one millisecond.
         * @param aggregations the aggregations, must not be {@code null} or empty and must not contain {@code null} elements.
         *        Several aggregations require Redis 8.8 or later.
         * @return new {@link TsRangeArgs}.
         * @throws IllegalArgumentException if {@code bucketDuration} or {@code aggregations} is invalid.
         * @see TsRangeArgs#aggregation(Duration, TsAggregation...)
         * @since 8.0
         */
        public static TsRangeArgs aggregation(Duration bucketDuration, TsAggregation... aggregations) {
            return new TsRangeArgs().aggregation(bucketDuration, aggregations);
        }

    }

    /**
     * Aggregate the samples into buckets ({@code AGGREGATION}). Each reported sample has one value per aggregation, in the
     * given order. Several aggregations require Redis 8.8 or later.
     *
     * @param bucketDuration the bucket duration in milliseconds, must be positive.
     * @param aggregations the aggregations, must not be {@code null} or empty and must not contain {@code null} elements.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code bucketDuration} is not positive, or if {@code aggregations} is {@code null},
     *         empty or contains {@code null} elements.
     * @since 8.0
     */
    public TsRangeArgs aggregation(long bucketDuration, TsAggregation... aggregations) {
        setAggregation(bucketDuration, aggregations);
        return this;
    }

    /**
     * Aggregate the samples into buckets ({@code AGGREGATION}). Each reported sample has one value per aggregation, in the
     * given order. Several aggregations require Redis 8.8 or later. The duration is sent in whole milliseconds.
     *
     * @param bucketDuration the bucket duration, must not be {@code null} and must be at least one millisecond.
     * @param aggregations the aggregations, must not be {@code null} or empty and must not contain {@code null} elements.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code bucketDuration} is {@code null} or shorter than one millisecond, or if
     *         {@code aggregations} is {@code null}, empty or contains {@code null} elements.
     * @since 8.0
     */
    public TsRangeArgs aggregation(Duration bucketDuration, TsAggregation... aggregations) {
        return aggregation(toMillis(bucketDuration), aggregations);
    }

    /**
     * Emit the options in the documented order: {@code LATEST}, {@code FILTER_BY_TS}, {@code FILTER_BY_VALUE}, {@code COUNT},
     * {@code ALIGN}, {@code AGGREGATION}, {@code BUCKETTIMESTAMP}, {@code EMPTY}.
     *
     * @param args the command arguments to append to.
     * @throws IllegalArgumentException if {@code ALIGN}, {@code BUCKETTIMESTAMP} or {@code EMPTY} is set without an
     *         aggregation.
     */
    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        validate();

        buildFilters(args);
        buildCountAndAlign(args);
        buildAggregation(args);
        buildBucketOptions(args);
    }

}
