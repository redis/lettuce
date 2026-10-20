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
 * Base for the TimeSeries series-option arguments. Holds the options that every series-creating or series-altering command
 * accepts and emits all options in one fixed order, with {@code LABELS} last.
 *
 * @param <T> the concrete subtype, for fluent method chaining.
 * @since 8.0
 */
abstract class BaseTsSeriesArgs<T extends BaseTsSeriesArgs<T>> implements CompositeArgument {

    private Long retention;

    private Long chunkSize;

    private TsDuplicatePolicy duplicatePolicy;

    private Long ignoreMaxTimeDiff;

    private Double ignoreMaxValDiff;

    private Map<String, String> labels;

    // options of the subclasses, emitted at their fixed position by build(CommandArgs)

    TsEncoding encoding;

    @SuppressWarnings("unchecked")
    final T self() {
        return (T) this;
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
    public T retention(long millis) {

        LettuceAssert.isTrue(millis >= 0, "Retention must not be negative");

        this.retention = millis;
        return self();
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
    public T retention(Duration retention) {

        LettuceAssert.notNull(retention, "Retention must not be null");
        LettuceAssert.isTrue(!retention.isNegative(), "Retention must not be negative");
        LettuceAssert.isTrue(retention.isZero() || retention.toMillis() > 0,
                "Retention must be zero or at least one millisecond");

        return retention(retention.toMillis());
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
    public T chunkSize(long bytes) {

        LettuceAssert.isTrue(bytes > 0, "Chunk size must be positive");

        this.chunkSize = bytes;
        return self();
    }

    /**
     * Set the policy for a sample whose timestamp already exists in the series ({@code DUPLICATE_POLICY}).
     *
     * @param duplicatePolicy the duplicate policy, must not be {@code null}.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code duplicatePolicy} is {@code null}.
     * @since 8.0
     */
    public T duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {

        LettuceAssert.notNull(duplicatePolicy, "Duplicate policy must not be null");

        this.duplicatePolicy = duplicatePolicy;
        return self();
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
    public T ignore(long maxTimeDiff, double maxValDiff) {

        LettuceAssert.isTrue(maxTimeDiff >= 0, "Maximum time difference must not be negative");
        LettuceAssert.isTrue(maxValDiff >= 0, "Maximum value difference must not be negative");

        this.ignoreMaxTimeDiff = maxTimeDiff;
        this.ignoreMaxValDiff = maxValDiff;
        return self();
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
    public T labels(Map<String, String> labels) {

        LettuceAssert.notNull(labels, "Labels must not be null");

        Map<String, String> copy = new LinkedHashMap<>();
        labels.forEach((name, value) -> {
            LettuceAssert.notNull(name, "Label name must not be null");
            LettuceAssert.notNull(value, "Label value must not be null");
            copy.put(name, value);
        });

        this.labels = copy;
        return self();
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
    public T label(String name, String value) {

        LettuceAssert.notNull(name, "Label name must not be null");
        LettuceAssert.notNull(value, "Label value must not be null");

        if (this.labels == null) {
            this.labels = new LinkedHashMap<>();
        }

        this.labels.put(name, value);
        return self();
    }

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
