/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.time.Duration;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/ts.read/">TS.READ</a> command. Options that are not
 * set are not sent.
 * <p>
 * With {@link #block(long, long)}, the command waits until enough samples exist, the block time ends, or the key is removed,
 * and it occupies its connection while it waits. See the Javadoc of {@code tsRead} for how to choose the connection and its
 * timeout.
 * <p>
 * {@link TsReadArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class TsReadArgs implements CompositeArgument {

    private Long blockMillis;

    private Long minCount;

    private Long maxCount;

    /**
     * Builder entry points for {@link TsReadArgs}.
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
         * Create new {@link TsReadArgs} with {@code BLOCK} set.
         *
         * @param millis the block time in milliseconds, must not be negative; {@code 0} blocks without a time limit.
         * @param minCount the number of samples to wait for, must be positive.
         * @return new {@link TsReadArgs}.
         * @throws IllegalArgumentException if {@code millis} is negative or {@code minCount} is not positive.
         * @see TsReadArgs#block(long, long)
         * @since 8.0
         */
        public static TsReadArgs block(long millis, long minCount) {
            return new TsReadArgs().block(millis, minCount);
        }

        /**
         * Create new {@link TsReadArgs} with {@code BLOCK} set.
         *
         * @param timeout the block time, must not be {@code null} or negative, and must be zero or at least one millisecond;
         *        {@link Duration#ZERO} blocks without a time limit.
         * @param minCount the number of samples to wait for, must be positive.
         * @return new {@link TsReadArgs}.
         * @throws IllegalArgumentException if {@code timeout} is {@code null}, negative, or not zero but shorter than one
         *         millisecond, or if {@code minCount} is not positive.
         * @see TsReadArgs#block(Duration, long)
         * @since 8.0
         */
        public static TsReadArgs block(Duration timeout, long minCount) {
            return new TsReadArgs().block(timeout, minCount);
        }

        /**
         * Create new {@link TsReadArgs} with {@code MAX_COUNT} set.
         *
         * @param maxCount the maximum number of samples, must be positive.
         * @return new {@link TsReadArgs}.
         * @throws IllegalArgumentException if {@code maxCount} is not positive.
         * @see TsReadArgs#maxCount(long)
         * @since 8.0
         */
        public static TsReadArgs maxCount(long maxCount) {
            return new TsReadArgs().maxCount(maxCount);
        }

    }

    /**
     * Wait until at least {@code minCount} samples exist from the offset on, the block time ends, or the key is removed
     * ({@code BLOCK}). A timeout returns the samples found so far, possibly none; removing the key returns an empty list.
     *
     * @param millis the block time in milliseconds, must not be negative; {@code 0} blocks without a time limit.
     * @param minCount the number of samples to wait for, must be positive.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code millis} is negative, {@code minCount} is not positive, or {@code minCount} is
     *         greater than a {@code maxCount} set earlier.
     * @since 8.0
     */
    public TsReadArgs block(long millis, long minCount) {

        LettuceAssert.isTrue(millis >= 0, "Block time must not be negative");
        LettuceAssert.isTrue(minCount > 0, "Minimum count must be positive");
        assertCounts(minCount, maxCount);

        this.blockMillis = millis;
        this.minCount = minCount;
        return this;
    }

    /**
     * Wait until at least {@code minCount} samples exist from the offset on, the block time ends, or the key is removed
     * ({@code BLOCK}). The duration is sent in whole milliseconds.
     *
     * @param timeout the block time, must not be {@code null} or negative, and must be zero or at least one millisecond;
     *        {@link Duration#ZERO} blocks without a time limit.
     * @param minCount the number of samples to wait for, must be positive.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code timeout} is {@code null}, negative, or not zero but shorter than one
     *         millisecond, if {@code minCount} is not positive, or if {@code minCount} is greater than a {@code maxCount} set
     *         earlier.
     * @since 8.0
     */
    public TsReadArgs block(Duration timeout, long minCount) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");
        LettuceAssert.isTrue(!timeout.isNegative(), "Timeout must not be negative");
        LettuceAssert.isTrue(timeout.isZero() || timeout.toMillis() > 0, "Timeout must be zero or at least one millisecond");

        return block(timeout.toMillis(), minCount);
    }

    /**
     * Return at most {@code maxCount} samples, the oldest ones first ({@code MAX_COUNT}).
     *
     * @param maxCount the maximum number of samples, must be positive.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code maxCount} is not positive or is less than a {@code minCount} set earlier.
     * @since 8.0
     */
    public TsReadArgs maxCount(long maxCount) {

        LettuceAssert.isTrue(maxCount > 0, "Maximum count must be positive");
        assertCounts(minCount, maxCount);

        this.maxCount = maxCount;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        assertCounts(minCount, maxCount);

        if (blockMillis != null) {
            args.add(CommandKeyword.BLOCK).add(blockMillis).add(minCount);
        }

        if (maxCount != null) {
            args.add(CommandKeyword.MAX_COUNT).add(maxCount);
        }
    }

    private static void assertCounts(Long minCount, Long maxCount) {

        if (minCount != null && maxCount != null) {
            LettuceAssert.isTrue(maxCount >= minCount, "Maximum count must not be less than the minimum count");
        }
    }

}
