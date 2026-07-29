/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.mget/">TS.MGET</a> command.
 * <p>
 * {@code WITHLABELS} and {@code SELECTED_LABELS} are mutually exclusive; the server itself rejects the combination
 * ({@code TSDB: cannot accept WITHLABELS and SELECT_LABELS together}), and {@link #build(CommandArgs)} rejects it client-side
 * for the same reason. On the wire, {@code SELECTED_LABELS} greedily consumes tokens up to the next recognized keyword, so it
 * must be emitted before the {@code FILTER} keyword that the caller appends after this builder's output; {@link #build} never
 * emits {@code FILTER} itself.
 * <p>
 * {@link TsMGetArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public class TsMGetArgs implements CompositeArgument {

    private boolean latest;

    private boolean withLabels;

    private final List<String> selectedLabels = new ArrayList<>();

    /**
     * Builder entry points for {@link TsMGetArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link TsMGetArgs} and requests the compacted value of the latest, possibly partial, bucket.
         *
         * @return a new {@link TsMGetArgs} with {@code LATEST} configured.
         */
        public static TsMGetArgs latest() {
            return new TsMGetArgs().latest();
        }

        /**
         * Creates a new {@link TsMGetArgs} and requests that all labels of each matching series be included in the reply.
         *
         * @return a new {@link TsMGetArgs} with {@code WITHLABELS} configured.
         */
        public static TsMGetArgs withLabels() {
            return new TsMGetArgs().withLabels();
        }

        /**
         * Creates a new {@link TsMGetArgs} and requests that only the given labels of each matching series be included in the
         * reply.
         *
         * @return a new {@link TsMGetArgs} with {@code SELECTED_LABELS} configured.
         */
        public static TsMGetArgs selectedLabels(String... labels) {
            return new TsMGetArgs().selectedLabels(labels);
        }

    }

    /**
     * Request the compacted value of the latest, possibly partial, bucket. Only meaningful for series that are the destination
     * of a compaction rule; ignored otherwise.
     *
     * @return {@code this} {@link TsMGetArgs}.
     */
    public TsMGetArgs latest() {
        this.latest = true;
        return this;
    }

    /**
     * Request that all labels of each matching series be included in the reply. Mutually exclusive with
     * {@link #selectedLabels(String...)}.
     *
     * @return {@code this} {@link TsMGetArgs}.
     */
    public TsMGetArgs withLabels() {
        this.withLabels = true;
        return this;
    }

    /**
     * Request that only the given labels of each matching series be included in the reply. Mutually exclusive with
     * {@link #withLabels()}.
     *
     * @return {@code this} {@link TsMGetArgs}.
     */
    public TsMGetArgs selectedLabels(String... labels) {
        this.selectedLabels.clear();
        this.selectedLabels.addAll(Arrays.asList(labels));
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (withLabels && !selectedLabels.isEmpty()) {
            throw new IllegalArgumentException("TS.MGET does not accept WITHLABELS and SELECTED_LABELS together");
        }

        if (latest) {
            args.add(CommandKeyword.LATEST);
        }
        if (withLabels) {
            args.add(CommandKeyword.WITHLABELS);
        } else if (!selectedLabels.isEmpty()) {
            args.add(CommandKeyword.SELECTED_LABELS);
            selectedLabels.forEach(args::add);
        }
    }

}
