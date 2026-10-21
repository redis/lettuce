/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.timeseries.TsAggregation;
import io.lettuce.core.timeseries.TsBucketTimestamp;

/**
 * Base for the TimeSeries range-query arguments. Holds the options common to all range commands and emits them in the
 * documented order. {@code ALIGN}, {@code BUCKETTIMESTAMP} and {@code EMPTY} require an aggregation; the check runs when the
 * arguments are built.
 *
 * @param <T> the concrete subtype, for fluent method chaining.
 * @since 8.0
 */
abstract class BaseTsRangeArgs<T extends BaseTsRangeArgs<T>> {

    private boolean latest;

    private long[] filterByTimestamps;

    private Double filterByValueMin;

    private Double filterByValueMax;

    private Long count;

    private String align;

    private TsBucketTimestamp bucketTimestamp;

    private boolean empty;

    // single aggregation list shared by the RANGE and MRANGE arguments; set through the subclasses' aggregation(...)

    Long bucketDuration;

    List<TsAggregation> aggregations = Collections.emptyList();

    @SuppressWarnings("unchecked")
    final T self() {
        return (T) this;
    }

    /**
     * Also report the latest, possibly partial, bucket of a compaction ({@code LATEST}). The server ignores it for a series
     * that is not a compaction.
     *
     * @return {@code this}.
     * @since 8.0
     */
    public T latest() {
        this.latest = true;
        return self();
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
    public T filterByTimestamps(long... timestamps) {

        LettuceAssert.notEmpty(timestamps, "Timestamps must not be empty");

        this.filterByTimestamps = timestamps.clone();
        return self();
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
    public T filterByValue(double min, double max) {
        this.filterByValueMin = min;
        this.filterByValueMax = max;
        return self();
    }

    /**
     * Limit the number of reported samples, or of buckets with an aggregation ({@code COUNT}).
     *
     * @param count the maximum number of samples or buckets.
     * @return {@code this}.
     * @since 8.0
     */
    public T count(long count) {
        this.count = count;
        return self();
    }

    /**
     * Align the aggregation buckets to {@code timestamp} ({@code ALIGN}). Requires an aggregation.
     *
     * @param timestamp the alignment timestamp in milliseconds.
     * @return {@code this}.
     * @since 8.0
     */
    public T align(long timestamp) {
        this.align = Long.toString(timestamp);
        return self();
    }

    /**
     * Align the aggregation buckets to the start of the range ({@code ALIGN start}). Requires an aggregation and a bounded
     * lower end of the range.
     *
     * @return {@code this}.
     * @since 8.0
     */
    public T alignStart() {
        this.align = "start";
        return self();
    }

    /**
     * Align the aggregation buckets to the end of the range ({@code ALIGN end}). Requires an aggregation and a bounded upper
     * end of the range.
     *
     * @return {@code this}.
     * @since 8.0
     */
    public T alignEnd() {
        this.align = "end";
        return self();
    }

    /**
     * Set which timestamp is reported for each bucket ({@code BUCKETTIMESTAMP}). Requires an aggregation.
     *
     * @param bucketTimestamp the reported bucket timestamp, must not be {@code null}.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code bucketTimestamp} is {@code null}.
     * @since 8.0
     */
    public T bucketTimestamp(TsBucketTimestamp bucketTimestamp) {

        LettuceAssert.notNull(bucketTimestamp, "Bucket timestamp must not be null");

        this.bucketTimestamp = bucketTimestamp;
        return self();
    }

    /**
     * Also report empty buckets ({@code EMPTY}). Requires an aggregation.
     *
     * @return {@code this}.
     * @since 8.0
     */
    public T empty() {
        this.empty = true;
        return self();
    }

    void setAggregation(long bucketDuration, TsAggregation... aggregations) {

        assertBucketDuration(bucketDuration);
        LettuceAssert.notEmpty(aggregations, "Aggregations must not be empty");
        LettuceAssert.noNullElements(aggregations, "Aggregations must not contain null elements");

        this.bucketDuration = bucketDuration;
        this.aggregations = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(aggregations)));
    }

    /**
     * Convert a bucket duration to whole milliseconds. A duration shorter than one millisecond counts as zero and is rejected.
     */
    static long toMillis(Duration bucket) {

        LettuceAssert.notNull(bucket, "Bucket duration must not be null");

        long millis = bucket.toMillis();
        assertBucketDuration(millis);

        return millis;
    }

    static void assertBucketDuration(long bucketDuration) {
        LettuceAssert.isTrue(bucketDuration > 0, "Bucket duration must be at least one millisecond");
    }

    /**
     * @return {@code true} if the arguments carry an aggregation.
     */
    boolean hasAggregation() {
        return bucketDuration != null;
    }

    /**
     * Check the options that require an aggregation.
     *
     * @throws IllegalArgumentException if {@code ALIGN}, {@code BUCKETTIMESTAMP} or {@code EMPTY} is set without an
     *         aggregation.
     */
    void validate() {

        if (!hasAggregation()) {
            LettuceAssert.isTrue(align == null, "ALIGN requires an aggregation");
            LettuceAssert.isTrue(bucketTimestamp == null, "BUCKETTIMESTAMP requires an aggregation");
            LettuceAssert.isTrue(!empty, "EMPTY requires an aggregation");
        }
    }

    /**
     * Emit {@code LATEST}, {@code FILTER_BY_TS} and {@code FILTER_BY_VALUE}.
     */
    <K, V> void buildFilters(CommandArgs<K, V> args) {

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
    }

    /**
     * Emit {@code COUNT} and {@code ALIGN}.
     */
    <K, V> void buildCountAndAlign(CommandArgs<K, V> args) {

        if (count != null) {
            args.add(CommandKeyword.COUNT).add(count);
        }

        if (align != null) {
            args.add(CommandKeyword.ALIGN).add(align);
        }
    }

    /**
     * Emit {@code AGGREGATION aggregations bucketDuration} for the single aggregation list, if set.
     */
    <K, V> void buildAggregation(CommandArgs<K, V> args) {

        if (bucketDuration != null) {
            args.add(CommandKeyword.AGGREGATION).add(aggregationToken(aggregations)).add(bucketDuration);
        }
    }

    /**
     * Emit {@code BUCKETTIMESTAMP} and {@code EMPTY}.
     */
    <K, V> void buildBucketOptions(CommandArgs<K, V> args) {

        if (bucketTimestamp != null) {
            args.add(CommandKeyword.BUCKETTIMESTAMP).add(bucketTimestamp);
        }

        if (empty) {
            args.add(CommandKeyword.EMPTY);
        }
    }

    /**
     * Join aggregation tokens with commas and no spaces, for example {@code MIN,AVG,MAX}.
     */
    static String aggregationToken(List<TsAggregation> aggregations) {

        StringBuilder token = new StringBuilder();
        for (TsAggregation aggregation : aggregations) {
            if (token.length() > 0) {
                token.append(',');
            }
            token.append(new String(aggregation.getBytes(), StandardCharsets.UTF_8));
        }
        return token.toString();
    }

}
