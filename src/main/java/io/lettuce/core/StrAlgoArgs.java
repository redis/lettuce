/*
 * Copyright 2011-2020 the original author or authors.
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
import io.lettuce.core.protocol.StringAlgorithm;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/stralgo">STRALGO</a> command.
 * Static import the methods from {@link StrAlgoArgs.Builder} and call the methods: {@code block(…)} .
 * <p>
 * {@link StrAlgoArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author dengliming
 * @since 6.0
 */
public class StrAlgoArgs implements CompositeArgument {

    private boolean justLen;
    private int minMatchLen;
    private boolean withMatchLen;
    private boolean withIdx;
    private StringAlgorithm algorithm = StringAlgorithm.LCS;
    private By by = By.STRINGS;
    private String[] keys;

    /**
     * Builder entry points for {@link StrAlgoArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link StrAlgoArgs} by keys.
         *
         * @return new {@link StrAlgoArgs} with {@literal By KEYS} set.
         */
        public static StrAlgoArgs keys(String... keys) {
            return new StrAlgoArgs().by(By.KEYS, keys);
        }

        /**
         * Creates new {@link StrAlgoArgs} by strings.
         *
         * @return new {@link StrAlgoArgs} with {@literal By STRINGS} set.
         */
        public static StrAlgoArgs strings(String... strings) {
            return new StrAlgoArgs().by(By.STRINGS, strings);
        }
    }
    /**
     * restrict the list of matches to the ones of a given minimal length.
     *
     * @return {@code this} {@link StrAlgoArgs}.
     */
    public StrAlgoArgs minMatchLen(int minMatchLen) {
        this.minMatchLen = minMatchLen;
        return this;
    }

    /**
     * Request just the length of the match for results.
     *
     * @return {@code this} {@link StrAlgoArgs}.
     */
    public StrAlgoArgs justLen() {
        justLen = true;
        return this;
    }

    /**
     * Request match len for results.
     *
     * @return {@code this} {@link StrAlgoArgs}.
     */
    public StrAlgoArgs withMatchLen() {
        withMatchLen = true;
        return this;
    }

    /**
     * Request match position in each strings for results.
     *
     * @return {@code this} {@link StrAlgoArgs}.
     */
    public StrAlgoArgs withIdx() {
        withIdx = true;
        return this;
    }

    public StrAlgoArgs by(By by, String... keys) {
        this.by = by;
        this.keys = keys;
        return this;
    }

    public boolean isWithIdx() {
        return withIdx;
    }

    public enum By {
        STRINGS, KEYS
    }

    public <K, V> void build(CommandArgs<K, V> args) {
        LettuceAssert.notEmpty(keys, "strings or keys must be not empty");

        args.add(algorithm.name());
        args.add(by.name());
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
