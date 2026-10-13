/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandKeyword.*;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis {@literal BLESS SCAN} command. Static import the methods from {@link Builder} and chain
 * the method calls: {@code count(100)}.
 * <p>
 * {@link BlessScanArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 7.9
 */
public class BlessScanArgs implements CompositeArgument {

    private Long count;

    /**
     * Builder entry points for {@link BlessScanArgs}.
     *
     * @since 7.9
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link BlessScanArgs} with {@literal COUNT} set.
         *
         * @param count hint for the number of index entries to visit per call, must be greater than zero.
         * @return new {@link BlessScanArgs} with {@literal COUNT} set.
         * @since 7.9
         * @see BlessScanArgs#count(long)
         */
        public static BlessScanArgs count(long count) {
            return new BlessScanArgs().count(count);
        }

    }

    /**
     * Set the {@literal COUNT} hint. The server visits about {@code count} index entries per call; the number of keys returned
     * may be higher or lower.
     *
     * @param count hint for the number of index entries to visit per call, must be greater than zero.
     * @return {@literal this} {@link BlessScanArgs}.
     * @since 7.9
     */
    public BlessScanArgs count(long count) {

        this.count = count;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (count != null) {
            args.add(COUNT).add(count);
        }
    }

}
