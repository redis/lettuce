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
 * Argument list builder for the Redis <a href="https://redis.io/commands/bitrangeintarray">BITRANGEINTARRAY</a> command. Static
 * import the methods from {@link Builder} and chain the method calls: {@code bit().limit(100).zeros()}.
 * <p>
 * The unit of the range is {@literal BYTE} unless {@link #bit()} is called. The options are sent in the order unit,
 * {@literal LIMIT}, {@literal ZEROS}, each at most once.
 * <p>
 * {@link BitRangeIntArrayArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class BitRangeIntArrayArgs implements CompositeArgument {

    private CommandKeyword unit;

    private Long limit;

    private boolean zeros;

    /**
     * Builder entry points for {@link BitRangeIntArrayArgs}.
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
         * Create new empty {@link BitRangeIntArrayArgs}.
         *
         * @return new {@link BitRangeIntArrayArgs}.
         * @since 8.0
         */
        public static BitRangeIntArrayArgs empty() {
            return new BitRangeIntArrayArgs();
        }

        /**
         * Create new {@link BitRangeIntArrayArgs} with the range in bits ({@literal BIT}).
         *
         * @return new {@link BitRangeIntArrayArgs} with {@literal BIT} set.
         * @see BitRangeIntArrayArgs#bit()
         * @since 8.0
         */
        public static BitRangeIntArrayArgs bit() {
            return new BitRangeIntArrayArgs().bit();
        }

        /**
         * Create new {@link BitRangeIntArrayArgs} with the range in bytes ({@literal BYTE}).
         *
         * @return new {@link BitRangeIntArrayArgs} with {@literal BYTE} set.
         * @see BitRangeIntArrayArgs#byteUnit()
         * @since 8.0
         */
        public static BitRangeIntArrayArgs byteUnit() {
            return new BitRangeIntArrayArgs().byteUnit();
        }

        /**
         * Create new {@link BitRangeIntArrayArgs} with {@literal LIMIT} set.
         *
         * @param count the number of lowest offsets to return, must be greater or equal to {@code 0}.
         * @return new {@link BitRangeIntArrayArgs} with {@literal LIMIT} set.
         * @throws IllegalArgumentException if {@code count} is negative.
         * @see BitRangeIntArrayArgs#limit(long)
         * @since 8.0
         */
        public static BitRangeIntArrayArgs limit(long count) {
            return new BitRangeIntArrayArgs().limit(count);
        }

        /**
         * Create new {@link BitRangeIntArrayArgs} with {@literal ZEROS} set.
         *
         * @return new {@link BitRangeIntArrayArgs} with {@literal ZEROS} set.
         * @see BitRangeIntArrayArgs#zeros()
         * @since 8.0
         */
        public static BitRangeIntArrayArgs zeros() {
            return new BitRangeIntArrayArgs().zeros();
        }

    }

    /**
     * Interpret {@code start} and {@code end} as bit indexes ({@literal BIT}). Replaces an earlier {@link #byteUnit()}.
     *
     * @return {@code this}.
     * @since 8.0
     */
    public BitRangeIntArrayArgs bit() {

        this.unit = CommandKeyword.BIT;
        return this;
    }

    /**
     * Interpret {@code start} and {@code end} as byte indexes ({@literal BYTE}), the server's default. Replaces an earlier
     * {@link #bit()}.
     *
     * @return {@code this}.
     * @since 8.0
     */
    public BitRangeIntArrayArgs byteUnit() {

        this.unit = CommandKeyword.BYTE;
        return this;
    }

    /**
     * Return only the lowest {@code count} offsets of the range ({@literal LIMIT}). A {@code count} of {@code 0} returns an
     * empty list. In byte units the server completes the byte that holds the last offset, so the reply can hold up to 7 offsets
     * more than {@code count}.
     *
     * @param count the number of lowest offsets to return, must be greater or equal to {@code 0}.
     * @return {@code this}.
     * @throws IllegalArgumentException if {@code count} is negative.
     * @since 8.0
     */
    public BitRangeIntArrayArgs limit(long count) {

        LettuceAssert.isTrue(count >= 0, "Limit must be greater or equal to 0");

        this.limit = count;
        return this;
    }

    /**
     * Return the offsets of the clear bits instead of the set bits ({@literal ZEROS}).
     *
     * @return {@code this}.
     * @since 8.0
     */
    public BitRangeIntArrayArgs zeros() {

        this.zeros = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (unit != null) {
            args.add(unit);
        }

        if (limit != null) {
            args.add(CommandKeyword.LIMIT).add(limit);
        }

        if (zeros) {
            args.add(CommandKeyword.ZEROS);
        }
    }

}
