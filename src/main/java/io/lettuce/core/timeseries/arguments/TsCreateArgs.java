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
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.create/">TS.CREATE</a> command.
 * <p>
 * {@link TsCreateArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public class TsCreateArgs implements CompositeArgument {

    private Long retention;

    private TsEncodingFormat encoding;

    private Long chunkSize;

    private TsDuplicatePolicy duplicatePolicy;

    private Long ignoreMaxTimeDiff;

    private Double ignoreMaxValDiff;

    private final Map<String, String> labels = new LinkedHashMap<>();

    /**
     * Builder entry points for {@link TsCreateArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link TsCreateArgs} and sets the retention period.
         *
         * @return a new {@link TsCreateArgs} with retention period configured.
         */
        public static TsCreateArgs retention(long retention) {
            return new TsCreateArgs().retention(retention);
        }

        /**
         * Creates a new {@link TsCreateArgs} and sets the chunk encoding.
         *
         * @return a new {@link TsCreateArgs} with encoding configured.
         */
        public static TsCreateArgs encoding(TsEncodingFormat encoding) {
            return new TsCreateArgs().encoding(encoding);
        }

        /**
         * Creates a new {@link TsCreateArgs} and sets the chunk size.
         *
         * @return a new {@link TsCreateArgs} with chunk size configured.
         */
        public static TsCreateArgs chunkSize(long chunkSize) {
            return new TsCreateArgs().chunkSize(chunkSize);
        }

        /**
         * Creates a new {@link TsCreateArgs} and sets the duplicate sample policy.
         *
         * @return a new {@link TsCreateArgs} with duplicate policy configured.
         */
        public static TsCreateArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
            return new TsCreateArgs().duplicatePolicy(duplicatePolicy);
        }

        /**
         * Creates a new {@link TsCreateArgs} and sets the ignore thresholds.
         *
         * @return a new {@link TsCreateArgs} with ignore thresholds configured.
         */
        public static TsCreateArgs ignore(long maxTimeDiff, double maxValDiff) {
            return new TsCreateArgs().ignore(maxTimeDiff, maxValDiff);
        }

        /**
         * Creates a new {@link TsCreateArgs} and adds a single label.
         *
         * @return a new {@link TsCreateArgs} with the given label configured.
         */
        public static TsCreateArgs label(String label, String value) {
            return new TsCreateArgs().label(label, value);
        }

        /**
         * Creates a new {@link TsCreateArgs} and sets the labels.
         *
         * @return a new {@link TsCreateArgs} with labels configured.
         */
        public static TsCreateArgs labels(Map<String, String> labels) {
            return new TsCreateArgs().labels(labels);
        }

    }

    /**
     * Set the retention period, in milliseconds.
     *
     * @return {@code this} {@link TsCreateArgs}.
     */
    public TsCreateArgs retention(long retention) {
        this.retention = retention;
        return this;
    }

    /**
     * Set the chunk encoding.
     *
     * @return {@code this} {@link TsCreateArgs}.
     */
    public TsCreateArgs encoding(TsEncodingFormat encoding) {
        this.encoding = encoding;
        return this;
    }

    /**
     * Set the chunk size, in bytes.
     *
     * @return {@code this} {@link TsCreateArgs}.
     */
    public TsCreateArgs chunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    /**
     * Set the duplicate sample policy.
     *
     * @return {@code this} {@link TsCreateArgs}.
     */
    public TsCreateArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
        this.duplicatePolicy = duplicatePolicy;
        return this;
    }

    /**
     * Set the maximum time and value difference under which a duplicate sample is ignored instead of applying the duplicate
     * policy.
     *
     * @return {@code this} {@link TsCreateArgs}.
     */
    public TsCreateArgs ignore(long maxTimeDiff, double maxValDiff) {
        this.ignoreMaxTimeDiff = maxTimeDiff;
        this.ignoreMaxValDiff = maxValDiff;
        return this;
    }

    /**
     * Add a single label. Repeated calls accumulate labels in call order; combine freely with {@link #labels(Map)}.
     *
     * @return {@code this} {@link TsCreateArgs}.
     */
    public TsCreateArgs label(String label, String value) {
        this.labels.put(label, value);
        return this;
    }

    /**
     * Replace the labels with the given map, preserving its iteration order.
     *
     * @return {@code this} {@link TsCreateArgs}.
     */
    public TsCreateArgs labels(Map<String, String> labels) {
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
        if (ignoreMaxTimeDiff != null) {
            args.add(CommandKeyword.IGNORE).add(ignoreMaxTimeDiff).add(ignoreMaxValDiff);
        }
        if (!labels.isEmpty()) {
            args.add(CommandKeyword.LABELS);
            labels.forEach((label, value) -> args.add(label).add(value));
        }
    }

}
