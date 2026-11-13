/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandKeyword.BYSCORE;
import static io.lettuce.core.protocol.CommandKeyword.COUNT;
import static io.lettuce.core.protocol.CommandKeyword.MAX;
import static io.lettuce.core.protocol.CommandKeyword.MIN;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.RangeEncoding;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Argument list builder for the {@code ZMOVE} command. Selects the members to move: the lowest scores ({@code MIN}), the
 * highest scores ({@code MAX}) or a score range ({@code BYSCORE}), optionally limited by a {@code COUNT}. Static import the
 * methods from {@link Builder} and chain the method calls: {@code byScore(Range.create(0, 100)).count(10)}.
 * <p>
 * {@link ZMoveArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class ZMoveArgs implements CompositeArgument {

    private final ProtocolKeyword selection;

    private final Range<? extends Number> range;

    private Long count;

    private ZMoveArgs(ProtocolKeyword selection, Range<? extends Number> range) {
        this.selection = selection;
        this.range = range;
    }

    /**
     * Builder entry points for {@link ZMoveArgs}.
     *
     * @since 8.0
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link ZMoveArgs} selecting the members with the lowest scores ({@code MIN}).
         *
         * @return new {@link ZMoveArgs} with {@code MIN} set.
         * @since 8.0
         */
        public static ZMoveArgs min() {
            return new ZMoveArgs(MIN, null);
        }

        /**
         * Creates new {@link ZMoveArgs} selecting the members with the highest scores ({@code MAX}).
         *
         * @return new {@link ZMoveArgs} with {@code MAX} set.
         * @since 8.0
         */
        public static ZMoveArgs max() {
            return new ZMoveArgs(MAX, null);
        }

        /**
         * Creates new {@link ZMoveArgs} selecting the members whose scores lie in {@code range} ({@code BYSCORE}). An unbounded
         * side is sent as {@code -inf} or {@code +inf}, an exclusive boundary with a leading {@code (}.
         *
         * @param range the score range, must not be {@code null}.
         * @return new {@link ZMoveArgs} with {@code BYSCORE} and the range set.
         * @throws IllegalArgumentException if {@code range} is {@code null}.
         * @since 8.0
         */
        public static ZMoveArgs byScore(Range<? extends Number> range) {
            LettuceAssert.notNull(range, "Range must not be null");
            return new ZMoveArgs(BYSCORE, range);
        }

    }

    /**
     * Move up to {@code count} members. The value is sent as given.
     *
     * @param count the maximum number of members to move.
     * @return {@code this} {@link ZMoveArgs}.
     * @since 8.0
     */
    public ZMoveArgs count(long count) {
        this.count = count;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        args.add(selection);

        if (range != null) {
            args.add(RangeEncoding.min(range)).add(RangeEncoding.max(range));
        }

        if (count != null) {
            args.add(COUNT).add(count);
        }
    }

}
