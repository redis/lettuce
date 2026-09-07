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
 * Argument list builder for the Redis <a href="https://redis.io/commands/sunioncard">SUNIONCARD</a> command. Static import the
 * methods from {@link Builder} and chain the method calls: {@code approx().limit(1000)}.
 * <p>
 * {@link SUnionCardArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 7.7
 */
public class SUnionCardArgs implements CompositeArgument {

    private boolean approx;

    private Long limit;

    /**
     * Builder entry points for {@link SUnionCardArgs}.
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
         * Creates new {@link SUnionCardArgs} with {@literal APPROX} set.
         *
         * @return new {@link SUnionCardArgs} with {@literal APPROX} set.
         * @see SUnionCardArgs#approx()
         * @since 7.7
         */
        public static SUnionCardArgs approx() {
            return new SUnionCardArgs().approx();
        }

        /**
         * Creates new {@link SUnionCardArgs} with {@literal LIMIT} set.
         *
         * @param limit the maximum cardinality to return, must not be negative. {@code 0} means no limit.
         * @return new {@link SUnionCardArgs} with {@literal LIMIT} set.
         * @see SUnionCardArgs#limit(long)
         * @since 7.7
         */
        public static SUnionCardArgs limit(long limit) {
            return new SUnionCardArgs().limit(limit);
        }

    }

    /**
     * Request an approximate cardinality computed using HyperLogLog instead of the exact cardinality.
     *
     * @return {@code this} {@link SUnionCardArgs}.
     * @since 7.7
     */
    public SUnionCardArgs approx() {

        this.approx = true;
        return this;
    }

    /**
     * Set the maximum cardinality to return. The server may stop the computation once the limit is reached.
     *
     * @param limit the maximum cardinality to return, must not be negative. {@code 0} means no limit.
     * @return {@code this} {@link SUnionCardArgs}.
     * @throws IllegalArgumentException if {@code limit} is negative.
     * @since 7.7
     */
    public SUnionCardArgs limit(long limit) {

        LettuceAssert.isTrue(limit >= 0, "Limit must not be negative");

        this.limit = limit;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (approx) {
            args.add(CommandKeyword.APPROX);
        }

        if (limit != null) {
            args.add(CommandKeyword.LIMIT).add(limit);
        }
    }

}
