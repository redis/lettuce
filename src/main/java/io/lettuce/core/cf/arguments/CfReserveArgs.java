/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cf.arguments;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/cf.reserve/">CF.RESERVE</a> command.
 * <p>
 * {@link CfReserveArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class CfReserveArgs implements CompositeArgument {

    private Long bucketSize;

    private Long maxIterations;

    private Long expansion;

    /**
     * Builder entry points for {@link CfReserveArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link CfReserveArgs} and sets the bucket size.
         *
         * @return a new {@link CfReserveArgs} with bucket size configured.
         */
        public static CfReserveArgs bucketSize(long bucketSize) {
            return new CfReserveArgs().bucketSize(bucketSize);
        }

        /**
         * Creates a new {@link CfReserveArgs} and sets the max iterations.
         *
         * @return a new {@link CfReserveArgs} with max iterations configured.
         */
        public static CfReserveArgs maxIterations(long maxIterations) {
            return new CfReserveArgs().maxIterations(maxIterations);
        }

        /**
         * Creates a new {@link CfReserveArgs} and sets the expansion rate of the filter.
         *
         * @return a new {@link CfReserveArgs} with expansion rate configured.
         */
        public static CfReserveArgs expansion(long expansion) {
            return new CfReserveArgs().expansion(expansion);
        }

    }

    /**
     * Sets the bucket size.
     *
     * @return this
     */
    public CfReserveArgs bucketSize(long bucketSize) {
        this.bucketSize = bucketSize;
        return this;
    }

    /**
     * Sets the max iterations.
     *
     * @return this
     */
    public CfReserveArgs maxIterations(long maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    /**
     * Sets the expansion rate of the filter.
     *
     * @return this
     */
    public CfReserveArgs expansion(long expansion) {
        this.expansion = expansion;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (bucketSize != null) {
            args.add(CommandKeyword.BUCKETSIZE).add(bucketSize);
        }
        if (maxIterations != null) {
            args.add(CommandKeyword.MAXITERATIONS).add(maxIterations);
        }
        if (expansion != null) {
            args.add(CommandKeyword.EXPANSION).add(expansion);
        }
    }

}
