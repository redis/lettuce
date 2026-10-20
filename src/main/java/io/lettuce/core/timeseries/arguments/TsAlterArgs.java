/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.time.Duration;
import java.util.Map;

import io.lettuce.core.timeseries.TsDuplicatePolicy;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.alter/">TS.ALTER</a> command. Only the options that
 * are set are changed. {@link #labels(Map)} replaces all labels of the series; an empty map removes all labels.
 * <p>
 * {@link TsAlterArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class TsAlterArgs extends BaseTsSeriesArgs<TsAlterArgs> {

    /**
     * Builder entry points for {@link TsAlterArgs}.
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
         * Create new {@link TsAlterArgs} with the retention period in milliseconds ({@code RETENTION}) set.
         *
         * @param millis the retention period in milliseconds, must not be negative.
         * @return new {@link TsAlterArgs}.
         * @throws IllegalArgumentException if {@code millis} is negative.
         * @see TsAlterArgs#retention(long)
         * @since 8.0
         */
        public static TsAlterArgs retention(long millis) {
            return new TsAlterArgs().retention(millis);
        }

        /**
         * Create new {@link TsAlterArgs} with the retention period ({@code RETENTION}) set.
         *
         * @param retention the retention period, must not be {@code null} or negative, and must be zero or at least one
         *        millisecond.
         * @return new {@link TsAlterArgs}.
         * @throws IllegalArgumentException if {@code retention} is {@code null}, negative, or not zero but shorter than one
         *         millisecond.
         * @see TsAlterArgs#retention(Duration)
         * @since 8.0
         */
        public static TsAlterArgs retention(Duration retention) {
            return new TsAlterArgs().retention(retention);
        }

        /**
         * Create new {@link TsAlterArgs} with the chunk size ({@code CHUNK_SIZE}) set.
         *
         * @param bytes the chunk size in bytes, must be positive.
         * @return new {@link TsAlterArgs}.
         * @throws IllegalArgumentException if {@code bytes} is not positive.
         * @see TsAlterArgs#chunkSize(long)
         * @since 8.0
         */
        public static TsAlterArgs chunkSize(long bytes) {
            return new TsAlterArgs().chunkSize(bytes);
        }

        /**
         * Create new {@link TsAlterArgs} with the duplicate policy ({@code DUPLICATE_POLICY}) set.
         *
         * @param duplicatePolicy the duplicate policy, must not be {@code null}.
         * @return new {@link TsAlterArgs}.
         * @throws IllegalArgumentException if {@code duplicatePolicy} is {@code null}.
         * @see TsAlterArgs#duplicatePolicy(TsDuplicatePolicy)
         * @since 8.0
         */
        public static TsAlterArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
            return new TsAlterArgs().duplicatePolicy(duplicatePolicy);
        }

        /**
         * Create new {@link TsAlterArgs} with the duplicate-ignore thresholds ({@code IGNORE}) set.
         *
         * @param maxTimeDiff the maximum time difference in milliseconds, must not be negative.
         * @param maxValDiff the maximum value difference, must not be negative.
         * @return new {@link TsAlterArgs}.
         * @throws IllegalArgumentException if {@code maxTimeDiff} or {@code maxValDiff} is negative.
         * @see TsAlterArgs#ignore(long, double)
         * @since 8.0
         */
        public static TsAlterArgs ignore(long maxTimeDiff, double maxValDiff) {
            return new TsAlterArgs().ignore(maxTimeDiff, maxValDiff);
        }

        /**
         * Create new {@link TsAlterArgs} with the labels ({@code LABELS}) set.
         *
         * @param labels the label names and values, must not be {@code null} and must not contain {@code null} names or values.
         * @return new {@link TsAlterArgs}.
         * @throws IllegalArgumentException if {@code labels} is {@code null} or contains a {@code null} name or value.
         * @see TsAlterArgs#labels(Map)
         * @since 8.0
         */
        public static TsAlterArgs labels(Map<String, String> labels) {
            return new TsAlterArgs().labels(labels);
        }

        /**
         * Create new {@link TsAlterArgs} with one label ({@code LABELS}) set.
         *
         * @param name the label name, must not be {@code null}.
         * @param value the label value, must not be {@code null}.
         * @return new {@link TsAlterArgs}.
         * @throws IllegalArgumentException if {@code name} or {@code value} is {@code null}.
         * @see TsAlterArgs#label(String, String)
         * @since 8.0
         */
        public static TsAlterArgs label(String name, String value) {
            return new TsAlterArgs().label(name, value);
        }

    }

}
