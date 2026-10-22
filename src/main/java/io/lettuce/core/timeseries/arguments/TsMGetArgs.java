/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.mget/">TS.MGET</a> command. Options that are not
 * set are not sent; without {@link #withLabels()} or {@link #selectedLabels(String...)} no labels are returned.
 * {@link #withLabels()} and {@link #selectedLabels(String...)} cannot be combined.
 * <p>
 * {@link TsMGetArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class TsMGetArgs implements CompositeArgument {

    private boolean latest;

    private boolean withLabels;

    private String[] selectedLabels;

    /**
     * Builder entry points for {@link TsMGetArgs}.
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
         * Create new {@link TsMGetArgs} with {@code LATEST} set.
         *
         * @return new {@link TsMGetArgs}.
         * @see TsMGetArgs#latest()
         * @since 8.0
         */
        public static TsMGetArgs latest() {
            return new TsMGetArgs().latest();
        }

        /**
         * Create new {@link TsMGetArgs} with {@code WITHLABELS} set.
         *
         * @return new {@link TsMGetArgs}.
         * @see TsMGetArgs#withLabels()
         * @since 8.0
         */
        public static TsMGetArgs withLabels() {
            return new TsMGetArgs().withLabels();
        }

        /**
         * Create new {@link TsMGetArgs} with {@code SELECTED_LABELS} set.
         *
         * @param labels the label names, must not be {@code null} or empty and must not contain {@code null} elements.
         * @return new {@link TsMGetArgs}.
         * @throws IllegalArgumentException if {@code labels} is {@code null}, empty or contains {@code null} elements.
         * @see TsMGetArgs#selectedLabels(String...)
         * @since 8.0
         */
        public static TsMGetArgs selectedLabels(String... labels) {
            return new TsMGetArgs().selectedLabels(labels);
        }

    }

    /**
     * Report the latest, possibly partial, bucket of a compaction ({@code LATEST}). The server ignores it for a series that is
     * not a compaction.
     *
     * @return {@code this}.
     * @since 8.0
     */
    public TsMGetArgs latest() {
        this.latest = true;
        return this;
    }

    /**
     * Return all labels of each series ({@code WITHLABELS}).
     *
     * @return {@code this}.
     * @throws IllegalArgumentException if {@link #selectedLabels(String...)} was set.
     * @since 8.0
     */
    public TsMGetArgs withLabels() {

        LettuceAssert.isTrue(selectedLabels == null, "WITHLABELS cannot be combined with SELECTED_LABELS");

        this.withLabels = true;
        return this;
    }

    /**
     * Return only the given labels of each series ({@code SELECTED_LABELS}). A label that a series does not have is returned
     * with a {@code null} value.
     *
     * @param labels the label names, must not be {@code null} or empty and must not contain {@code null} elements.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code labels} is {@code null}, empty or contains {@code null} elements, or if
     *         {@link #withLabels()} was set.
     * @since 8.0
     */
    public TsMGetArgs selectedLabels(String... labels) {

        LettuceAssert.notEmpty(labels, "Labels must not be empty");
        LettuceAssert.noNullElements(labels, "Labels must not contain null elements");
        LettuceAssert.isTrue(!withLabels, "SELECTED_LABELS cannot be combined with WITHLABELS");

        this.selectedLabels = labels.clone();
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (latest) {
            args.add(CommandKeyword.LATEST);
        }

        if (withLabels) {
            args.add(CommandKeyword.WITHLABELS);
        }

        if (selectedLabels != null) {
            args.add(CommandKeyword.SELECTED_LABELS);
            for (String label : selectedLabels) {
                args.add(label);
            }
        }
    }

}
