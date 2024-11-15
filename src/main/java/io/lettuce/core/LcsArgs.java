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
 * {@link LcsArgs.Builder} and call the methods: {@code keys(…)} .
 * <p>
 * {@link LcsArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Seonghwan Lee
 * @since 6.6
 * @see <a href="https://redis.io/commands/lcs">LCS command refference</a>
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
         */
        public static LcsArgs keys(String... keys) {
            return new LcsArgs().by(keys);
        }

    }

    /**
     * restrict the list of matches to the ones of a given minimal length.
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

    public LcsArgs by(String... keys) {
        LettuceAssert.notEmpty(keys, "Keys must not be empty");

        this.keys = keys;
        return this;
    }

    public boolean isWithIdx() {
        return withIdx;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        for (String key : keys) {
            args.add(key);
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
