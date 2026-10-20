/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

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
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.nrange/">TS.NRANGE</a> and
 * <a href="https://redis.io/commands/ts.nrevrange/">TS.NREVRANGE</a> commands. Options that are not set are not sent.
 * <p>
 * The server requires one aggregation argument per key. {@link #aggregation(long, List)} sets one list per key, in key order;
 * {@link #aggregationForEachKey(long, TsAggregation...)} sends the same aggregations once for every key. A list count that
 * differs from the number of keys throws {@link IllegalArgumentException} when the arguments are built. {@code ALIGN},
 * {@code BUCKETTIMESTAMP} and {@code EMPTY} require an aggregation.
 * <p>
 * {@link TsNRangeArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class TsNRangeArgs extends BaseTsRangeArgs<TsNRangeArgs> {

    private Long nBucketDuration;

    private List<List<TsAggregation>> aggregationsPerKey;

    private List<TsAggregation> aggregationsForEachKey;

    /**
     * Builder entry points for {@link TsNRangeArgs}.
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
         * Create new {@link TsNRangeArgs} with {@code LATEST} set.
         *
         * @return new {@link TsNRangeArgs}.
         * @see TsNRangeArgs#latest()
         * @since 8.0
         */
        public static TsNRangeArgs latest() {
            return new TsNRangeArgs().latest();
        }

        /**
         * Create new {@link TsNRangeArgs} with a timestamp filter ({@code FILTER_BY_TS}) set.
         *
         * @param timestamps the timestamps in milliseconds, must not be {@code null} or empty.
         * @return new {@link TsNRangeArgs}.
         * @throws IllegalArgumentException if {@code timestamps} is {@code null} or empty.
         * @see TsNRangeArgs#filterByTimestamps(long...)
         * @since 8.0
         */
        public static TsNRangeArgs filterByTimestamps(long... timestamps) {
            return new TsNRangeArgs().filterByTimestamps(timestamps);
        }

        /**
         * Create new {@link TsNRangeArgs} with a value filter ({@code FILTER_BY_VALUE}) set.
         *
         * @param min the lowest value.
         * @param max the highest value.
         * @return new {@link TsNRangeArgs}.
         * @see TsNRangeArgs#filterByValue(double, double)
         * @since 8.0
         */
        public static TsNRangeArgs filterByValue(double min, double max) {
            return new TsNRangeArgs().filterByValue(min, max);
        }

        /**
         * Create new {@link TsNRangeArgs} with a limit ({@code COUNT}) set.
         *
         * @param count the maximum number of samples or buckets.
         * @return new {@link TsNRangeArgs}.
         * @see TsNRangeArgs#count(long)
         * @since 8.0
         */
        public static TsNRangeArgs count(long count) {
            return new TsNRangeArgs().count(count);
        }

        /**
         * Create new {@link TsNRangeArgs} with an alignment timestamp ({@code ALIGN}) set.
         *
         * @param timestamp the alignment timestamp in milliseconds.
         * @return new {@link TsNRangeArgs}.
         * @see TsNRangeArgs#align(long)
         * @since 8.0
         */
        public static TsNRangeArgs align(long timestamp) {
            return new TsNRangeArgs().align(timestamp);
        }

        /**
         * Create new {@link TsNRangeArgs} with {@code ALIGN start} set.
         *
         * @return new {@link TsNRangeArgs}.
         * @see TsNRangeArgs#alignStart()
         * @since 8.0
         */
        public static TsNRangeArgs alignStart() {
            return new TsNRangeArgs().alignStart();
        }

        /**
         * Create new {@link TsNRangeArgs} with {@code ALIGN end} set.
         *
         * @return new {@link TsNRangeArgs}.
         * @see TsNRangeArgs#alignEnd()
         * @since 8.0
         */
        public static TsNRangeArgs alignEnd() {
            return new TsNRangeArgs().alignEnd();
        }

        /**
         * Create new {@link TsNRangeArgs} with the reported bucket timestamp ({@code BUCKETTIMESTAMP}) set.
         *
         * @param bucketTimestamp the reported bucket timestamp, must not be {@code null}.
         * @return new {@link TsNRangeArgs}.
         * @throws IllegalArgumentException if {@code bucketTimestamp} is {@code null}.
         * @see TsNRangeArgs#bucketTimestamp(TsBucketTimestamp)
         * @since 8.0
         */
        public static TsNRangeArgs bucketTimestamp(TsBucketTimestamp bucketTimestamp) {
            return new TsNRangeArgs().bucketTimestamp(bucketTimestamp);
        }

        /**
         * Create new {@link TsNRangeArgs} with {@code EMPTY} set.
         *
         * @return new {@link TsNRangeArgs}.
         * @see TsNRangeArgs#empty()
         * @since 8.0
         */
        public static TsNRangeArgs empty() {
            return new TsNRangeArgs().empty();
        }

        /**
         * Create new {@link TsNRangeArgs} with one aggregation list per key ({@code AGGREGATION}) set.
         *
         * @param bucketDuration the bucket duration in milliseconds.
         * @param aggregationsPerKey one non-empty aggregation list per key, in key order, must not be {@code null} or empty.
         * @return new {@link TsNRangeArgs}.
         * @throws IllegalArgumentException if {@code aggregationsPerKey} is {@code null} or empty, or contains a {@code null}
         *         or empty list or a {@code null} aggregation.
         * @see TsNRangeArgs#aggregation(long, List)
         * @since 8.0
         */
        public static TsNRangeArgs aggregation(long bucketDuration, List<List<TsAggregation>> aggregationsPerKey) {
            return new TsNRangeArgs().aggregation(bucketDuration, aggregationsPerKey);
        }

        /**
         * Create new {@link TsNRangeArgs} with one aggregation list per key ({@code AGGREGATION}) set.
         *
         * @param bucketDuration the bucket duration, must not be {@code null} or negative, and must be zero or at least one
         *        millisecond.
         * @param aggregationsPerKey one non-empty aggregation list per key, in key order, must not be {@code null} or empty.
         * @return new {@link TsNRangeArgs}.
         * @throws IllegalArgumentException if {@code bucketDuration} or {@code aggregationsPerKey} is invalid.
         * @see TsNRangeArgs#aggregation(Duration, List)
         * @since 8.0
         */
        public static TsNRangeArgs aggregation(Duration bucketDuration, List<List<TsAggregation>> aggregationsPerKey) {
            return new TsNRangeArgs().aggregation(bucketDuration, aggregationsPerKey);
        }

        /**
         * Create new {@link TsNRangeArgs} with the same aggregations for every key ({@code AGGREGATION}) set.
         *
         * @param bucketDuration the bucket duration in milliseconds.
         * @param aggregations the aggregations of each key, must not be {@code null} or empty and must not contain {@code null}
         *        elements.
         * @return new {@link TsNRangeArgs}.
         * @throws IllegalArgumentException if {@code aggregations} is {@code null}, empty or contains {@code null} elements.
         * @see TsNRangeArgs#aggregationForEachKey(long, TsAggregation...)
         * @since 8.0
         */
        public static TsNRangeArgs aggregationForEachKey(long bucketDuration, TsAggregation... aggregations) {
            return new TsNRangeArgs().aggregationForEachKey(bucketDuration, aggregations);
        }

        /**
         * Create new {@link TsNRangeArgs} with the same aggregations for every key ({@code AGGREGATION}) set.
         *
         * @param bucketDuration the bucket duration, must not be {@code null} or negative, and must be zero or at least one
         *        millisecond.
         * @param aggregations the aggregations of each key, must not be {@code null} or empty and must not contain {@code null}
         *        elements.
         * @return new {@link TsNRangeArgs}.
         * @throws IllegalArgumentException if {@code bucketDuration} or {@code aggregations} is invalid.
         * @see TsNRangeArgs#aggregationForEachKey(Duration, TsAggregation...)
         * @since 8.0
         */
        public static TsNRangeArgs aggregationForEachKey(Duration bucketDuration, TsAggregation... aggregations) {
            return new TsNRangeArgs().aggregationForEachKey(bucketDuration, aggregations);
        }

    }

    /**
     * Aggregate the samples of each key into buckets with its own aggregations ({@code AGGREGATION}). Each row then has, for
     * every key in key order, one value per aggregation of that key. The list must have exactly one entry per key of the
     * command. Replaces {@link #aggregationForEachKey(long, TsAggregation...)}.
     *
     * @param bucketDuration the bucket duration in milliseconds, shared by all keys.
     * @param aggregationsPerKey one non-empty aggregation list per key, in key order, must not be {@code null} or empty.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code aggregationsPerKey} is {@code null} or empty, or contains a {@code null} or
     *         empty list or a {@code null} aggregation.
     * @since 8.0
     */
    public TsNRangeArgs aggregation(long bucketDuration, List<List<TsAggregation>> aggregationsPerKey) {

        LettuceAssert.notNull(aggregationsPerKey, "Aggregations per key must not be null");
        LettuceAssert.isTrue(!aggregationsPerKey.isEmpty(), "Aggregations per key must not be empty");

        List<List<TsAggregation>> copy = new ArrayList<>(aggregationsPerKey.size());
        for (List<TsAggregation> aggregations : aggregationsPerKey) {
            LettuceAssert.notNull(aggregations, "Aggregations of a key must not be null");
            LettuceAssert.isTrue(!aggregations.isEmpty(), "Aggregations of a key must not be empty");
            LettuceAssert.noNullElements(aggregations, "Aggregations must not contain null elements");
            copy.add(Collections.unmodifiableList(new ArrayList<>(aggregations)));
        }

        this.nBucketDuration = bucketDuration;
        this.aggregationsPerKey = Collections.unmodifiableList(copy);
        this.aggregationsForEachKey = null;
        return this;
    }

    /**
     * Aggregate the samples of each key into buckets with its own aggregations ({@code AGGREGATION}). The duration is sent in
     * whole milliseconds. See {@link #aggregation(long, List)}.
     *
     * @param bucketDuration the bucket duration, must not be {@code null} or negative, and must be zero or at least one
     *        millisecond.
     * @param aggregationsPerKey one non-empty aggregation list per key, in key order, must not be {@code null} or empty.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code bucketDuration} is {@code null}, negative or not zero but shorter than one
     *         millisecond, or if {@code aggregationsPerKey} is invalid.
     * @since 8.0
     */
    public TsNRangeArgs aggregation(Duration bucketDuration, List<List<TsAggregation>> aggregationsPerKey) {
        return aggregation(toMillis(bucketDuration), aggregationsPerKey);
    }

    /**
     * Aggregate the samples of every key into buckets with the same aggregations ({@code AGGREGATION}). The aggregations are
     * sent once for every key of the command. Each row then has, for every key in key order, one value per aggregation.
     * Replaces {@link #aggregation(long, List)}.
     *
     * @param bucketDuration the bucket duration in milliseconds.
     * @param aggregations the aggregations of each key, must not be {@code null} or empty and must not contain {@code null}
     *        elements.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code aggregations} is {@code null}, empty or contains {@code null} elements.
     * @since 8.0
     */
    public TsNRangeArgs aggregationForEachKey(long bucketDuration, TsAggregation... aggregations) {

        LettuceAssert.notEmpty(aggregations, "Aggregations must not be empty");
        LettuceAssert.noNullElements(aggregations, "Aggregations must not contain null elements");

        this.nBucketDuration = bucketDuration;
        this.aggregationsForEachKey = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(aggregations)));
        this.aggregationsPerKey = null;
        return this;
    }

    /**
     * Aggregate the samples of every key into buckets with the same aggregations ({@code AGGREGATION}). The duration is sent in
     * whole milliseconds. See {@link #aggregationForEachKey(long, TsAggregation...)}.
     *
     * @param bucketDuration the bucket duration, must not be {@code null} or negative, and must be zero or at least one
     *        millisecond.
     * @param aggregations the aggregations of each key, must not be {@code null} or empty and must not contain {@code null}
     *        elements.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code bucketDuration} is {@code null}, negative or not zero but shorter than one
     *         millisecond, or if {@code aggregations} is {@code null}, empty or contains {@code null} elements.
     * @since 8.0
     */
    public TsNRangeArgs aggregationForEachKey(Duration bucketDuration, TsAggregation... aggregations) {
        return aggregationForEachKey(toMillis(bucketDuration), aggregations);
    }

    /**
     * Get the number of values per key in each row: {@code 1} for every key without an aggregation, otherwise the number of
     * aggregations of that key.
     *
     * @param keyCount the number of keys of the command, must be positive.
     * @return the value count of each key, in key order.
     * @throws IllegalArgumentException if {@code keyCount} is not positive, or if the per-key aggregation lists do not match
     *         {@code keyCount}.
     * @since 8.0
     */
    public List<Integer> getColumnCounts(int keyCount) {

        LettuceAssert.isTrue(keyCount > 0, "Key count must be positive");
        assertKeyCount(keyCount);

        List<Integer> counts = new ArrayList<>(keyCount);
        for (int i = 0; i < keyCount; i++) {
            if (aggregationsPerKey != null) {
                counts.add(aggregationsPerKey.get(i).size());
            } else if (aggregationsForEachKey != null) {
                counts.add(aggregationsForEachKey.size());
            } else {
                counts.add(1);
            }
        }

        return counts;
    }

    @Override
    boolean hasAggregation() {
        return nBucketDuration != null;
    }

    /**
     * Emit the options in the documented order: {@code LATEST}, {@code FILTER_BY_TS}, {@code FILTER_BY_VALUE}, {@code COUNT},
     * {@code ALIGN}, {@code AGGREGATION} with one aggregation argument per key and the bucket duration,
     * {@code BUCKETTIMESTAMP}, {@code EMPTY}. The command builder emits {@code numkeys}, the keys and the range first.
     *
     * @param args the command arguments to append to, must not be {@code null}.
     * @param keyCount the number of keys of the command, must be positive.
     * @param <K> Key type.
     * @param <V> Value type.
     * @throws IllegalArgumentException if {@code keyCount} is not positive, if the per-key aggregation lists do not match
     *         {@code keyCount}, or if {@code ALIGN}, {@code BUCKETTIMESTAMP} or {@code EMPTY} is set without an aggregation.
     * @since 8.0
     */
    public <K, V> void build(CommandArgs<K, V> args, int keyCount) {

        LettuceAssert.isTrue(keyCount > 0, "Key count must be positive");
        assertKeyCount(keyCount);
        validate();

        buildFilters(args);
        buildCountAndAlign(args);

        if (nBucketDuration != null) {
            args.add(CommandKeyword.AGGREGATION);
            for (int i = 0; i < keyCount; i++) {
                args.add(aggregationToken(aggregationsPerKey != null ? aggregationsPerKey.get(i) : aggregationsForEachKey));
            }
            args.add(nBucketDuration);
        }

        buildBucketOptions(args);
    }

    private void assertKeyCount(int keyCount) {

        if (aggregationsPerKey != null) {
            LettuceAssert.isTrue(aggregationsPerKey.size() == keyCount, () -> "Aggregation lists (" + aggregationsPerKey.size()
                    + ") must match the number of keys (" + keyCount + ")");
        }
    }

}
