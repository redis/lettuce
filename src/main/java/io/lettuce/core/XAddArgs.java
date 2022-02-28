/*
 * Copyright 2018-2022 the original author or authors.
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
 * Argument list builder for the Redis <a href="https://redis.io/commands/xadd">XADD</a> command. Static import the methods from
 * {@link Builder} and call the methods: {@code maxlen(…)} .
 * <p>
 * {@link XAddArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @author dengliming
 * @since 5.1
 */
public class XAddArgs {

    private String id;

    private Long maxlen;

    private boolean approximateTrimming;

    private boolean exactTrimming;

    private boolean nomkstream;

    private String minid;

    private Long limit;

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

        /**
         * Creates new {@link XAddArgs} and setting {@literal NOMKSTREAM}.
         *
         * @return new {@link XAddArgs} with {@literal NOMKSTREAM} set.
         * @see XAddArgs#nomkstream()
         * @since 6.1
         */
        public static XAddArgs nomkstream() {
            return new XAddArgs().nomkstream();
        }

        /**
         * Creates new {@link XAddArgs} and setting {@literal MINID}.
         *
         * @param minid the oldest ID in the stream will be exactly the minimum between its original oldest ID and the specified
         *        threshold.
         * @return new {@link XAddArgs} with {@literal MINID} set.
         * @see XAddArgs#minId(String)
         * @since 6.1
         */
        public static XAddArgs minId(String minid) {
            return new XAddArgs().minId(minid);
        }
    }

    /**
     * Specify the message {@code id}.
     *
     * @param id must not be {@code null}.
     * @return {@code this}
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
     * @return {@code this}
     */
    public XAddArgs maxlen(long maxlen) {

        LettuceAssert.isTrue(maxlen > 0, "Maxlen must be greater 0");

        this.maxlen = maxlen;
        return this;
    }

    /**
     * Limit stream entries by message Id.
     *
     * @param minid the oldest ID in the stream will be exactly the minimum between its original oldest ID and the specified
     *        threshold.
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs minId(String minid) {

        LettuceAssert.notNull(minid, "minId must not be null");

        this.minid = minid;
        return this;
    }

    /**
     * The maximum number of entries to trim.
     *
     * @param limit has meaning only if {@link #approximateTrimming `~`} was set.
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs limit(long limit) {

        LettuceAssert.isTrue(limit > 0, "Limit must be greater 0");

        this.limit = limit;
        return this;
    }

    /**
     * Apply efficient trimming for capped streams using the {@code ~} flag.
     *
     * @return {@code this}
     */
    public XAddArgs approximateTrimming() {
        return approximateTrimming(true);
    }

    /**
     * Apply efficient trimming for capped streams using the {@code ~} flag.
     *
     * @param approximateTrimming {@code true} to apply efficient radix node trimming.
     * @return {@code this}
     */
    public XAddArgs approximateTrimming(boolean approximateTrimming) {

        this.approximateTrimming = approximateTrimming;
        return this;
    }

    /**
     * Apply exact trimming for capped streams using the {@code =} flag.
     *
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs exactTrimming() {
        return exactTrimming(true);
    }

    /**
     * Apply exact trimming for capped streams using the {@code =} flag.
     *
     * @param exactTrimming {@code true} to apply exact radix node trimming.
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs exactTrimming(boolean exactTrimming) {

        this.exactTrimming = exactTrimming;
        return this;
    }

    /**
     * Do add the message if the stream does not already exist.
     *
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs nomkstream() {
        return nomkstream(true);
    }

    /**
     * Do add the message if the stream does not already exist.
     *
     * @param nomkstream {@code true} to not create a stream if it does not already exist.
     * @return {@code this}
     * @since 6.1
     */
    public XAddArgs nomkstream(boolean nomkstream) {

        this.nomkstream = nomkstream;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (maxlen != null) {
            args.add(CommandKeyword.MAXLEN);

            if (approximateTrimming) {
                args.add("~");
            } else if (exactTrimming) {
                args.add("=");
            }

            args.add(maxlen);
        }

        if (minid != null) {

            args.add(CommandKeyword.MINID);

            if (approximateTrimming) {
                args.add("~");
            } else if (exactTrimming) {
                args.add("=");
            }

            args.add(minid);
        }

        if (limit != null && approximateTrimming) {
            args.add(CommandKeyword.LIMIT).add(limit);
        }

        if (nomkstream) {
            args.add(CommandKeyword.NOMKSTREAM);
        }

        if (id != null) {
            args.add(id);
        } else {
            args.add("*");
        }
    }

}
