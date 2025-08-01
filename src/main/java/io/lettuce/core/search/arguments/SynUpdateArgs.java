/*
 * Copyright 2011-2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/ft.synupdate/">FT.SYNUPDATE</a> command.
 * Static import methods are available.
 * <p>
 * {@link SynUpdateArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
public class SynUpdateArgs<K, V> {

    private boolean skipInitialScan = false;

    /**
     * Builder entry points for {@link SynUpdateArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link SynUpdateArgs} setting {@literal SKIPINITIALSCAN}.
         *
         * @return new {@link SynUpdateArgs} with {@literal SKIPINITIALSCAN} set.
         * @see SynUpdateArgs#skipInitialScan()
         */
        public static <K, V> SynUpdateArgs<K, V> skipInitialScan() {
            return new SynUpdateArgs<K, V>().skipInitialScan();
        }

    }

    /**
     * Skip the initial scan of all documents when updating the synonym group. Only documents that are indexed after the update
     * are affected.
     *
     * @return {@code this} {@link SynUpdateArgs}.
     */
    public SynUpdateArgs<K, V> skipInitialScan() {
        this.skipInitialScan = true;
        return this;
    }

    /**
     * Builds the arguments and appends them to the {@link CommandArgs}.
     *
     * @param args the command arguments to append to.
     */
    public void build(CommandArgs<K, V> args) {
        if (skipInitialScan) {
            args.add("SKIPINITIALSCAN");
        }
    }

}
