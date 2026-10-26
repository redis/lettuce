/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.timeseries.TsDuplicatePolicy;
import io.lettuce.core.timeseries.TsDuplicatePolicyType;
import io.lettuce.core.timeseries.TsEncoding;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.add/">TS.ADD</a> command. The series options
 * ({@code RETENTION}, {@code ENCODING}, {@code CHUNK_SIZE}, {@code DUPLICATE_POLICY}, {@code IGNORE}, {@code LABELS}) apply
 * only when the command creates the series; the server ignores them for an existing series. {@link #onDuplicate} applies to
 * this call only. Options that are not set are not sent.
 * <p>
 * {@link TsAddArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Aleksandar Todorov
 * @since 8.0
 */
public class TsAddArgs implements CompositeArgument {

    private Long retention;

    private TsEncoding encoding;

    private Long chunkSize;

    private TsDuplicatePolicy duplicatePolicy;

    private TsDuplicatePolicy onDuplicate;

    private Long ignoreMaxTimeDiff;

    private Double ignoreMaxValDiff;

    private Map<String, String> labels;

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
     * Set the maximum age of samples relative to the highest timestamp of the series ({@code RETENTION}). Older samples are
     * removed. {@code 0} means that samples never expire.
     *
     * @param millis the retention period in milliseconds, must not be negative.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code millis} is negative.
     * @since 8.0
     */
    public TsAddArgs retention(long millis) {

        LettuceAssert.isTrue(millis >= 0, "Retention must not be negative");

        this.retention = millis;
        return this;
    }

    /**
     * Set the maximum age of samples relative to the highest timestamp of the series ({@code RETENTION}). Older samples are
     * removed. {@link Duration#ZERO} means that samples never expire. The duration is sent in whole milliseconds.
     *
     * @param retention the retention period, must not be {@code null} or negative, and must be zero or at least one
     *        millisecond.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code retention} is {@code null}, negative, or not zero but shorter than one
     *         millisecond.
     * @since 8.0
     */
    public TsAddArgs retention(Duration retention) {
        return retention(TsArgsSupport.retentionMillis(retention));
    }

    /**
     * Set the encoding of the data chunks of the series ({@code ENCODING}).
     *
     * @param encoding the chunk encoding, must not be {@code null}.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code encoding} is {@code null}.
     * @since 8.0
     */
    public TsAddArgs encoding(TsEncoding encoding) {

        LettuceAssert.notNull(encoding, "Encoding must not be null");

        this.encoding = encoding;
        return this;
    }

    /**
     * Set the memory size of each new data chunk ({@code CHUNK_SIZE}). The server requires a multiple of 8 within the range it
     * supports.
     *
     * @param bytes the chunk size in bytes, must be positive.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code bytes} is not positive.
     * @since 8.0
     */
    public TsAddArgs chunkSize(long bytes) {

        LettuceAssert.isTrue(bytes > 0, "Chunk size must be positive");

        this.chunkSize = bytes;
        return this;
    }

    /**
     * Set the policy for a sample whose timestamp already exists in the series ({@code DUPLICATE_POLICY}).
     *
     * @param duplicatePolicy the duplicate policy, must not be {@code null}.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code duplicatePolicy} is {@code null}.
     * @since 8.0
     */
    public TsAddArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {

        LettuceAssert.notNull(duplicatePolicy, "Duplicate policy must not be null");

        this.duplicatePolicy = duplicatePolicy;
        return this;
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

    /**
     * Set the thresholds under which a new sample is ignored as a duplicate ({@code IGNORE}). A sample is ignored when its
     * timestamp is at most {@code maxTimeDiff} after the highest timestamp of the series and its value differs from the value
     * at that timestamp by at most {@code maxValDiff}. An ignored sample is not stored, and the command replies with the
     * highest existing timestamp.
     * <p>
     * The server applies {@code IGNORE} only when the duplicate policy of the series is {@link TsDuplicatePolicyType#LAST};
     * with any other policy the sample is stored.
     *
     * @param maxTimeDiff the maximum time difference in milliseconds, must not be negative.
     * @param maxValDiff the maximum value difference, must not be negative.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code maxTimeDiff} or {@code maxValDiff} is negative.
     * @since 8.0
     */
    public TsAddArgs ignore(long maxTimeDiff, double maxValDiff) {

        LettuceAssert.isTrue(maxTimeDiff >= 0, "Maximum time difference must not be negative");
        LettuceAssert.isTrue(maxValDiff >= 0, "Maximum value difference must not be negative");

        this.ignoreMaxTimeDiff = maxTimeDiff;
        this.ignoreMaxValDiff = maxValDiff;
        return this;
    }

    /**
     * Set the labels of the series ({@code LABELS}), replacing labels set earlier on these arguments. Labels are sent in the
     * iteration order of {@code labels}. An empty map sends {@code LABELS} without pairs.
     *
     * @param labels the label names and values, must not be {@code null} and must not contain {@code null} names or values.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code labels} is {@code null} or contains a {@code null} name or value.
     * @since 8.0
     */
    public TsAddArgs labels(Map<String, String> labels) {

        this.labels = TsArgsSupport.copyLabels(labels);
        return this;
    }

    /**
     * Add a label to the series ({@code LABELS}). Labels are sent in the order they were added.
     *
     * @param name the label name, must not be {@code null}.
     * @param value the label value, must not be {@code null}.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code name} or {@code value} is {@code null}.
     * @since 8.0
     */
    public TsAddArgs label(String name, String value) {

        LettuceAssert.notNull(name, "Label name must not be null");
        LettuceAssert.notNull(value, "Label value must not be null");

        if (this.labels == null) {
            this.labels = new LinkedHashMap<>();
        }

        this.labels.put(name, value);
        return this;
    }

    /**
     * Emit the options in the documented order: {@code RETENTION}, {@code ENCODING}, {@code CHUNK_SIZE},
     * {@code DUPLICATE_POLICY}, {@code ON_DUPLICATE}, {@code IGNORE}, {@code LABELS}.
     *
     * @param args the command arguments to append to.
     */
    @Override
    public final <K, V> void build(CommandArgs<K, V> args) {

        if (retention != null) {
            args.add(CommandKeyword.RETENTION).add(retention);
        }

        if (encoding != null) {
            args.add(CommandKeyword.ENCODING).add(encoding);
        }

        if (chunkSize != null) {
            args.add(CommandKeyword.CHUNK_SIZE).add(chunkSize);
        }

        if (duplicatePolicy != null) {
            args.add(CommandKeyword.DUPLICATE_POLICY).add(duplicatePolicy);
        }

        if (onDuplicate != null) {
            args.add(CommandKeyword.ON_DUPLICATE).add(onDuplicate);
        }

        if (ignoreMaxTimeDiff != null) {
            args.add(CommandKeyword.IGNORE).add(ignoreMaxTimeDiff).add(ignoreMaxValDiff);
        }

        // LABELS must be last: the server stores every argument after LABELS as a label pair
        if (labels != null) {
            args.add(CommandKeyword.LABELS);
            labels.forEach((name, value) -> args.add(name).add(value));
        }
    }

}
