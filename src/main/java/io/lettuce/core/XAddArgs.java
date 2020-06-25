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
 * Argument list builder for the Redis <a href="http://redis.io/commands/xadd">XADD</a> command. Static import the methods from
 * {@link Builder} and call the methods: {@code maxlen(…)} .
 * <p>
 * {@link XAddArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class XAddArgs {

    private String id;

    private Long maxlen;

    private boolean approximateTrimming;

    /**
     * Builder entry points for {@link XAddArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link XAddArgs} and setting {@literal MAXLEN}.
         *
         * @return new {@link XAddArgs} with {@literal MAXLEN} set.
         * @see XAddArgs#maxlen(long)
         */
        public static XAddArgs maxlen(long count) {
            return new XAddArgs().maxlen(count);
        }

    }

    /**
     * Limit results to {@code maxlen} entries.
     *
     * @param id must not be {@code null}.
     * @return {@code this}.
     */
    public XAddArgs id(String id) {

        LettuceAssert.notNull(id, "Id must not be null");

        this.id = id;
        return this;
    }

    /**
     * Limit stream to {@code maxlen} entries.
     *
     * @param maxlen number greater 0.
     * @return {@code this}.
     */
    public XAddArgs maxlen(long maxlen) {

        LettuceAssert.isTrue(maxlen > 0, "Maxlen must be greater 0");

        this.maxlen = maxlen;
        return this;
    }

    /**
     * Apply efficient trimming for capped streams using the {@code ~} flag.
     *
     * @return {@code this}.
     */
    public XAddArgs approximateTrimming() {
        return approximateTrimming(true);
    }

    /**
     * Apply efficient trimming for capped streams using the {@code ~} flag.
     *
     * @param approximateTrimming {@code true} to apply efficient radix node trimming.
     * @return {@code this}.
     */
    public XAddArgs approximateTrimming(boolean approximateTrimming) {

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

        if (id != null) {
            args.add(id);
        } else {
            args.add("*");
        }
    }

}
