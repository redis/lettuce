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

import java.time.Duration;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/xread">XREAD</a> and {@literal XREADGROUP} commands.
 * Static import the methods from {@link XReadArgs.Builder} and call the methods: {@code block(…)} .
 * <p>
 * {@link XReadArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class XReadArgs {

    private Long block;

    private Long count;

    private boolean noack;

    /**
     * Builder entry points for {@link XReadArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Create a new {@link XReadArgs} and set {@literal BLOCK}.
         *
         * @param milliseconds time to block.
         * @return new {@link XReadArgs} with {@literal BLOCK} set.
         * @see XReadArgs#block(long)
         */
        public static XReadArgs block(long milliseconds) {
            return new XReadArgs().block(milliseconds);
        }

        /**
         * Create a new {@link XReadArgs} and set {@literal BLOCK}.
         *
         * @param timeout time to block.
         * @return new {@link XReadArgs} with {@literal BLOCK} set.
         * @see XReadArgs#block(Duration)
         */
        public static XReadArgs block(Duration timeout) {

            LettuceAssert.notNull(timeout, "Block timeout must not be null");

            return block(timeout.toMillis());
        }

        /**
         * Create a new {@link XReadArgs} and set {@literal COUNT}.
         *
         * @param count
         * @return new {@link XReadArgs} with {@literal COUNT} set.
         */
        public static XReadArgs count(long count) {
            return new XReadArgs().count(count);
        }

        /**
         * Create a new {@link XReadArgs} and set {@literal NOACK}.
         *
         * @return new {@link XReadArgs} with {@literal NOACK} set.
         * @see XReadArgs#noack(boolean)
         */
        public static XReadArgs noack() {
            return noack(true);
        }

        /**
         * Create a new {@link XReadArgs} and set {@literal NOACK}.
         *
         * @param noack
         * @return new {@link XReadArgs} with {@literal NOACK} set.
         * @see XReadArgs#noack(boolean)
         */
        public static XReadArgs noack(boolean noack) {
            return new XReadArgs().noack(noack);
        }

    }

    /**
     * Perform a blocking read and wait up to {@code milliseconds} for a new stream message.
     *
     * @param milliseconds max time to wait.
     * @return {@code this}.
     */
    public XReadArgs block(long milliseconds) {

        this.block = milliseconds;
        return this;
    }

    /**
     * Perform a blocking read and wait up to a {@link Duration timeout} for a new stream message.
     *
     * @param timeout max time to wait.
     * @return {@code this}.
     */
    public XReadArgs block(Duration timeout) {

        LettuceAssert.notNull(timeout, "Block timeout must not be null");

        return block(timeout.toMillis());
    }

    /**
     * Limit read to {@code count} messages.
     *
     * @param count number of messages.
     * @return {@code this}.
     */
    public XReadArgs count(long count) {

        this.count = count;
        return this;
    }

    /**
     * Use NOACK option to disable auto-acknowledgement. Only valid for {@literal XREADGROUP}.
     *
     * @param noack {@code true} to disable auto-ack.
     * @return {@code this}.
     */
    public XReadArgs noack(boolean noack) {

        this.noack = noack;
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (block != null) {
            args.add(CommandKeyword.BLOCK).add(block);
        }

        if (count != null) {
            args.add(CommandKeyword.COUNT).add(count);
        }

        if (noack) {
            args.add(CommandKeyword.NOACK);
        }
    }

    /**
     * Value object representing a Stream with its offset.
     */
    public static class StreamOffset<K> {

        final K name;

        final String offset;

        private StreamOffset(K name, String offset) {
            this.name = name;
            this.offset = offset;
        }

        /**
         * Read all new arriving elements from the stream identified by {@code name}.
         *
         * @param name must not be {@code null}.
         * @return the {@link StreamOffset} object without a specific offset.
         */
        public static <K> StreamOffset<K> latest(K name) {

            LettuceAssert.notNull(name, "Stream must not be null");

            return new StreamOffset<>(name, "$");
        }

        /**
         * Read all new arriving elements from the stream identified by {@code name} with ids greater than the last one consumed
         * by the consumer group.
         *
         * @param name must not be {@code null}.
         * @return the {@link StreamOffset} object without a specific offset.
         */
        public static <K> StreamOffset<K> lastConsumed(K name) {

            LettuceAssert.notNull(name, "Stream must not be null");

            return new StreamOffset<>(name, ">");
        }

        /**
         * Read all arriving elements from the stream identified by {@code name} starting at {@code offset}.
         *
         * @param name must not be {@code null}.
         * @param offset the stream offset.
         * @return the {@link StreamOffset} object without a specific offset.
         */
        public static <K> StreamOffset<K> from(K name, String offset) {

            LettuceAssert.notNull(name, "Stream must not be null");
            LettuceAssert.notEmpty(offset, "Offset must not be empty");

            return new StreamOffset<>(name, offset);
        }

        public K getName() {
            return name;
        }

        public String getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return String.format("%s:%s", name, offset);
        }

    }

}
