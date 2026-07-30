/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Argument list builder for the blocking Redis <a href="https://redis.io/commands/blmovem">BLMOVEM</a> command. Extends
 * {@link LMovemArgs} with the mandatory {@code timeout} that {@code BLMOVEM} places between the directions and the optional
 * count block. Static import the methods from {@link Builder} and chain the method calls:
 * {@code leftRight().count(2, Ordering.BULK).timeout(1.5)}.
 * <p>
 * The {@code timeout} defaults to {@code 0} (block indefinitely) when not set explicitly.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 */
public class BLMovemArgs extends LMovemArgs {

    private Long longTimeout;

    private Double doubleTimeout;

    private BLMovemArgs(ProtocolKeyword source, ProtocolKeyword destination) {
        super(source, destination);
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
            return new BLMovemArgs(CommandKeyword.LEFT, CommandKeyword.LEFT);
        }

        /**
         * Creates new {@link BLMovemArgs} with {@code LEFT} {@code RIGHT} directions.
         *
         * @return new {@link BLMovemArgs} with directions set.
         */
        public static BLMovemArgs leftRight() {
            return new BLMovemArgs(CommandKeyword.LEFT, CommandKeyword.RIGHT);
        }

        /**
         * Creates new {@link BLMovemArgs} with {@code RIGHT} {@code LEFT} directions.
         *
         * @return new {@link BLMovemArgs} with directions set.
         */
        public static BLMovemArgs rightLeft() {
            return new BLMovemArgs(CommandKeyword.RIGHT, CommandKeyword.LEFT);
        }

        /**
         * Creates new {@link BLMovemArgs} with {@code RIGHT} {@code RIGHT} directions.
         *
         * @return new {@link BLMovemArgs} with directions set.
         */
        public static BLMovemArgs rightRight() {
            return new BLMovemArgs(CommandKeyword.RIGHT, CommandKeyword.RIGHT);
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

    @Override
    public BLMovemArgs count(long count, Ordering ordering) {
        super.count(count, ordering);
        return this;
    }

    @Override
    public BLMovemArgs exactly(long count, Ordering ordering) {
        super.exactly(count, ordering);
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        buildDirections(args);
        if (doubleTimeout != null) {
            args.add(doubleTimeout);
        } else {
            args.add(longTimeout != null ? longTimeout : 0L);
        }
        buildCount(args);
    }

}
