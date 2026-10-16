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
 * Argument list builder for the Redis <a href="https://redis.io/commands/lcs">LCS</a> command. Static import the methods from
 * {@link LcsArgs.Builder} and call the methods.
 * <p>
 * {@link LcsArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Seonghwan Lee
 * @author Yordan Tsintsov
 * @since 6.6
 * @see <a href="https://redis.io/commands/lcs">LCS command reference</a>
 */
public class LcsArgs implements CompositeArgument {

    private boolean justLen;

    private int minMatchLen;

    private boolean withMatchLen;

    private boolean withIdx;

    private String[] keys;

    /**
     * Builder entry points for {@link LcsArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link LcsArgs} by keys.
         *
         * @return new {@link LcsArgs} with {@literal By KEYS} set.
         * @deprecated since 7.8, pass the keys to
         *             {@link io.lettuce.core.api.sync.RedisStringCommands#lcs(Object, Object, LcsArgs)} instead and create the
         *             arguments through {@link #justLen()}, {@link #withIdx()}, {@link #minMatchLen(int)} or
         *             {@link #withMatchLen()}; scheduled for removal in a future major release. Keys set through this method
         *             are encoded as plain strings and not through the key codec, so they neither work with non-{@code String}
         *             key codecs nor participate in Redis Cluster slot routing.
         */
        @Deprecated
        public static LcsArgs keys(String... keys) {
            return new LcsArgs().by(keys);
        }

        /**
         * Creates new {@link LcsArgs} and enables {@literal LEN}.
         *
         * @return new {@link LcsArgs} with {@literal LEN} enabled.
         * @since 7.8
         */
        public static LcsArgs justLen() {
            return new LcsArgs().justLen();
        }

        /**
         * Creates new {@link LcsArgs} and enables {@literal IDX}.
         *
         * @return new {@link LcsArgs} with {@literal IDX} enabled.
         * @since 7.8
         */
        public static LcsArgs withIdx() {
            return new LcsArgs().withIdx();
        }

        /**
         * Creates new {@link LcsArgs} and sets {@literal MINMATCHLEN}.
         *
         * @param minMatchLen the minimal length of a match to be reported.
         * @return new {@link LcsArgs} with {@literal MINMATCHLEN} set.
         * @since 7.8
         */
        public static LcsArgs minMatchLen(int minMatchLen) {
            return new LcsArgs().minMatchLen(minMatchLen);
        }

        /**
         * Creates new {@link LcsArgs} and enables {@literal WITHMATCHLEN}.
         *
         * @return new {@link LcsArgs} with {@literal WITHMATCHLEN} enabled.
         * @since 7.8
         */
        public static LcsArgs withMatchLen() {
            return new LcsArgs().withMatchLen();
        }

    }

    /**
     * Restrict the list of matches to the ones of a given minimal length.
     *
     * @return {@code this} {@link LcsArgs}.
     */
    public LcsArgs minMatchLen(int minMatchLen) {
        this.minMatchLen = minMatchLen;
        return this;
    }

    /**
     * Request just the length of the match for results.
     *
     * @return {@code this} {@link LcsArgs}.
     */
    public LcsArgs justLen() {
        justLen = true;
        return this;
    }

    /**
     * Request match len for results.
     *
     * @return {@code this} {@link LcsArgs}.
     */
    public LcsArgs withMatchLen() {
        withMatchLen = true;
        return this;
    }

    /**
     * Request match position in each string for results.
     *
     * @return {@code this} {@link LcsArgs}.
     */
    public LcsArgs withIdx() {
        withIdx = true;
        return this;
    }

    /**
     * Set the keys.
     *
     * @param keys the keys, must not be empty.
     * @return {@code this} {@link LcsArgs}.
     * @throws IllegalArgumentException if {@code keys} is {@code null} or empty.
     * @deprecated since 7.8, pass the keys to {@link io.lettuce.core.api.sync.RedisStringCommands#lcs(Object, Object, LcsArgs)}
     *             instead; scheduled for removal in a future major release. Keys set through this method are encoded as plain
     *             strings and not through the key codec, so they neither work with non-{@code String} key codecs nor
     *             participate in Redis Cluster slot routing.
     */
    @Deprecated
    public LcsArgs by(String... keys) {
        LettuceAssert.notEmpty(keys, "Keys must not be empty");

        this.keys = keys;
        return this;
    }

    public boolean isWithIdx() {
        return withIdx;
    }

    /**
     * @return {@code true} if keys were set through the deprecated {@link #by(String...)} method.
     */
    boolean hasKeys() {
        return keys != null;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (keys != null) {
            for (String key : keys) {
                args.add(key);
            }
        }

        if (justLen) {
            args.add(CommandKeyword.LEN);
        }

        if (withIdx) {
            args.add(CommandKeyword.IDX);
        }

        if (minMatchLen > 0) {
            args.add(CommandKeyword.MINMATCHLEN);
            args.add(minMatchLen);
        }

        if (withMatchLen) {
            args.add(CommandKeyword.WITHMATCHLEN);
        }
    }

}
