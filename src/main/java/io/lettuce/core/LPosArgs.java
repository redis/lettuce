/*
 * Copyright 2018-2020 the original author or authors.
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
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/lpos">LPOS</a> command. Static import the methods from
 * {@link Builder} and call the methods: {@code maxlen(…)} .
 * <p>
 * {@link LPosArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 5.3.2
 */
public class LPosArgs implements CompositeArgument {

    private Long first;

    private Long maxlen;

    /**
     * Builder entry points for {@link LPosArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new empty {@link LPosArgs}.
         *
         * @return new {@link LPosArgs}.
         * @see LPosArgs#maxlen(long)
         */
        public static LPosArgs empty() {
            return new LPosArgs();
        }

        /**
         * Creates new {@link LPosArgs} and setting {@literal MAXLEN}.
         *
         * @return new {@link LPosArgs} with {@literal MAXLEN} set.
         * @see LPosArgs#maxlen(long)
         */
        public static LPosArgs maxlen(long count) {
            return new LPosArgs().maxlen(count);
        }

        /**
         * Creates new {@link LPosArgs} and setting {@literal FIRST}.
         *
         * @return new {@link LPosArgs} with {@literal FIRST} set.
         * @see LPosArgs#maxlen(long)
         */
        public static LPosArgs first(long rank) {
            return new LPosArgs().first(rank);
        }

    }

    /**
     * Limit list scanning to {@code maxlen} entries.
     *
     * @param maxlen number greater 0.
     * @return {@code this}
     */
    public LPosArgs maxlen(long maxlen) {

        LettuceAssert.isTrue(maxlen > 0, "Maxlen must be greater 0");

        this.maxlen = maxlen;
        return this;
    }

    /**
     * Specify the rank of the first element to return, in case there are multiple matches
     *
     * @param rank number.
     * @return {@code this}
     */
    public LPosArgs first(long rank) {

        this.first = rank;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (maxlen != null) {

            args.add(CommandKeyword.MAXLEN);
            args.add(maxlen);
        }

        if (first != null) {
            args.add("FIRST");
            args.add(first);
        }
    }

}
