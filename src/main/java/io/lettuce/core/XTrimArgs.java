/*
 * Copyright 2018-2021 the original author or authors.
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
 * Argument list builder for the Redis <a href="http://redis.io/commands/xadd">XTRIM</a> command. Static import the methods from
 * {@link Builder} and call the methods: {@code maxlen(…)} .
 * <p>
 * {@link XTrimArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author dengliming
 * @since 6.1
 */
public class XTrimArgs {

    private Long maxlen;

    private boolean approximateTrimming;

    private String minid;

    private Long limit;

    /**
     * Builder entry points for {@link XTrimArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link XTrimArgs} and setting {@literal MAXLEN}.
         *
         * @return new {@link XTrimArgs} with {@literal MAXLEN} set.
         * @see XTrimArgs#maxlen(long)
         */
        public static XTrimArgs maxlen(long count) {
            return new XTrimArgs().maxlen(count);
        }

        /**
         * Creates new {@link XTrimArgs} and setting {@literal MINID}.
         *
         * @return new {@link XTrimArgs} with {@literal MINID} set.
         * @see XTrimArgs#minid(String)
         */
        public static XTrimArgs minid(String minid) {
            return new XTrimArgs().minid(minid);
        }
    }

    /**
     * Limit stream to {@code maxlen} entries.
     *
     * @param maxlen number greater 0.
     * @return {@code this}
     */
    public XTrimArgs maxlen(long maxlen) {

        LettuceAssert.isTrue(maxlen > 0, "Maxlen must be greater 0");

        this.maxlen = maxlen;
        return this;
    }

    /**
     * Limit stream to {@code maxlen} entries.
     *
     * @param minid
     * @return {@code this}
     */
    public XTrimArgs minid(String minid) {

        LettuceAssert.notNull(minid, "Minid must not be null");

        this.minid = minid;
        return this;
    }

    /**
     * The maximum number of entries to trim.
     *
     * @param limit has meaning only if `~` was provided.
     * @return {@code this}
     */
    public XTrimArgs limit(Long limit) {

        LettuceAssert.isTrue(limit > 0, "Limit must be greater 0");

        this.limit = limit;
        return this;
    }

    /**
     * Apply efficient trimming for capped streams using the {@code ~} flag.
     *
     * @return {@code this}
     */
    public XTrimArgs approximateTrimming() {
        return approximateTrimming(true);
    }

    /**
     * Apply efficient trimming for capped streams using the {@code ~} flag.
     *
     * @param approximateTrimming {@code true} to apply efficient radix node trimming.
     * @return {@code this}
     */
    public XTrimArgs approximateTrimming(boolean approximateTrimming) {

        this.approximateTrimming = approximateTrimming;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (maxlen != null) {
            args.add(CommandKeyword.MAXLEN);

            if (approximateTrimming) {
                args.add("~");
            }

            args.add(maxlen);
        }

        if (minid != null) {

            args.add(CommandKeyword.MINID);

            if (approximateTrimming) {
                args.add("~");
            }

            args.add(minid);
        }

        if (limit != null && approximateTrimming) {
            args.add(CommandKeyword.LIMIT).add(limit);
        }
    }

}
