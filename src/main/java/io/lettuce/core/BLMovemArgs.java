/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the blocking Redis <a href="https://redis.io/commands/blmovem">BLMOVEM</a> command. Wraps the
 * directions and optional count block of an {@link LMovemArgs} and adds the mandatory {@code timeout} that {@code BLMOVEM}
 * places between them. Static import the methods from {@link Builder} and chain the method calls:
 * {@code leftRight().count(2, Ordering.BULK).timeout(1.5)}.
 * <p>
 * The {@code timeout} defaults to {@code 0} (block indefinitely) when not set explicitly. This type intentionally does not
 * extend {@link LMovemArgs} so that it cannot be passed to the non-blocking {@code lmovem} command, which would emit an invalid
 * {@code LMOVEM} carrying a timeout.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 */
public class BLMovemArgs implements CompositeArgument {

    private final LMovemArgs lMovemArgs;

    private Long longTimeout;

    private Double doubleTimeout;

    private BLMovemArgs(LMovemArgs lMovemArgs) {
        this.lMovemArgs = lMovemArgs;
    }

    /**
     * Builder entry points for {@link BLMovemArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link BLMovemArgs} with {@code LEFT} {@code LEFT} directions.
         *
         * @return new {@link BLMovemArgs} with directions set.
         */
        public static BLMovemArgs leftLeft() {
            return new BLMovemArgs(LMovemArgs.Builder.leftLeft());
        }

        /**
         * Creates new {@link BLMovemArgs} with {@code LEFT} {@code RIGHT} directions.
         *
         * @return new {@link BLMovemArgs} with directions set.
         */
        public static BLMovemArgs leftRight() {
            return new BLMovemArgs(LMovemArgs.Builder.leftRight());
        }

        /**
         * Creates new {@link BLMovemArgs} with {@code RIGHT} {@code LEFT} directions.
         *
         * @return new {@link BLMovemArgs} with directions set.
         */
        public static BLMovemArgs rightLeft() {
            return new BLMovemArgs(LMovemArgs.Builder.rightLeft());
        }

        /**
         * Creates new {@link BLMovemArgs} with {@code RIGHT} {@code RIGHT} directions.
         *
         * @return new {@link BLMovemArgs} with directions set.
         */
        public static BLMovemArgs rightRight() {
            return new BLMovemArgs(LMovemArgs.Builder.rightRight());
        }

    }

    /**
     * Set the blocking {@code timeout} in seconds.
     *
     * @param timeout the timeout in seconds.
     * @return {@code this} {@link BLMovemArgs}.
     */
    public BLMovemArgs timeout(long timeout) {
        this.longTimeout = timeout;
        this.doubleTimeout = null;
        return this;
    }

    /**
     * Set the blocking {@code timeout} in seconds, allowing for sub-second resolution.
     *
     * @param timeout the timeout in seconds.
     * @return {@code this} {@link BLMovemArgs}.
     */
    public BLMovemArgs timeout(double timeout) {
        this.doubleTimeout = timeout;
        this.longTimeout = null;
        return this;
    }

    /**
     * Move up-to {@code count} elements. Follows the same semantics as the {@code count} parameter of {@code LPOP}.
     *
     * @param count the maximum number of elements to move.
     * @param ordering the {@link LMovemArgs.Ordering} in which the elements are moved, must not be {@code null}.
     * @return {@code this} {@link BLMovemArgs}.
     */
    public BLMovemArgs count(long count, LMovemArgs.Ordering ordering) {
        this.lMovemArgs.count(count, ordering);
        return this;
    }

    /**
     * Move exactly {@code count} elements or return an empty list if the source list does not have enough elements.
     *
     * @param count the exact number of elements to move.
     * @param ordering the {@link LMovemArgs.Ordering} in which the elements are moved, must not be {@code null}.
     * @return {@code this} {@link BLMovemArgs}.
     */
    public BLMovemArgs exactly(long count, LMovemArgs.Ordering ordering) {
        this.lMovemArgs.exactly(count, ordering);
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        LettuceAssert.notNull(lMovemArgs, "LMovemArgs must not be null");
        lMovemArgs.buildDirections(args);
        if (doubleTimeout != null) {
            args.add(doubleTimeout);
        } else {
            args.add(longTimeout != null ? longTimeout : 0L);
        }
        lMovemArgs.buildCount(args);
    }

}
