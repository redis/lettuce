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
 * Sample of a TimeSeries: a timestamp and one or more values. A raw sample, or a bucket aggregated with one aggregation, has
 * one value. A bucket aggregated with several aggregations has one value per aggregation, in the order of the request.
 * <p>
 * Values may be NaN on Redis 8.6 and later. Equality compares values with {@link Double#compare(double, double)} semantics, so
 * {@code NaN} equals {@code NaN}. {@link TsSample} is immutable.
 *
 * @since 8.0
 */
public final class TsSample {

    private final long timestamp;

    private final List<Double> values;

    private TsSample(long timestamp, List<Double> values) {
        this.timestamp = timestamp;
        this.values = values;
    }

    /**
     * Create a sample with one value.
     *
     * @param timestamp the timestamp in milliseconds.
     * @param value the value.
     * @return the sample.
     * @since 8.0
     */
    public static TsSample of(long timestamp, double value) {
        return new TsSample(timestamp, Collections.singletonList(value));
    }

    /**
     * Create a sample with one or more values.
     *
     * @param timestamp the timestamp in milliseconds.
     * @param values the values, must not be {@code null} or empty and must not contain {@code null} elements.
     * @return the sample.
     * @throws IllegalArgumentException if {@code values} is {@code null}, empty or contains {@code null} elements.
     * @since 8.0
     */
    public static TsSample of(long timestamp, List<Double> values) {

        LettuceAssert.notNull(values, "Values must not be null");
        LettuceAssert.isTrue(!values.isEmpty(), "Values must not be empty");
        LettuceAssert.noNullElements(values, "Values must not contain null elements");

        return new TsSample(timestamp, Collections.unmodifiableList(new ArrayList<>(values)));
    }

    /**
     * Get the timestamp of the sample.
     *
     * @return the timestamp in milliseconds.
     * @since 8.0
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the first value of the sample. For a bucket aggregated with several aggregations, this is the value of the first
     * aggregation.
     *
     * @return the first value; may be NaN.
     * @since 8.0
     */
    public double getValue() {
        return values.get(0);
    }

    /**
     * Get all values of the sample: one value per aggregation, in the order of the request, or a single value.
     *
     * @return the values, never {@code null} or empty.
     * @since 8.0
     */
    public List<Double> getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof TsSample)) {
            return false;
        }

        TsSample that = (TsSample) o;
        // Double.equals follows Double.compare semantics: NaN equals NaN
        return timestamp == that.timestamp && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return 31 * Long.hashCode(timestamp) + values.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [timestamp=" + timestamp + ", values=" + values + "]";
    }

}
