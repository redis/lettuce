/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic.topk.arguments;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/topk.reserve/">TOPK.RESERVE</a> command.
 * <p>
 * {@link TopKReserveArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class TopKReserveArgs implements CompositeArgument {

    private Long width;

    private Long depth;

    private Double decay;

    /**
     * Builder entry points for {@link TopKReserveArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link TopKReserveArgs} and sets the width of the filter.
         *
         * @return a new {@link TopKReserveArgs} with width configured.
         */
        public static TopKReserveArgs width(long width) {
            return new TopKReserveArgs().width(width);
        }

        /**
         * Creates a new {@link TopKReserveArgs} and sets the depth of the filter.
         *
         * @return a new {@link TopKReserveArgs} with depth configured.
         */
        public static TopKReserveArgs depth(long depth) {
            return new TopKReserveArgs().depth(depth);
        }

        /**
         * Creates a new {@link TopKReserveArgs} and sets the decay of the filter.
         *
         * @return a new {@link TopKReserveArgs} with decay configured.
         */
        public static TopKReserveArgs decay(double decay) {
            return new TopKReserveArgs().decay(decay);
        }

    }

    /**
     * Sets the width of the filter.
     *
     * @param width the width of the filter
     * @return {@code this} {@link TopKReserveArgs}
     */
    public TopKReserveArgs width(long width) {
        this.width = width;
        return this;
    }

    /**
     * Sets the depth of the filter.
     *
     * @param depth the depth of the filter
     * @return {@code this} {@link TopKReserveArgs}
     */
    public TopKReserveArgs depth(long depth) {
        this.depth = depth;
        return this;
    }

    /**
     * Sets the decay of the filter.
     *
     * @param decay the decay of the filter
     * @return {@code this} {@link TopKReserveArgs}
     */
    public TopKReserveArgs decay(double decay) {
        this.decay = decay;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (width != null) {
            args.add(width);
        }
        if (depth != null) {
            args.add(depth);
        }
        if (decay != null) {
            args.add(decay);
        }
    }

}
