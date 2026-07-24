/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.timeseries.TsDuplicatePolicy;
import io.lettuce.core.timeseries.TsEncodingFormat;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.add/">TS.ADD</a> command.
 * <p>
 * {@code TS.ADD} recognizes two distinct duplicate-policy keywords depending on whether the key already exists: when the series
 * does not exist yet and is created inline by this call, {@code DUPLICATE_POLICY} configures the policy stored on the newly
 * created series (the same keyword {@code TS.CREATE}/{@code TS.ALTER} use); when the series already exists,
 * {@code ON_DUPLICATE} overrides the policy for this single sample only, without persisting it on the series. Both keywords are
 * therefore exposed here as separate methods, {@link #duplicatePolicy(TsDuplicatePolicy)} and
 * {@link #onDuplicate(TsDuplicatePolicy)}.
 * <p>
 * {@link TsAddArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public class TsAddArgs implements CompositeArgument {

    private Long retention;

    private TsEncodingFormat encoding;

    private Long chunkSize;

    private TsDuplicatePolicy duplicatePolicy;

    private TsDuplicatePolicy onDuplicate;

    private Long ignoreMaxTimeDiff;

    private Double ignoreMaxValDiff;

    private final Map<String, String> labels = new LinkedHashMap<>();

    /**
     * Builder entry points for {@link TsAddArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link TsAddArgs} and sets the retention period.
         *
         * @return a new {@link TsAddArgs} with retention period configured.
         */
        public static TsAddArgs retention(long retention) {
            return new TsAddArgs().retention(retention);
        }

        /**
         * Creates a new {@link TsAddArgs} and sets the chunk encoding.
         *
         * @return a new {@link TsAddArgs} with encoding configured.
         */
        public static TsAddArgs encoding(TsEncodingFormat encoding) {
            return new TsAddArgs().encoding(encoding);
        }

        /**
         * Creates a new {@link TsAddArgs} and sets the chunk size.
         *
         * @return a new {@link TsAddArgs} with chunk size configured.
         */
        public static TsAddArgs chunkSize(long chunkSize) {
            return new TsAddArgs().chunkSize(chunkSize);
        }

        /**
         * Creates a new {@link TsAddArgs} and sets the duplicate sample policy to apply if the series is created by this call.
         *
         * @return a new {@link TsAddArgs} with duplicate policy configured.
         */
        public static TsAddArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
            return new TsAddArgs().duplicatePolicy(duplicatePolicy);
        }

        /**
         * Creates a new {@link TsAddArgs} and sets the duplicate sample policy override for an existing series.
         *
         * @return a new {@link TsAddArgs} with the duplicate policy override configured.
         */
        public static TsAddArgs onDuplicate(TsDuplicatePolicy onDuplicate) {
            return new TsAddArgs().onDuplicate(onDuplicate);
        }

        /**
         * Creates a new {@link TsAddArgs} and sets the ignore thresholds.
         *
         * @return a new {@link TsAddArgs} with ignore thresholds configured.
         */
        public static TsAddArgs ignore(long maxTimeDiff, double maxValDiff) {
            return new TsAddArgs().ignore(maxTimeDiff, maxValDiff);
        }

        /**
         * Creates a new {@link TsAddArgs} and adds a single label.
         *
         * @return a new {@link TsAddArgs} with the given label configured.
         */
        public static TsAddArgs label(String label, String value) {
            return new TsAddArgs().label(label, value);
        }

        /**
         * Creates a new {@link TsAddArgs} and sets the labels.
         *
         * @return a new {@link TsAddArgs} with labels configured.
         */
        public static TsAddArgs labels(Map<String, String> labels) {
            return new TsAddArgs().labels(labels);
        }

    }

    /**
     * Set the retention period, in milliseconds.
     *
     * @return {@code this} {@link TsAddArgs}.
     */
    public TsAddArgs retention(long retention) {
        this.retention = retention;
        return this;
    }

    /**
     * Set the chunk encoding.
     *
     * @return {@code this} {@link TsAddArgs}.
     */
    public TsAddArgs encoding(TsEncodingFormat encoding) {
        this.encoding = encoding;
        return this;
    }

    /**
     * Set the chunk size, in bytes.
     *
     * @return {@code this} {@link TsAddArgs}.
     */
    public TsAddArgs chunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    /**
     * Set the duplicate sample policy to apply if the series is created by this call.
     *
     * @return {@code this} {@link TsAddArgs}.
     */
    public TsAddArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
        this.duplicatePolicy = duplicatePolicy;
        return this;
    }

    /**
     * Override the duplicate sample policy for this sample when the series already exists.
     *
     * @return {@code this} {@link TsAddArgs}.
     */
    public TsAddArgs onDuplicate(TsDuplicatePolicy onDuplicate) {
        this.onDuplicate = onDuplicate;
        return this;
    }

    /**
     * Set the maximum time and value difference under which a duplicate sample is ignored instead of applying the duplicate
     * policy.
     *
     * @return {@code this} {@link TsAddArgs}.
     */
    public TsAddArgs ignore(long maxTimeDiff, double maxValDiff) {
        this.ignoreMaxTimeDiff = maxTimeDiff;
        this.ignoreMaxValDiff = maxValDiff;
        return this;
    }

    /**
     * Add a single label. Repeated calls accumulate labels in call order; combine freely with {@link #labels(Map)}.
     *
     * @return {@code this} {@link TsAddArgs}.
     */
    public TsAddArgs label(String label, String value) {
        this.labels.put(label, value);
        return this;
    }

    /**
     * Replace the labels with the given map, preserving its iteration order.
     *
     * @return {@code this} {@link TsAddArgs}.
     */
    public TsAddArgs labels(Map<String, String> labels) {
        this.labels.clear();
        this.labels.putAll(labels);
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

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
        if (!labels.isEmpty()) {
            args.add(CommandKeyword.LABELS);
            labels.forEach((label, value) -> args.add(label).add(value));
        }
    }

}
