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
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.incrby/">TS.INCRBY</a> and
 * <a href="https://redis.io/commands/ts.decrby/">TS.DECRBY</a> commands.
 * <p>
 * {@code TS.INCRBY} and {@code TS.DECRBY} are registered by the server against the very same handler and accept an identical
 * option signature, so a single argument type covers both. Unlike {@code TS.ADD}, neither command recognizes
 * {@code ON_DUPLICATE}: the duplicate-policy override only applies to samples added to an already-existing series, and
 * {@code TS.INCRBY}/{@code TS.DECRBY} always resolve their own sample with the {@code LAST} policy internally. The
 * {@code DUPLICATE_POLICY} keyword is still accepted (and exposed here via {@link #duplicatePolicy(TsDuplicatePolicy)}), but
 * only takes effect when the series does not exist yet and is created inline by this call.
 * <p>
 * {@link TsIncrByArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public class TsIncrByArgs implements CompositeArgument {

    private Long timestamp;

    private Long retention;

    private TsEncodingFormat encoding;

    private Long chunkSize;

    private TsDuplicatePolicy duplicatePolicy;

    private Long ignoreMaxTimeDiff;

    private Double ignoreMaxValDiff;

    private final Map<String, String> labels = new LinkedHashMap<>();

    /**
     * Builder entry points for {@link TsIncrByArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link TsIncrByArgs} and sets the sample timestamp.
         *
         * @return a new {@link TsIncrByArgs} with the timestamp configured.
         */
        public static TsIncrByArgs timestamp(long timestamp) {
            return new TsIncrByArgs().timestamp(timestamp);
        }

        /**
         * Creates a new {@link TsIncrByArgs} and sets the retention period.
         *
         * @return a new {@link TsIncrByArgs} with retention period configured.
         */
        public static TsIncrByArgs retention(long retention) {
            return new TsIncrByArgs().retention(retention);
        }

        /**
         * Creates a new {@link TsIncrByArgs} and sets the chunk encoding.
         *
         * @return a new {@link TsIncrByArgs} with encoding configured.
         */
        public static TsIncrByArgs encoding(TsEncodingFormat encoding) {
            return new TsIncrByArgs().encoding(encoding);
        }

        /**
         * Creates a new {@link TsIncrByArgs} and sets the chunk size.
         *
         * @return a new {@link TsIncrByArgs} with chunk size configured.
         */
        public static TsIncrByArgs chunkSize(long chunkSize) {
            return new TsIncrByArgs().chunkSize(chunkSize);
        }

        /**
         * Creates a new {@link TsIncrByArgs} and sets the duplicate sample policy to apply if the series is created by this
         * call.
         *
         * @return a new {@link TsIncrByArgs} with duplicate policy configured.
         */
        public static TsIncrByArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
            return new TsIncrByArgs().duplicatePolicy(duplicatePolicy);
        }

        /**
         * Creates a new {@link TsIncrByArgs} and sets the ignore thresholds.
         *
         * @return a new {@link TsIncrByArgs} with ignore thresholds configured.
         */
        public static TsIncrByArgs ignore(long maxTimeDiff, double maxValDiff) {
            return new TsIncrByArgs().ignore(maxTimeDiff, maxValDiff);
        }

        /**
         * Creates a new {@link TsIncrByArgs} and adds a single label.
         *
         * @return a new {@link TsIncrByArgs} with the given label configured.
         */
        public static TsIncrByArgs label(String label, String value) {
            return new TsIncrByArgs().label(label, value);
        }

        /**
         * Creates a new {@link TsIncrByArgs} and sets the labels.
         *
         * @return a new {@link TsIncrByArgs} with labels configured.
         */
        public static TsIncrByArgs labels(Map<String, String> labels) {
            return new TsIncrByArgs().labels(labels);
        }

    }

    /**
     * Set the sample timestamp, in milliseconds. Defaults to the server's current time when omitted.
     *
     * @return {@code this} {@link TsIncrByArgs}.
     */
    public TsIncrByArgs timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Set the retention period, in milliseconds.
     *
     * @return {@code this} {@link TsIncrByArgs}.
     */
    public TsIncrByArgs retention(long retention) {
        this.retention = retention;
        return this;
    }

    /**
     * Set the chunk encoding.
     *
     * @return {@code this} {@link TsIncrByArgs}.
     */
    public TsIncrByArgs encoding(TsEncodingFormat encoding) {
        this.encoding = encoding;
        return this;
    }

    /**
     * Set the chunk size, in bytes.
     *
     * @return {@code this} {@link TsIncrByArgs}.
     */
    public TsIncrByArgs chunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    /**
     * Set the duplicate sample policy to apply if the series is created by this call.
     *
     * @return {@code this} {@link TsIncrByArgs}.
     */
    public TsIncrByArgs duplicatePolicy(TsDuplicatePolicy duplicatePolicy) {
        this.duplicatePolicy = duplicatePolicy;
        return this;
    }

    /**
     * Set the maximum time and value difference under which a duplicate sample is ignored instead of applying the duplicate
     * policy.
     *
     * @return {@code this} {@link TsIncrByArgs}.
     */
    public TsIncrByArgs ignore(long maxTimeDiff, double maxValDiff) {
        this.ignoreMaxTimeDiff = maxTimeDiff;
        this.ignoreMaxValDiff = maxValDiff;
        return this;
    }

    /**
     * Add a single label. Repeated calls accumulate labels in call order; combine freely with {@link #labels(Map)}.
     *
     * @return {@code this} {@link TsIncrByArgs}.
     */
    public TsIncrByArgs label(String label, String value) {
        this.labels.put(label, value);
        return this;
    }

    /**
     * Replace the labels with the given map, preserving its iteration order.
     *
     * @return {@code this} {@link TsIncrByArgs}.
     */
    public TsIncrByArgs labels(Map<String, String> labels) {
        this.labels.clear();
        this.labels.putAll(labels);
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (timestamp != null) {
            args.add(CommandKeyword.TIMESTAMP).add(timestamp);
        }
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
