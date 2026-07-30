/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/blmovem">BLMOVEM</a> and
 * <a href="https://redis.io/commands/lmovem">LMOVEM</a> commands. Static import the methods from {@link Builder} and chain the
 * method calls: {@code leftRight().count(2, Ordering.BULK)}.
 * <p>
 * Directions ({@code LEFT}/{@code RIGHT}) are mandatory. The {@code COUNT}/{@code EXACTLY} block is optional; when applied it
 * requires an {@link Ordering} ({@code OBO} or {@code BULK}). Without a count block {@code LMOVEM} and {@code BLMOVEM} are
 * equivalent to {@code LMOVE} and {@code BLMOVE}.
 *
 * @author Aleksandar Todorov
 * @since 7.7
 */
public class LMovemArgs implements CompositeArgument {

    /**
     * Ordering in which the moved elements are popped from the source and pushed to the destination.
     */
    public enum Ordering {

        /**
         * Move elements one by one, preserving the reversed order produced by popping one element at a time.
         */
        OBO(CommandKeyword.OBO),

        /**
         * Move all elements in bulk, preserving their original order.
         */
        BULK(CommandKeyword.BULK);

        private final ProtocolKeyword keyword;

        Ordering(ProtocolKeyword keyword) {
            this.keyword = keyword;
        }

    }

    private final ProtocolKeyword source;

    private final ProtocolKeyword destination;

    private Long count;

    private ProtocolKeyword countType;

    private ProtocolKeyword ordering;

    protected LMovemArgs(ProtocolKeyword source, ProtocolKeyword destination) {
        this.source = source;
        this.destination = destination;
    }

    /**
     * Builder entry points for {@link LMovemArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link LMovemArgs} setting with {@code LEFT} {@code LEFT} directions.
         *
         * @return new {@link LMovemArgs} with args set.
         */
        public static LMovemArgs leftLeft() {
            return new LMovemArgs(CommandKeyword.LEFT, CommandKeyword.LEFT);
        }

        /**
         * Creates new {@link LMovemArgs} setting with {@code LEFT} {@code RIGHT} directions.
         *
         * @return new {@link LMovemArgs} with args set.
         */
        public static LMovemArgs leftRight() {
            return new LMovemArgs(CommandKeyword.LEFT, CommandKeyword.RIGHT);
        }

        /**
         * Creates new {@link LMovemArgs} setting with {@code RIGHT} {@code LEFT} directions.
         *
         * @return new {@link LMovemArgs} with args set.
         */
        public static LMovemArgs rightLeft() {
            return new LMovemArgs(CommandKeyword.RIGHT, CommandKeyword.LEFT);
        }

        /**
         * Creates new {@link LMovemArgs} setting with {@code RIGHT} {@code RIGHT} directions.
         *
         * @return new {@link LMovemArgs} with args set.
         */
        public static LMovemArgs rightRight() {
            return new LMovemArgs(CommandKeyword.RIGHT, CommandKeyword.RIGHT);
        }

    }

    /**
     * Move up-to {@code count} elements. Follows the same semantics as the {@code count} parameter of {@code LPOP}.
     *
     * @param count the maximum number of elements to move.
     * @param ordering the {@link Ordering} in which the elements are moved, must not be {@code null}.
     * @return {@code this} {@link LMovemArgs}.
     */
    public LMovemArgs count(long count, Ordering ordering) {
        LettuceAssert.notNull(ordering, "Ordering must not be null");
        this.countType = CommandKeyword.COUNT;
        this.count = count;
        this.ordering = ordering.keyword;
        return this;
    }

    /**
     * Move exactly {@code count} elements or return an empty list if the source list does not have enough elements.
     *
     * @param count the exact number of elements to move.
     * @param ordering the {@link Ordering} in which the elements are moved, must not be {@code null}.
     * @return {@code this} {@link LMovemArgs}.
     */
    public LMovemArgs exactly(long count, Ordering ordering) {
        LettuceAssert.notNull(ordering, "Ordering must not be null");
        this.countType = CommandKeyword.EXACTLY;
        this.count = count;
        this.ordering = ordering.keyword;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        buildDirections(args);
        buildCount(args);
    }

    <K, V> void buildDirections(CommandArgs<K, V> args) {
        args.add(source).add(destination);
    }

    <K, V> void buildCount(CommandArgs<K, V> args) {
        if (countType != null) {
            args.add(countType).add(count).add(ordering);
        }
    }

}
