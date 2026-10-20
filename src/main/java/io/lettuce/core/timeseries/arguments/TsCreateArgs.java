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
import io.lettuce.core.timeseries.TsEncoding;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.create/">TS.CREATE</a> command. Options that are
 * not set are not sent, so the server applies its configured defaults.
 * <p>
 * {@link TsCreateArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class TsCreateArgs extends BaseTsCreateArgs<TsCreateArgs> {

    /**
     * Builder entry points for {@link TsCreateArgs}.
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
         * Create new {@link TsCreateArgs} with the retention period in milliseconds ({@code RETENTION}) set.
         *
         * @param millis the retention period in milliseconds, must not be negative.
         * @return new {@link TsCreateArgs}.
         * @throws IllegalArgumentException if {@code millis} is negative.
         * @see TsCreateArgs#retention(long)
         * @since 8.0
         */
        public static TsCreateArgs retention(long millis) {
            return new TsCreateArgs().retention(millis);
        }

        /**
         * Create new {@link TsCreateArgs} with the retention period ({@code RETENTION}) set.
         *
         * @param retention the retention period, must not be {@code null} or negative, and must be zero or at least one
         *        millisecond.
         * @return new {@link TsCreateArgs}.
         * @throws IllegalArgumentException if {@code retention} is {@code null}, negative, or not zero but shorter than one
         *         millisecond.
         * @see TsCreateArgs#retention(Duration)
         * @since 8.0
         */
        public static TsCreateArgs retention(Duration retention) {
            return new TsCreateArgs().retention(retention);
        }

        /**
         * Create new {@link TsCreateArgs} with the chunk encoding ({@code ENCODING}) set.
         *
         * @param encoding the chunk encoding, must not be {@code null}.
         * @return new {@link TsCreateArgs}.
         * @throws IllegalArgumentException if {@code encoding} is {@code null}.
         * @see TsCreateArgs#encoding(TsEncoding)
         * @since 8.0
         */
        public static TsCreateArgs encoding(TsEncoding encoding) {
            return new TsCreateArgs().encoding(encoding);
        }

        /**
         * Create new {@link TsCreateArgs} with the chunk size ({@code CHUNK_SIZE}) set.
         *
         * @param bytes the chunk size in bytes, must be positive.
         * @return new {@link TsCreateArgs}.
         * @throws IllegalArgumentException if {@code bytes} is not positive.
         * @see TsCreateArgs#chunkSize(long)
         * @since 8.0
         */
        public static TsCreateArgs chunkSize(long bytes) {
            return new TsCreateArgs().chunkSize(bytes);
        }

        /**
         * Create new {@link TsCreateArgs} with the duplicate policy ({@code DUPLICATE_POLICY}) set.
         *
         * @param duplicatePolicy the duplicate policy, must not be {@code null}.
         * @return new {@link TsCreateArgs}.
         * @throws IllegalArgumentException if {@code duplicatePolicy} is {@code null}.
         * @see TsCreateArgs#duplicatePolicy(TsDuplicatePolicy)
         * @since 8.0
         */
        public static TsCreateArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
            return new TsCreateArgs().duplicatePolicy(duplicatePolicy);
        }

        /**
         * Create new {@link TsCreateArgs} with the duplicate-ignore thresholds ({@code IGNORE}) set.
         *
         * @param maxTimeDiff the maximum time difference in milliseconds, must not be negative.
         * @param maxValDiff the maximum value difference, must not be negative.
         * @return new {@link TsCreateArgs}.
         * @throws IllegalArgumentException if {@code maxTimeDiff} or {@code maxValDiff} is negative.
         * @see TsCreateArgs#ignore(long, double)
         * @since 8.0
         */
        public static TsCreateArgs ignore(long maxTimeDiff, double maxValDiff) {
            return new TsCreateArgs().ignore(maxTimeDiff, maxValDiff);
        }

        /**
         * Create new {@link TsCreateArgs} with the labels ({@code LABELS}) set.
         *
         * @param labels the label names and values, must not be {@code null} and must not contain {@code null} names or values.
         * @return new {@link TsCreateArgs}.
         * @throws IllegalArgumentException if {@code labels} is {@code null} or contains a {@code null} name or value.
         * @see TsCreateArgs#labels(Map)
         * @since 8.0
         */
        public static TsCreateArgs labels(Map<String, String> labels) {
            return new TsCreateArgs().labels(labels);
        }

        /**
         * Create new {@link TsCreateArgs} with one label ({@code LABELS}) set.
         *
         * @param name the label name, must not be {@code null}.
         * @param value the label value, must not be {@code null}.
         * @return new {@link TsCreateArgs}.
         * @throws IllegalArgumentException if {@code name} or {@code value} is {@code null}.
         * @see TsCreateArgs#label(String, String)
         * @since 8.0
         */
        public static TsCreateArgs label(String name, String value) {
            return new TsCreateArgs().label(name, value);
        }

    }

}
