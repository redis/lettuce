/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic.arguments;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/tdigest.merge/">TDIGEST.MERGE</a> command.
 * <p>
 * {@link TDigestMergeArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class TDigestMergeArgs implements CompositeArgument {

    private Long compression;

    private boolean override;

    /**
     * Builder entry points for {@link TDigestMergeArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates a new {@link TDigestMergeArgs} and sets the compression of the destination sketch.
         *
         * @param compression the compression parameter of the destination sketch.
         * @return a new {@link TDigestMergeArgs} with {@literal COMPRESSION} configured.
         */
        public static TDigestMergeArgs compression(long compression) {
            return new TDigestMergeArgs().compression(compression);
        }

        /**
         * Creates a new {@link TDigestMergeArgs} and sets the override flag.
         *
         * @return a new {@link TDigestMergeArgs} with {@literal OVERRIDE} configured.
         */
        public static TDigestMergeArgs override() {
            return new TDigestMergeArgs().override();
        }

        /**
         * Creates a new {@link TDigestMergeArgs} with default settings.
         *
         * @return a new {@link TDigestMergeArgs} with default settings.
         */
        public static TDigestMergeArgs defaults() {
            return new TDigestMergeArgs();
        }

    }

    /**
     * Set the compression of the destination sketch. Higher compression values yield more accurate estimates at the cost of
     * more memory and slower operations.
     *
     * @param compression the compression parameter of the destination sketch.
     * @return {@code this} {@link TDigestMergeArgs}.
     */
    public TDigestMergeArgs compression(long compression) {
        this.compression = compression;
        return this;
    }

    /**
     * Reset the destination sketch before the merge so that only the source data is retained. Without {@literal OVERRIDE} the
     * source data is merged into the existing destination data and the compression of the destination sketch is retained.
     *
     * @return {@code this} {@link TDigestMergeArgs}.
     */
    public TDigestMergeArgs override() {
        this.override = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (compression != null) {
            args.add(CommandKeyword.COMPRESSION).add(compression);
        }
        if (override) {
            args.add(CommandKeyword.OVERRIDE);
        }
    }

}
