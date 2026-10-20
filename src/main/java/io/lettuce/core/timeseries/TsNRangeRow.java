/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.internal.LettuceAssert;

/**
 * One row of a <a href="https://redis.io/commands/ts.nrange/">TS.NRANGE</a> or
 * <a href="https://redis.io/commands/ts.nrevrange/">TS.NREVRANGE</a> reply: a timestamp and the values of every key at that
 * timestamp, or in that bucket with an aggregation.
 * <p>
 * {@link #getValues()} is flat, in key order and then aggregation order. {@link #getValues(int)} returns the values of one key.
 * A key without data at the timestamp, or in the bucket, has {@code NaN} values, which cannot be told apart from a stored NaN.
 * Equality compares values with {@link Double#compare(double, double)} semantics, so {@code NaN} equals {@code NaN}.
 * {@link TsNRangeRow} is immutable.
 *
 * @since 8.0
 */
public final class TsNRangeRow {

    private final long timestamp;

    private final List<Double> values;

    private final List<Integer> columnCounts;

    private TsNRangeRow(long timestamp, List<Double> values, List<Integer> columnCounts) {
        this.timestamp = timestamp;
        this.values = values;
        this.columnCounts = columnCounts;
    }

    /**
     * Create a row.
     *
     * @param timestamp the timestamp in milliseconds.
     * @param values the values in key order and then aggregation order, must not be {@code null} and must not contain
     *        {@code null} elements.
     * @param columnCounts the number of values of each key, in key order, must not be {@code null} or empty, must contain only
     *        positive counts, and must add up to the number of values.
     * @return the row.
     * @throws IllegalArgumentException if an argument is invalid.
     * @since 8.0
     */
    public static TsNRangeRow of(long timestamp, List<Double> values, List<Integer> columnCounts) {

        LettuceAssert.notNull(values, "Values must not be null");
        LettuceAssert.noNullElements(values, "Values must not contain null elements");
        LettuceAssert.notNull(columnCounts, "Column counts must not be null");
        LettuceAssert.isTrue(!columnCounts.isEmpty(), "Column counts must not be empty");

        int total = 0;
        for (Integer count : columnCounts) {
            LettuceAssert.isTrue(count != null && count > 0, "Column counts must be positive");
            total += count;
        }
        int sum = total;
        LettuceAssert.isTrue(sum == values.size(),
                () -> "Column counts add up to " + sum + " but there are " + values.size() + " values");

        return new TsNRangeRow(timestamp, Collections.unmodifiableList(new ArrayList<>(values)),
                Collections.unmodifiableList(new ArrayList<>(columnCounts)));
    }

    /**
     * Get the timestamp of the row.
     *
     * @return the timestamp in milliseconds; the bucket timestamp with an aggregation.
     * @since 8.0
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get all values of the row, in key order and then aggregation order.
     *
     * @return the values, never {@code null}; {@code NaN} where a key has no data.
     * @since 8.0
     */
    public List<Double> getValues() {
        return values;
    }

    /**
     * Get the values of one key: one value without an aggregation, otherwise one value per aggregation of that key.
     *
     * @param keyIndex the position of the key in the command, starting at {@code 0}; a key given twice has two positions.
     * @return the values of the key; {@code NaN} where the key has no data.
     * @throws IndexOutOfBoundsException if {@code keyIndex} is negative or not less than the number of keys.
     * @since 8.0
     */
    public List<Double> getValues(int keyIndex) {

        if (keyIndex < 0 || keyIndex >= columnCounts.size()) {
            throw new IndexOutOfBoundsException("Key index " + keyIndex + ", keys: " + columnCounts.size());
        }

        int from = 0;
        for (int i = 0; i < keyIndex; i++) {
            from += columnCounts.get(i);
        }

        return values.subList(from, from + columnCounts.get(keyIndex));
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof TsNRangeRow)) {
            return false;
        }

        TsNRangeRow that = (TsNRangeRow) o;
        // Double.equals follows Double.compare semantics: NaN equals NaN
        return timestamp == that.timestamp && values.equals(that.values) && columnCounts.equals(that.columnCounts);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Long.hashCode(timestamp) + values.hashCode()) + columnCounts.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [timestamp=" + timestamp + ", values=" + values + ", columnCounts=" + columnCounts
                + "]";
    }

}
