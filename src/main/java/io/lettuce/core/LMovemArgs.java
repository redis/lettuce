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
 * Argument list builder for the Redis <a href="https://redis.io/commands/blmovem">BLMOVEM</a> and
 * <a href="https://redis.io/commands/lmovem">LMOVEM</a> commands. Static import the methods from {@link Builder} and chain the
 * method calls: {@code leftRight().count(2).bulk()}.
 * <p>
 * Directions ({@code LEFT}/{@code RIGHT}) are mandatory. The {@code COUNT}/{@code EXACTLY} block is optional; when applied it
 * requires an ordering ({@code OBO} or {@code BULK}) which defaults to {@code BULK}. Without a count block {@code LMOVEM} and
 * {@code BLMOVEM} are equivalent to {@code LMOVE} and {@code BLMOVE}.
 *
 * @author Aleksandar Todorov
 * @since 7.8
 */
public class LMovemArgs implements CompositeArgument {

    private final ProtocolKeyword source;

    private final ProtocolKeyword destination;

    private Long count;

    private ProtocolKeyword countType;

    private ProtocolKeyword ordering;

    private LMovemArgs(ProtocolKeyword source, ProtocolKeyword destination) {
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
     * Move up-to {@code count} elements. Follows the same semantics as the {@code count} parameter of {@code LPOP}. The
     * ordering defaults to {@code BULK}; use {@link #obo()} or {@link #bulk()} to select it explicitly.
     *
     * @param count the maximum number of elements to move.
     * @return {@code this} {@link LMovemArgs}.
     */
    public LMovemArgs count(long count) {
        this.countType = CommandKeyword.COUNT;
        this.count = count;
        return this;
    }

    /**
     * Move exactly {@code count} elements or return {@code null} if the source list does not have enough elements. The ordering
     * defaults to {@code BULK}; use {@link #obo()} or {@link #bulk()} to select it explicitly.
     *
     * @param count the exact number of elements to move.
     * @return {@code this} {@link LMovemArgs}.
     */
    public LMovemArgs exactly(long count) {
        this.countType = CommandKeyword.EXACTLY;
        this.count = count;
        return this;
    }

    /**
     * Move elements one by one ({@code OBO}), preserving the reversed order produced by popping one element at a time.
     *
     * @return {@code this} {@link LMovemArgs}.
     */
    public LMovemArgs obo() {
        this.ordering = CommandKeyword.OBO;
        return this;
    }

    /**
     * Move all elements in bulk ({@code BULK}), preserving their original order.
     *
     * @return {@code this} {@link LMovemArgs}.
     */
    public LMovemArgs bulk() {
        this.ordering = CommandKeyword.BULK;
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
            args.add(countType).add(count);
            args.add(ordering != null ? ordering : CommandKeyword.BULK);
        }
    }

}
