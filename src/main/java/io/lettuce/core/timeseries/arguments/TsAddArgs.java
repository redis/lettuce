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
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.add/">TS.ADD</a> command. The series options
 * ({@code RETENTION}, {@code ENCODING}, {@code CHUNK_SIZE}, {@code DUPLICATE_POLICY}, {@code IGNORE}, {@code LABELS}) apply
 * only when the command creates the series; the server ignores them for an existing series. {@link #onDuplicate} applies to
 * this call only. Options that are not set are not sent.
 * <p>
 * {@link TsAddArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class TsAddArgs extends BaseTsCreateArgs<TsAddArgs> {

    /**
     * Builder entry points for {@link TsAddArgs}.
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
         * Create new {@link TsAddArgs} with the retention period in milliseconds ({@code RETENTION}) set.
         *
         * @param millis the retention period in milliseconds, must not be negative.
         * @return new {@link TsAddArgs}.
         * @throws IllegalArgumentException if {@code millis} is negative.
         * @see TsAddArgs#retention(long)
         * @since 8.0
         */
        public static TsAddArgs retention(long millis) {
            return new TsAddArgs().retention(millis);
        }

        /**
         * Create new {@link TsAddArgs} with the retention period ({@code RETENTION}) set.
         *
         * @param retention the retention period, must not be {@code null} or negative, and must be zero or at least one
         *        millisecond.
         * @return new {@link TsAddArgs}.
         * @throws IllegalArgumentException if {@code retention} is {@code null}, negative, or not zero but shorter than one
         *         millisecond.
         * @see TsAddArgs#retention(Duration)
         * @since 8.0
         */
        public static TsAddArgs retention(Duration retention) {
            return new TsAddArgs().retention(retention);
        }

        /**
         * Create new {@link TsAddArgs} with the chunk encoding ({@code ENCODING}) set.
         *
         * @param encoding the chunk encoding, must not be {@code null}.
         * @return new {@link TsAddArgs}.
         * @throws IllegalArgumentException if {@code encoding} is {@code null}.
         * @see TsAddArgs#encoding(TsEncoding)
         * @since 8.0
         */
        public static TsAddArgs encoding(TsEncoding encoding) {
            return new TsAddArgs().encoding(encoding);
        }

        /**
         * Create new {@link TsAddArgs} with the chunk size ({@code CHUNK_SIZE}) set.
         *
         * @param bytes the chunk size in bytes, must be positive.
         * @return new {@link TsAddArgs}.
         * @throws IllegalArgumentException if {@code bytes} is not positive.
         * @see TsAddArgs#chunkSize(long)
         * @since 8.0
         */
        public static TsAddArgs chunkSize(long bytes) {
            return new TsAddArgs().chunkSize(bytes);
        }

        /**
         * Create new {@link TsAddArgs} with the duplicate policy ({@code DUPLICATE_POLICY}) set.
         *
         * @param duplicatePolicy the duplicate policy, must not be {@code null}.
         * @return new {@link TsAddArgs}.
         * @throws IllegalArgumentException if {@code duplicatePolicy} is {@code null}.
         * @see TsAddArgs#duplicatePolicy(TsDuplicatePolicy)
         * @since 8.0
         */
        public static TsAddArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
            return new TsAddArgs().duplicatePolicy(duplicatePolicy);
        }

        /**
         * Create new {@link TsAddArgs} with the duplicate policy for this call ({@code ON_DUPLICATE}) set.
         *
         * @param policy the duplicate policy for this call, must not be {@code null}.
         * @return new {@link TsAddArgs}.
         * @throws IllegalArgumentException if {@code policy} is {@code null}.
         * @see TsAddArgs#onDuplicate(TsDuplicatePolicy)
         * @since 8.0
         */
        public static TsAddArgs onDuplicate(TsDuplicatePolicy policy) {
            return new TsAddArgs().onDuplicate(policy);
        }

        /**
         * Create new {@link TsAddArgs} with the duplicate-ignore thresholds ({@code IGNORE}) set.
         *
         * @param maxTimeDiff the maximum time difference in milliseconds, must not be negative.
         * @param maxValDiff the maximum value difference, must not be negative.
         * @return new {@link TsAddArgs}.
         * @throws IllegalArgumentException if {@code maxTimeDiff} or {@code maxValDiff} is negative.
         * @see TsAddArgs#ignore(long, double)
         * @since 8.0
         */
        public static TsAddArgs ignore(long maxTimeDiff, double maxValDiff) {
            return new TsAddArgs().ignore(maxTimeDiff, maxValDiff);
        }

        /**
         * Create new {@link TsAddArgs} with the labels ({@code LABELS}) set.
         *
         * @param labels the label names and values, must not be {@code null} and must not contain {@code null} names or values.
         * @return new {@link TsAddArgs}.
         * @throws IllegalArgumentException if {@code labels} is {@code null} or contains a {@code null} name or value.
         * @see TsAddArgs#labels(Map)
         * @since 8.0
         */
        public static TsAddArgs labels(Map<String, String> labels) {
            return new TsAddArgs().labels(labels);
        }

        /**
         * Create new {@link TsAddArgs} with one label ({@code LABELS}) set.
         *
         * @param name the label name, must not be {@code null}.
         * @param value the label value, must not be {@code null}.
         * @return new {@link TsAddArgs}.
         * @throws IllegalArgumentException if {@code name} or {@code value} is {@code null}.
         * @see TsAddArgs#label(String, String)
         * @since 8.0
         */
        public static TsAddArgs label(String name, String value) {
            return new TsAddArgs().label(name, value);
        }

    }

    /**
     * Set the duplicate policy for this call only ({@code ON_DUPLICATE}), overriding the duplicate policy of the series. It
     * does not change the policy of the series and has no effect when the call creates the series.
     *
     * @param policy the duplicate policy for this call, must not be {@code null}.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code policy} is {@code null}.
     * @since 8.0
     */
    public TsAddArgs onDuplicate(TsDuplicatePolicy policy) {

        LettuceAssert.notNull(policy, "Duplicate policy must not be null");

        this.onDuplicate = policy;
        return this;
    }

}
