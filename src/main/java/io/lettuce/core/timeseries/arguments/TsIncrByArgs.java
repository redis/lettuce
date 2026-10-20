/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.time.Duration;
import java.util.Map;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.timeseries.TsDuplicatePolicy;
import io.lettuce.core.timeseries.TsEncoding;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.incrby/">TS.INCRBY</a> and
 * <a href="https://redis.io/commands/ts.decrby/">TS.DECRBY</a> commands. The series options ({@code RETENTION},
 * {@code ENCODING}, {@code CHUNK_SIZE}, {@code DUPLICATE_POLICY}, {@code IGNORE}, {@code LABELS}) apply only when the command
 * creates the series; the server ignores them for an existing series. Options that are not set are not sent.
 * <p>
 * {@link TsIncrByArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class TsIncrByArgs extends BaseTsCreateArgs<TsIncrByArgs> {

    /**
     * Builder entry points for {@link TsIncrByArgs}.
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
         * Create new {@link TsIncrByArgs} with the timestamp ({@code TIMESTAMP}) set.
         *
         * @param timestamp the timestamp in milliseconds, must not be negative.
         * @return new {@link TsIncrByArgs}.
         * @throws IllegalArgumentException if {@code timestamp} is negative.
         * @see TsIncrByArgs#timestamp(long)
         * @since 8.0
         */
        public static TsIncrByArgs timestamp(long timestamp) {
            return new TsIncrByArgs().timestamp(timestamp);
        }

        /**
         * Create new {@link TsIncrByArgs} with the retention period in milliseconds ({@code RETENTION}) set.
         *
         * @param millis the retention period in milliseconds, must not be negative.
         * @return new {@link TsIncrByArgs}.
         * @throws IllegalArgumentException if {@code millis} is negative.
         * @see TsIncrByArgs#retention(long)
         * @since 8.0
         */
        public static TsIncrByArgs retention(long millis) {
            return new TsIncrByArgs().retention(millis);
        }

        /**
         * Create new {@link TsIncrByArgs} with the retention period ({@code RETENTION}) set.
         *
         * @param retention the retention period, must not be {@code null} or negative, and must be zero or at least one
         *        millisecond.
         * @return new {@link TsIncrByArgs}.
         * @throws IllegalArgumentException if {@code retention} is {@code null}, negative, or not zero but shorter than one
         *         millisecond.
         * @see TsIncrByArgs#retention(Duration)
         * @since 8.0
         */
        public static TsIncrByArgs retention(Duration retention) {
            return new TsIncrByArgs().retention(retention);
        }

        /**
         * Create new {@link TsIncrByArgs} with the chunk encoding ({@code ENCODING}) set.
         *
         * @param encoding the chunk encoding, must not be {@code null}.
         * @return new {@link TsIncrByArgs}.
         * @throws IllegalArgumentException if {@code encoding} is {@code null}.
         * @see TsIncrByArgs#encoding(TsEncoding)
         * @since 8.0
         */
        public static TsIncrByArgs encoding(TsEncoding encoding) {
            return new TsIncrByArgs().encoding(encoding);
        }

        /**
         * Create new {@link TsIncrByArgs} with the chunk size ({@code CHUNK_SIZE}) set.
         *
         * @param bytes the chunk size in bytes, must be positive.
         * @return new {@link TsIncrByArgs}.
         * @throws IllegalArgumentException if {@code bytes} is not positive.
         * @see TsIncrByArgs#chunkSize(long)
         * @since 8.0
         */
        public static TsIncrByArgs chunkSize(long bytes) {
            return new TsIncrByArgs().chunkSize(bytes);
        }

        /**
         * Create new {@link TsIncrByArgs} with the duplicate policy ({@code DUPLICATE_POLICY}) set.
         *
         * @param duplicatePolicy the duplicate policy, must not be {@code null}.
         * @return new {@link TsIncrByArgs}.
         * @throws IllegalArgumentException if {@code duplicatePolicy} is {@code null}.
         * @see TsIncrByArgs#duplicatePolicy(TsDuplicatePolicy)
         * @since 8.0
         */
        public static TsIncrByArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
            return new TsIncrByArgs().duplicatePolicy(duplicatePolicy);
        }

        /**
         * Create new {@link TsIncrByArgs} with the duplicate-ignore thresholds ({@code IGNORE}) set.
         *
         * @param maxTimeDiff the maximum time difference in milliseconds, must not be negative.
         * @param maxValDiff the maximum value difference, must not be negative.
         * @return new {@link TsIncrByArgs}.
         * @throws IllegalArgumentException if {@code maxTimeDiff} or {@code maxValDiff} is negative.
         * @see TsIncrByArgs#ignore(long, double)
         * @since 8.0
         */
        public static TsIncrByArgs ignore(long maxTimeDiff, double maxValDiff) {
            return new TsIncrByArgs().ignore(maxTimeDiff, maxValDiff);
        }

        /**
         * Create new {@link TsIncrByArgs} with the labels ({@code LABELS}) set.
         *
         * @param labels the label names and values, must not be {@code null} and must not contain {@code null} names or values.
         * @return new {@link TsIncrByArgs}.
         * @throws IllegalArgumentException if {@code labels} is {@code null} or contains a {@code null} name or value.
         * @see TsIncrByArgs#labels(Map)
         * @since 8.0
         */
        public static TsIncrByArgs labels(Map<String, String> labels) {
            return new TsIncrByArgs().labels(labels);
        }

        /**
         * Create new {@link TsIncrByArgs} with one label ({@code LABELS}) set.
         *
         * @param name the label name, must not be {@code null}.
         * @param value the label value, must not be {@code null}.
         * @return new {@link TsIncrByArgs}.
         * @throws IllegalArgumentException if {@code name} or {@code value} is {@code null}.
         * @see TsIncrByArgs#label(String, String)
         * @since 8.0
         */
        public static TsIncrByArgs label(String name, String value) {
            return new TsIncrByArgs().label(name, value);
        }

    }

    /**
     * Set the timestamp of the sample to update ({@code TIMESTAMP}). The server requires a timestamp equal to or higher than
     * the highest timestamp of the series. Without it, the server clock is used.
     *
     * @param timestamp the timestamp in milliseconds, must not be negative.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code timestamp} is negative.
     * @since 8.0
     */
    public TsIncrByArgs timestamp(long timestamp) {

        LettuceAssert.isTrue(timestamp >= 0, "Timestamp must not be negative");

        this.timestamp = timestamp;
        return this;
    }

}
