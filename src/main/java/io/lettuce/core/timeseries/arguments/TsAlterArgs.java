/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.timeseries.TsDuplicatePolicy;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.alter/">TS.ALTER</a> command.
 * <p>
 * Mirrors {@link TsCreateArgs} except that {@code ENCODING} is omitted, as the chunk encoding of an existing time series cannot
 * be changed.
 * <p>
 * {@link TsAlterArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public class TsAlterArgs implements CompositeArgument {

    private Long retention;

    private Long chunkSize;

    private TsDuplicatePolicy duplicatePolicy;

    private Long ignoreMaxTimeDiff;

    private Double ignoreMaxValDiff;

    private final Map<String, String> labels = new LinkedHashMap<>();

    private boolean labelsSet;

    /**
     * Builder entry points for {@link TsAlterArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link TsAlterArgs} and sets the retention period.
         *
         * @return a new {@link TsAlterArgs} with retention period configured.
         */
        public static TsAlterArgs retention(long retention) {
            return new TsAlterArgs().retention(retention);
        }

        /**
         * Creates a new {@link TsAlterArgs} and sets the chunk size.
         *
         * @return a new {@link TsAlterArgs} with chunk size configured.
         */
        public static TsAlterArgs chunkSize(long chunkSize) {
            return new TsAlterArgs().chunkSize(chunkSize);
        }

        /**
         * Creates a new {@link TsAlterArgs} and sets the duplicate sample policy.
         *
         * @return a new {@link TsAlterArgs} with duplicate policy configured.
         */
        public static TsAlterArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
            return new TsAlterArgs().duplicatePolicy(duplicatePolicy);
        }

        /**
         * Creates a new {@link TsAlterArgs} and sets the ignore thresholds.
         *
         * @return a new {@link TsAlterArgs} with ignore thresholds configured.
         */
        public static TsAlterArgs ignore(long maxTimeDiff, double maxValDiff) {
            return new TsAlterArgs().ignore(maxTimeDiff, maxValDiff);
        }

        /**
         * Creates a new {@link TsAlterArgs} and adds a single label.
         *
         * @return a new {@link TsAlterArgs} with the given label configured.
         */
        public static TsAlterArgs label(String label, String value) {
            return new TsAlterArgs().label(label, value);
        }

        /**
         * Creates a new {@link TsAlterArgs} and sets the labels.
         *
         * @return a new {@link TsAlterArgs} with labels configured.
         */
        public static TsAlterArgs labels(Map<String, String> labels) {
            return new TsAlterArgs().labels(labels);
        }

        /**
         * Creates a new {@link TsAlterArgs} that clears all existing labels on the series.
         *
         * @return a new {@link TsAlterArgs} with an empty label set configured.
         */
        public static TsAlterArgs labelsReset() {
            return new TsAlterArgs().labelsReset();
        }

    }

    /**
     * Set the retention period, in milliseconds.
     *
     * @return {@code this} {@link TsAlterArgs}.
     */
    public TsAlterArgs retention(long retention) {
        this.retention = retention;
        return this;
    }

    /**
     * Set the chunk size, in bytes.
     *
     * @return {@code this} {@link TsAlterArgs}.
     */
    public TsAlterArgs chunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    /**
     * Set the duplicate sample policy.
     *
     * @return {@code this} {@link TsAlterArgs}.
     */
    public TsAlterArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
        this.duplicatePolicy = duplicatePolicy;
        return this;
    }

    /**
     * Set the maximum time and value difference under which a duplicate sample is ignored instead of applying the duplicate
     * policy.
     *
     * @return {@code this} {@link TsAlterArgs}.
     */
    public TsAlterArgs ignore(long maxTimeDiff, double maxValDiff) {
        this.ignoreMaxTimeDiff = maxTimeDiff;
        this.ignoreMaxValDiff = maxValDiff;
        return this;
    }

    /**
     * Add a single label. Repeated calls accumulate labels in call order; combine freely with {@link #labels(Map)}.
     *
     * @return {@code this} {@link TsAlterArgs}.
     */
    public TsAlterArgs label(String label, String value) {
        this.labels.put(label, value);
        this.labelsSet = true;
        return this;
    }

    /**
     * Replace the labels with the given map, preserving its iteration order.
     *
     * @return {@code this} {@link TsAlterArgs}.
     */
    public TsAlterArgs labels(Map<String, String> labels) {
        this.labels.clear();
        this.labels.putAll(labels);
        this.labelsSet = true;
        return this;
    }

    /**
     * Clear all existing labels on the series by sending an empty {@code LABELS} keyword.
     *
     * @return {@code this} {@link TsAlterArgs}.
     */
    public TsAlterArgs labelsReset() {
        return labels(Collections.emptyMap());
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (retention != null) {
            args.add(CommandKeyword.RETENTION).add(retention);
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
        if (labelsSet) {
            args.add(CommandKeyword.LABELS);
            labels.forEach((label, value) -> args.add(label).add(value));
        }
    }

}
