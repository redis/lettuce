/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.core.protocol.CommandKeyword.LIMIT;
import static io.lettuce.core.protocol.CommandKeyword.REV;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/zrange/">ZRANGE</a> command with the
 * unified range syntax.
 * <p>
 * {@link ZRangeArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Yordan Tsintsov
 * @since 7.8
 */
public class ZRangeArgs implements CompositeArgument {

    private boolean rev = false;

    private Limit limit = Limit.unlimited();

    /**
     * Builder entry points for {@link ZRangeArgs}.
     *
     * @since 7.8
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link ZRangeArgs} and enabling {@literal REV}.
         *
         * @return new {@link ZRangeArgs} with {@literal REV} enabled.
         * @since 7.8
         */
        public static ZRangeArgs rev() {
            return new ZRangeArgs().rev();
        }

        /**
         * Creates new {@link ZRangeArgs} with a {@literal LIMIT} of {@code offset} and {@code count}.
         *
         * @param offset the offset within the range.
         * @param count the number of members to return, {@code -1} to return all members starting at {@code offset}.
         * @return new {@link ZRangeArgs} with the {@literal LIMIT} applied.
         * @since 7.8
         */
        public static ZRangeArgs limit(long offset, long count) {
            return new ZRangeArgs().limit(offset, count);
        }

        /**
         * Creates new {@link ZRangeArgs} with a specified {@link Limit}.
         *
         * @param limit the limit, must not be {@code null}.
         * @return new {@link ZRangeArgs} with the {@link Limit} applied.
         * @since 7.8
         */
        public static ZRangeArgs limit(Limit limit) {
            return new ZRangeArgs().limit(limit);
        }

    }

    /**
     * Reverse the traversal order, returning members from the highest to the lowest score (respective reverse lexicographical
     * order). The range boundaries keep their lower-to-upper meaning.
     *
     * @return {@code this} {@link ZRangeArgs}.
     * @since 7.8
     */
    public ZRangeArgs rev() {

        this.rev = true;
        return this;
    }

    /**
     * Limit the returned members to {@code count} members, starting at {@code offset} within the range. Requires a
     * {@link ZRange#byScore(Range)} or {@link ZRange#byLex(Range)} range.
     *
     * @param offset the offset within the range.
     * @param count the number of members to return, {@code -1} to return all members starting at {@code offset}.
     * @return {@code this} {@link ZRangeArgs}.
     * @since 7.8
     */
    public ZRangeArgs limit(long offset, long count) {
        return limit(Limit.create(offset, count));
    }

    /**
     * Limit the returned members according to {@link Limit}. Requires a {@link ZRange#byScore(Range)} or
     * {@link ZRange#byLex(Range)} range.
     *
     * @param limit the limit, must not be {@code null}.
     * @return {@code this} {@link ZRangeArgs}.
     * @since 7.8
     */
    public ZRangeArgs limit(Limit limit) {

        LettuceAssert.notNull(limit, "Limit must not be null");

        this.limit = limit;
        return this;
    }

    /**
     * Return whether {@literal REV} is enabled.
     *
     * @return {@code true} if {@literal REV} is enabled.
     * @since 7.8
     */
    public boolean isRev() {
        return rev;
    }

    /**
     * Return the {@link Limit}.
     *
     * @return the {@link Limit}, {@link Limit#unlimited()} if not set.
     * @since 7.8
     */
    public Limit getLimit() {
        return limit;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (rev) {
            args.add(REV);
        }

        if (limit.isLimited()) {
            args.add(LIMIT).add(limit.getOffset()).add(limit.getCount());
        }
    }

}
