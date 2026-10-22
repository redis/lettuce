/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.time.Duration;
import java.util.List;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.timeseries.TsAggregation;
import io.lettuce.core.timeseries.TsBucketTimestamp;
import io.lettuce.core.timeseries.TsFilter;
import io.lettuce.core.timeseries.TsReducer;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.mrange/">TS.MRANGE</a> and
 * <a href="https://redis.io/commands/ts.mrevrange/">TS.MREVRANGE</a> commands. Options that are not set are not sent.
 * <p>
 * {@code ALIGN}, {@code BUCKETTIMESTAMP} and {@code EMPTY} require an aggregation. These combinations are rejected with
 * {@link IllegalArgumentException}, by the setter that completes them and again when the arguments are built:
 * {@link #withLabels()} with {@link #selectedLabels(String...)}; {@link #groupBy(String, TsReducer)} with more than one
 * aggregation; {@link #groupBy(String, TsReducer)} with {@link #excludeEmpty()}.
 * <p>
 * {@link TsMRangeArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class TsMRangeArgs extends BaseTsRangeArgs<TsMRangeArgs> {

    private boolean withLabels;

    private String[] selectedLabels;

    private String groupByLabel;

    private TsReducer reducer;

    private boolean excludeEmpty;

    /**
     * Builder entry points for {@link TsMRangeArgs}.
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
         * Create new {@link TsMRangeArgs} with {@code LATEST} set.
         *
         * @return new {@link TsMRangeArgs}.
         * @see TsMRangeArgs#latest()
         * @since 8.0
         */
        public static TsMRangeArgs latest() {
            return new TsMRangeArgs().latest();
        }

        /**
         * Create new {@link TsMRangeArgs} with a timestamp filter ({@code FILTER_BY_TS}) set.
         *
         * @param timestamps the timestamps in milliseconds, must not be {@code null} or empty.
         * @return new {@link TsMRangeArgs}.
         * @throws IllegalArgumentException if {@code timestamps} is {@code null} or empty.
         * @see TsMRangeArgs#filterByTimestamps(long...)
         * @since 8.0
         */
        public static TsMRangeArgs filterByTimestamps(long... timestamps) {
            return new TsMRangeArgs().filterByTimestamps(timestamps);
        }

        /**
         * Create new {@link TsMRangeArgs} with a value filter ({@code FILTER_BY_VALUE}) set.
         *
         * @param min the lowest value.
         * @param max the highest value.
         * @return new {@link TsMRangeArgs}.
         * @see TsMRangeArgs#filterByValue(double, double)
         * @since 8.0
         */
        public static TsMRangeArgs filterByValue(double min, double max) {
            return new TsMRangeArgs().filterByValue(min, max);
        }

        /**
         * Create new {@link TsMRangeArgs} with a limit ({@code COUNT}) set.
         *
         * @param count the maximum number of samples or buckets.
         * @return new {@link TsMRangeArgs}.
         * @see TsMRangeArgs#count(long)
         * @since 8.0
         */
        public static TsMRangeArgs count(long count) {
            return new TsMRangeArgs().count(count);
        }

        /**
         * Create new {@link TsMRangeArgs} with an alignment timestamp ({@code ALIGN}) set.
         *
         * @param timestamp the alignment timestamp in milliseconds.
         * @return new {@link TsMRangeArgs}.
         * @see TsMRangeArgs#align(long)
         * @since 8.0
         */
        public static TsMRangeArgs align(long timestamp) {
            return new TsMRangeArgs().align(timestamp);
        }

        /**
         * Create new {@link TsMRangeArgs} with {@code ALIGN start} set.
         *
         * @return new {@link TsMRangeArgs}.
         * @see TsMRangeArgs#alignStart()
         * @since 8.0
         */
        public static TsMRangeArgs alignStart() {
            return new TsMRangeArgs().alignStart();
        }

        /**
         * Create new {@link TsMRangeArgs} with {@code ALIGN end} set.
         *
         * @return new {@link TsMRangeArgs}.
         * @see TsMRangeArgs#alignEnd()
         * @since 8.0
         */
        public static TsMRangeArgs alignEnd() {
            return new TsMRangeArgs().alignEnd();
        }

        /**
         * Create new {@link TsMRangeArgs} with the reported bucket timestamp ({@code BUCKETTIMESTAMP}) set.
         *
         * @param bucketTimestamp the reported bucket timestamp, must not be {@code null}.
         * @return new {@link TsMRangeArgs}.
         * @throws IllegalArgumentException if {@code bucketTimestamp} is {@code null}.
         * @see TsMRangeArgs#bucketTimestamp(TsBucketTimestamp)
         * @since 8.0
         */
        public static TsMRangeArgs bucketTimestamp(TsBucketTimestamp bucketTimestamp) {
            return new TsMRangeArgs().bucketTimestamp(bucketTimestamp);
        }

        /**
         * Create new {@link TsMRangeArgs} with {@code EMPTY} set.
         *
         * @return new {@link TsMRangeArgs}.
         * @see TsMRangeArgs#empty()
         * @since 8.0
         */
        public static TsMRangeArgs empty() {
            return new TsMRangeArgs().empty();
        }

        /**
         * Create new {@link TsMRangeArgs} with an aggregation ({@code AGGREGATION}) set.
         *
         * @param bucketDuration the bucket duration in milliseconds, must be positive.
         * @param aggregations the aggregations, must not be {@code null} or empty and must not contain {@code null} elements.
         *        Several aggregations require Redis 8.8 or later.
         * @return new {@link TsMRangeArgs}.
         * @throws IllegalArgumentException if {@code bucketDuration} is not positive, or if {@code aggregations} is
         *         {@code null}, empty or contains {@code null} elements.
         * @see TsMRangeArgs#aggregation(long, TsAggregation...)
         * @since 8.0
         */
        public static TsMRangeArgs aggregation(long bucketDuration, TsAggregation... aggregations) {
            return new TsMRangeArgs().aggregation(bucketDuration, aggregations);
        }

        /**
         * Create new {@link TsMRangeArgs} with an aggregation ({@code AGGREGATION}) set.
         *
         * @param bucketDuration the bucket duration, must not be {@code null} and must be at least one millisecond.
         * @param aggregations the aggregations, must not be {@code null} or empty and must not contain {@code null} elements.
         *        Several aggregations require Redis 8.8 or later.
         * @return new {@link TsMRangeArgs}.
         * @throws IllegalArgumentException if {@code bucketDuration} is {@code null} or shorter than one millisecond, or if
         *         {@code aggregations} is {@code null}, empty or contains {@code null} elements.
         * @see TsMRangeArgs#aggregation(Duration, TsAggregation...)
         * @since 8.0
         */
        public static TsMRangeArgs aggregation(Duration bucketDuration, TsAggregation... aggregations) {
            return new TsMRangeArgs().aggregation(bucketDuration, aggregations);
        }

        /**
         * Create new {@link TsMRangeArgs} with {@code WITHLABELS} set.
         *
         * @return new {@link TsMRangeArgs}.
         * @see TsMRangeArgs#withLabels()
         * @since 8.0
         */
        public static TsMRangeArgs withLabels() {
            return new TsMRangeArgs().withLabels();
        }

        /**
         * Create new {@link TsMRangeArgs} with {@code SELECTED_LABELS} set.
         *
         * @param labels the label names, must not be {@code null} or empty and must not contain {@code null} elements.
         * @return new {@link TsMRangeArgs}.
         * @throws IllegalArgumentException if {@code labels} is {@code null}, empty or contains {@code null} elements.
         * @see TsMRangeArgs#selectedLabels(String...)
         * @since 8.0
         */
        public static TsMRangeArgs selectedLabels(String... labels) {
            return new TsMRangeArgs().selectedLabels(labels);
        }

        /**
         * Create new {@link TsMRangeArgs} with {@code GROUPBY label REDUCE reducer} set.
         *
         * @param label the label to group by, must not be {@code null} or empty.
         * @param reducer the reducer, must not be {@code null}.
         * @return new {@link TsMRangeArgs}.
         * @throws IllegalArgumentException if {@code label} is {@code null} or empty or {@code reducer} is {@code null}.
         * @see TsMRangeArgs#groupBy(String, TsReducer)
         * @since 8.0
         */
        public static TsMRangeArgs groupBy(String label, TsReducer reducer) {
            return new TsMRangeArgs().groupBy(label, reducer);
        }

        /**
         * Create new {@link TsMRangeArgs} with {@code EXCLUDEEMPTY} set.
         *
         * @return new {@link TsMRangeArgs}.
         * @see TsMRangeArgs#excludeEmpty()
         * @since 8.0
         */
        public static TsMRangeArgs excludeEmpty() {
            return new TsMRangeArgs().excludeEmpty();
        }

    }

    /**
     * Aggregate the samples of each series into buckets ({@code AGGREGATION}). Each reported sample has one value per
     * aggregation, in the given order. Several aggregations require Redis 8.8 or later and cannot be combined with
     * {@link #groupBy(String, TsReducer)}.
     *
     * @param bucketDuration the bucket duration in milliseconds, must be positive.
     * @param aggregations the aggregations, must not be {@code null} or empty and must not contain {@code null} elements.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code bucketDuration} is not positive, or if {@code aggregations} is {@code null},
     *         empty or contains {@code null} elements, or if there is more than one aggregation and
     *         {@link #groupBy(String, TsReducer)} was set.
     * @since 8.0
     */
    public TsMRangeArgs aggregation(long bucketDuration, TsAggregation... aggregations) {

        LettuceAssert.isTrue(groupByLabel == null || aggregations == null || aggregations.length <= 1,
                "GROUPBY cannot be combined with more than one aggregation");

        setAggregation(bucketDuration, aggregations);
        return this;
    }

    /**
     * Aggregate the samples of each series into buckets ({@code AGGREGATION}). The duration is sent in whole milliseconds.
     * Several aggregations require Redis 8.8 or later and cannot be combined with {@link #groupBy(String, TsReducer)}.
     *
     * @param bucketDuration the bucket duration, must not be {@code null} and must be at least one millisecond.
     * @param aggregations the aggregations, must not be {@code null} or empty and must not contain {@code null} elements.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code bucketDuration} is {@code null} or shorter than one millisecond, if
     *         {@code aggregations} is {@code null}, empty or contains {@code null} elements, or if there is more than one
     *         aggregation and {@link #groupBy(String, TsReducer)} was set.
     * @since 8.0
     */
    public TsMRangeArgs aggregation(Duration bucketDuration, TsAggregation... aggregations) {
        return aggregation(toMillis(bucketDuration), aggregations);
    }

    /**
     * Return all labels of each series ({@code WITHLABELS}).
     *
     * @return {@code this}.
     * @throws IllegalArgumentException if {@link #selectedLabels(String...)} was set.
     * @since 8.0
     */
    public TsMRangeArgs withLabels() {

        LettuceAssert.isTrue(selectedLabels == null, "WITHLABELS cannot be combined with SELECTED_LABELS");

        this.withLabels = true;
        return this;
    }

    /**
     * Return only the given labels of each series ({@code SELECTED_LABELS}). A label that a series does not have is returned
     * with a {@code null} value.
     *
     * @param labels the label names, must not be {@code null} or empty and must not contain {@code null} elements.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code labels} is {@code null}, empty or contains {@code null} elements, or if
     *         {@link #withLabels()} was set.
     * @since 8.0
     */
    public TsMRangeArgs selectedLabels(String... labels) {

        LettuceAssert.notEmpty(labels, "Labels must not be empty");
        LettuceAssert.noNullElements(labels, "Labels must not contain null elements");
        LettuceAssert.isTrue(!withLabels, "SELECTED_LABELS cannot be combined with WITHLABELS");

        this.selectedLabels = labels.clone();
        return this;
    }

    /**
     * Group the series by the value of {@code label} and combine each group into one series with {@code reducer}
     * ({@code GROUPBY label REDUCE reducer}). Each grouped series is named {@code label=value}.
     *
     * @param label the label to group by, must not be {@code null} or empty.
     * @param reducer the reducer, must not be {@code null}. {@code countNaN} and {@code countAll} require Redis 8.6 or later.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code label} is {@code null} or empty, {@code reducer} is {@code null}, more than
     *         one aggregation was set, or {@link #excludeEmpty()} was set.
     * @since 8.0
     */
    public TsMRangeArgs groupBy(String label, TsReducer reducer) {

        LettuceAssert.notEmpty(label, "Label must not be empty");
        LettuceAssert.notNull(reducer, "Reducer must not be null");
        LettuceAssert.isTrue(aggregations.size() <= 1, "GROUPBY cannot be combined with more than one aggregation");
        LettuceAssert.isTrue(!excludeEmpty, "GROUPBY cannot be combined with EXCLUDEEMPTY");

        this.groupByLabel = label;
        this.reducer = reducer;
        return this;
    }

    /**
     * Omit series that have no samples in the range ({@code EXCLUDEEMPTY}), even when aggregation {@code EMPTY} is set. A
     * series whose only samples in the range are NaN is still reported. Requires Redis 8.10 or later; earlier servers accept it
     * and ignore it without an error, so series without samples in the range are still returned.
     *
     * @return {@code this}.
     * @throws IllegalArgumentException if {@link #groupBy(String, TsReducer)} was set.
     * @since 8.0
     */
    public TsMRangeArgs excludeEmpty() {

        LettuceAssert.isTrue(groupByLabel == null, "EXCLUDEEMPTY cannot be combined with GROUPBY");

        this.excludeEmpty = true;
        return this;
    }

    /**
     * Get the aggregations of the request.
     *
     * @return the aggregations in request order; empty without an aggregation.
     * @since 8.0
     */
    public List<TsAggregation> getAggregations() {
        return aggregations;
    }

    /**
     * Get the label the series are grouped by.
     *
     * @return the group label, or {@code null} without {@code GROUPBY}.
     * @since 8.0
     */
    public String getGroupByLabel() {
        return groupByLabel;
    }

    /**
     * Get the reducer of the groups.
     *
     * @return the reducer, or {@code null} without {@code GROUPBY}.
     * @since 8.0
     */
    public TsReducer getReducer() {
        return reducer;
    }

    /**
     * Emit the options and the filters in the documented order: {@code LATEST}, {@code FILTER_BY_TS}, {@code FILTER_BY_VALUE},
     * {@code WITHLABELS} or {@code SELECTED_LABELS}, {@code COUNT}, {@code ALIGN}, {@code AGGREGATION},
     * {@code BUCKETTIMESTAMP}, {@code EMPTY}, {@code EXCLUDEEMPTY}, {@code FILTER}, {@code GROUPBY ... REDUCE}.
     *
     * @param args the command arguments to append to, must not be {@code null}.
     * @param filters the filters, must not be {@code null} or empty and must not contain {@code null} elements.
     * @param <K> Key type.
     * @param <V> Value type.
     * @throws IllegalArgumentException if {@code filters} is {@code null}, empty or contains {@code null} elements, or if the
     *         options conflict.
     * @since 8.0
     */
    public <K, V> void build(CommandArgs<K, V> args, List<TsFilter> filters) {

        LettuceAssert.notNull(filters, "Filters must not be null");
        LettuceAssert.isTrue(!filters.isEmpty(), "Filters must not be empty");
        LettuceAssert.noNullElements(filters, "Filters must not contain null elements");

        validate();
        LettuceAssert.isTrue(!(withLabels && selectedLabels != null), "WITHLABELS cannot be combined with SELECTED_LABELS");
        LettuceAssert.isTrue(groupByLabel == null || aggregations.size() <= 1,
                "GROUPBY cannot be combined with more than one aggregation");
        LettuceAssert.isTrue(groupByLabel == null || !excludeEmpty, "GROUPBY cannot be combined with EXCLUDEEMPTY");

        buildFilters(args);

        if (withLabels) {
            args.add(CommandKeyword.WITHLABELS);
        }

        if (selectedLabels != null) {
            args.add(CommandKeyword.SELECTED_LABELS);
            for (String label : selectedLabels) {
                args.add(label);
            }
        }

        buildCountAndAlign(args);
        buildAggregation(args);
        buildBucketOptions(args);

        // EXCLUDEEMPTY must precede FILTER: the server reads every argument after FILTER as a filter until GROUPBY
        if (excludeEmpty) {
            args.add(CommandKeyword.EXCLUDEEMPTY);
        }

        args.add(CommandKeyword.FILTER);
        for (TsFilter filter : filters) {
            args.add(filter.toString());
        }

        if (groupByLabel != null) {
            args.add(CommandKeyword.GROUPBY).add(groupByLabel).add(CommandKeyword.REDUCE).add(reducer);
        }
    }

}
