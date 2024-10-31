/*
 * Copyright 2011-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/lcs">LCS</a> command. Static import the methods from
 * {@link LcsArgs.Builder} and call the methods: {@code keys(…)} .
 * <p>
 * {@link LcsArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Seonghwan Lee
 * @since 6.6
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
     * Request match position in each strings for results.
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
            args.add("LEN");
        }
        if (withIdx) {
            args.add("IDX");
        }

        if (minMatchLen > 0) {
            args.add("MINMATCHLEN");
            args.add(minMatchLen);
        }

        if (withMatchLen) {
            args.add("WITHMATCHLEN");
        }
    }

}
