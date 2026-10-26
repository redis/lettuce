/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
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
 * @author Aleksandar Todorov
 * @since 8.0
 */
public class TsRangeArgs implements CompositeArgument {

    private boolean latest;

    private long[] filterByTimestamps;

    private Double filterByValueMin;

    private Double filterByValueMax;

    private Long count;

    private String align;

    private TsBucketTimestamp bucketTimestamp;

    private boolean empty;

    private Long bucketDuration;

    private List<TsAggregation> aggregations = Collections.emptyList();

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
         * @param count the maximum number of samples or buckets, must be positive.
         * @return new {@link TsRangeArgs}.
         * @throws IllegalArgumentException if {@code count} is not positive.
         * @see TsRangeArgs#count(long)
         * @since 8.0
         */
        public static TsRangeArgs count(long count) {
            return new TsRangeArgs().count(count);
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
         * @throws IllegalArgumentException if {@code bucketDuration} is {@code null}, shorter than one millisecond or longer
         *         than {@code Long.MAX_VALUE} milliseconds, or if {@code aggregations} is {@code null}, empty or contains
         *         {@code null} elements.
         * @see TsRangeArgs#aggregation(Duration, TsAggregation...)
         * @since 8.0
         */
        public static TsRangeArgs aggregation(Duration bucketDuration, TsAggregation... aggregations) {
            return new TsRangeArgs().aggregation(bucketDuration, aggregations);
        }

    }

    /**
     * Also report the latest, possibly partial, bucket of a compaction ({@code LATEST}). The server ignores it for a series
     * that is not a compaction.
     *
     * @return {@code this}.
     * @since 8.0
     */
    public TsRangeArgs latest() {
        this.latest = true;
        return this;
    }

    /**
     * Keep only samples whose timestamp is one of {@code timestamps} ({@code FILTER_BY_TS}). The filter applies before
     * aggregation.
     *
     * @param timestamps the timestamps in milliseconds, must not be {@code null} or empty.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code timestamps} is {@code null} or empty.
     * @since 8.0
     */
    public TsRangeArgs filterByTimestamps(long... timestamps) {

        LettuceAssert.notEmpty(timestamps, "Timestamps must not be empty");

        this.filterByTimestamps = timestamps.clone();
        return this;
    }

    /**
     * Keep only samples whose value is between {@code min} and {@code max}, inclusive ({@code FILTER_BY_VALUE}). The filter
     * applies before aggregation. The server rejects NaN bounds.
     *
     * @param min the lowest value.
     * @param max the highest value.
     * @return {@code this}.
     * @since 8.0
     */
    public TsRangeArgs filterByValue(double min, double max) {
        this.filterByValueMin = min;
        this.filterByValueMax = max;
        return this;
    }

    /**
     * Limit the number of reported samples, or of buckets with an aggregation ({@code COUNT}).
     *
     * @param count the maximum number of samples or buckets, must be positive.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code count} is not positive.
     * @since 8.0
     */
    public TsRangeArgs count(long count) {

        LettuceAssert.isTrue(count > 0, "Count must be positive");

        this.count = count;
        return this;
    }

    /**
     * Align the aggregation buckets to {@code timestamp} ({@code ALIGN}). Requires an aggregation.
     *
     * @param timestamp the alignment timestamp in milliseconds.
     * @return {@code this}.
     * @since 8.0
     */
    public TsRangeArgs align(long timestamp) {
        this.align = Long.toString(timestamp);
        return this;
    }

    /**
     * Align the aggregation buckets to the start of the range ({@code ALIGN start}). Requires an aggregation and a bounded
     * lower end of the range.
     *
     * @return {@code this}.
     * @since 8.0
     */
    public TsRangeArgs alignStart() {
        this.align = "start";
        return this;
    }

    /**
     * Align the aggregation buckets to the end of the range ({@code ALIGN end}). Requires an aggregation and a bounded upper
     * end of the range.
     *
     * @return {@code this}.
     * @since 8.0
     */
    public TsRangeArgs alignEnd() {
        this.align = "end";
        return this;
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

        long checkedBucketDuration = TsArgsSupport.bucketDuration(bucketDuration);
        List<TsAggregation> checkedAggregations = TsArgsSupport.aggregations(aggregations);

        this.bucketDuration = checkedBucketDuration;
        this.aggregations = checkedAggregations;
        return this;
    }

    /**
     * Aggregate the samples into buckets ({@code AGGREGATION}). Each reported sample has one value per aggregation, in the
     * given order. Several aggregations require Redis 8.8 or later. The duration is sent in whole milliseconds.
     *
     * @param bucketDuration the bucket duration, must not be {@code null} and must be at least one millisecond.
     * @param aggregations the aggregations, must not be {@code null} or empty and must not contain {@code null} elements.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code bucketDuration} is {@code null}, shorter than one millisecond or longer than
     *         {@code Long.MAX_VALUE} milliseconds, or if {@code aggregations} is {@code null}, empty or contains {@code null}
     *         elements.
     * @since 8.0
     */
    public TsRangeArgs aggregation(Duration bucketDuration, TsAggregation... aggregations) {
        return aggregation(TsArgsSupport.bucketMillis(bucketDuration), aggregations);
    }

    /**
     * Set which timestamp is reported for each bucket ({@code BUCKETTIMESTAMP}). Requires an aggregation.
     *
     * @param bucketTimestamp the reported bucket timestamp, must not be {@code null}.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code bucketTimestamp} is {@code null}.
     * @since 8.0
     */
    public TsRangeArgs bucketTimestamp(TsBucketTimestamp bucketTimestamp) {

        LettuceAssert.notNull(bucketTimestamp, "Bucket timestamp must not be null");

        this.bucketTimestamp = bucketTimestamp;
        return this;
    }

    /**
     * Also report empty buckets ({@code EMPTY}). Requires an aggregation.
     *
     * @return {@code this}.
     * @since 8.0
     */
    public TsRangeArgs empty() {
        this.empty = true;
        return this;
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

        TsArgsSupport.assertAggregationOptions(bucketDuration != null, align, bucketTimestamp, empty);

        if (latest) {
            args.add(CommandKeyword.LATEST);
        }

        if (filterByTimestamps != null) {
            args.add(CommandKeyword.FILTER_BY_TS);
            for (long timestamp : filterByTimestamps) {
                args.add(timestamp);
            }
        }

        if (filterByValueMin != null) {
            args.add(CommandKeyword.FILTER_BY_VALUE).add(filterByValueMin).add(filterByValueMax);
        }

        if (count != null) {
            args.add(CommandKeyword.COUNT).add(count);
        }

        if (align != null) {
            args.add(CommandKeyword.ALIGN).add(align);
        }

        if (bucketDuration != null) {
            args.add(CommandKeyword.AGGREGATION).add(TsArgsSupport.aggregationToken(aggregations)).add(bucketDuration);
        }

        if (bucketTimestamp != null) {
            args.add(CommandKeyword.BUCKETTIMESTAMP).add(bucketTimestamp);
        }

        if (empty) {
            args.add(CommandKeyword.EMPTY);
        }
    }

}
