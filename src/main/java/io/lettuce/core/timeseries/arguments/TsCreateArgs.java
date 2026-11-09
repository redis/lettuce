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
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.create/">TS.CREATE</a> command. Options that are
 * not set are not sent, so the server applies its configured defaults.
 * <p>
 * {@link TsCreateArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Aleksandar Todorov
 * @since 8.0
 */
public class TsCreateArgs implements CompositeArgument {

    private Long retention;

    private TsEncoding encoding;

    private Long chunkSize;

    private TsDuplicatePolicy duplicatePolicy;

    private Long ignoreMaxTimeDiff;

    private Double ignoreMaxValDiff;

    private Map<String, String> labels;

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

    /**
     * Set the maximum age of samples relative to the highest timestamp of the series ({@code RETENTION}). Older samples are
     * removed. {@code 0} means that samples never expire.
     *
     * @param millis the retention period in milliseconds, must not be negative.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code millis} is negative.
     * @since 8.0
     */
    public TsCreateArgs retention(long millis) {

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
    public TsCreateArgs retention(Duration retention) {
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
    public TsCreateArgs encoding(TsEncoding encoding) {

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
    public TsCreateArgs chunkSize(long bytes) {

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
    public TsCreateArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {

        LettuceAssert.notNull(duplicatePolicy, "Duplicate policy must not be null");

        this.duplicatePolicy = duplicatePolicy;
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
    public TsCreateArgs ignore(long maxTimeDiff, double maxValDiff) {

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
    public TsCreateArgs labels(Map<String, String> labels) {

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
    public TsCreateArgs label(String name, String value) {

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
     * {@code DUPLICATE_POLICY}, {@code IGNORE}, {@code LABELS}.
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
