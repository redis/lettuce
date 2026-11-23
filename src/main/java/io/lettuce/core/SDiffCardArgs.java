/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/sdiffcard">SDIFFCARD</a> command. Static import the
 * methods from {@link Builder} and chain the method calls: {@code limit(100)}.
 * <p>
 * {@link SDiffCardArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 7.7
 */
public class SDiffCardArgs implements CompositeArgument {

    private Long limit;

    /**
     * Builder entry points for {@link SDiffCardArgs}.
     *
     * @since 7.7
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link SDiffCardArgs} with {@literal LIMIT} set.
         *
         * @param limit the maximum cardinality to return, must not be negative. {@code 0} means no limit.
         * @return new {@link SDiffCardArgs} with {@literal LIMIT} set.
         * @see SDiffCardArgs#limit(long)
         * @since 7.7
         */
        public static SDiffCardArgs limit(long limit) {
            return new SDiffCardArgs().limit(limit);
        }

    }

    /**
     * Set the maximum cardinality to return. The server may stop the computation once the limit is reached.
     *
     * @param limit the maximum cardinality to return, must not be negative. {@code 0} means no limit.
     * @return {@code this} {@link SDiffCardArgs}.
     * @throws IllegalArgumentException if {@code limit} is negative.
     * @since 7.7
     */
    public SDiffCardArgs limit(long limit) {

        LettuceAssert.isTrue(limit >= 0, "Limit must not be negative");

        this.limit = limit;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (limit != null) {
            args.add(CommandKeyword.LIMIT).add(limit);
        }
    }

}
