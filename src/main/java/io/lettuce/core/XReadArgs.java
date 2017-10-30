/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;

/**
 * Args for the {@literal XREAD} command.
 *
 * @author Mark Paluch
 */
public class XReadArgs {

    private Long block;
    private Long count;
    private boolean noack;

    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {

        }

        public static XReadArgs block(long milliseconds) {
            return new XReadArgs().block(milliseconds);
        }

        public static XReadArgs count(long count) {
            return new XReadArgs().count(count);
        }

        public static XReadArgs noack(boolean noack) {
            return new XReadArgs().noack(noack);
        }
    }

    /**
     * Wait up to {@code milliseconds} for a new stream message.
     *
     * @param milliseconds max time to wait.
     * @return {@code this}.
     */
    public XReadArgs block(long milliseconds) {
        this.block = milliseconds;
        return this;
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
     * Use NOACK option to disable auto-acknowledgement.
     *
     * @param noack {@literal true} to disable auto-ack.
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
     * Value object representing a Stream consumer group.
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
         * @param name must not be {@literal null}.
         * @return the {@link StreamOffset} object without a specific offset.
         */
        public static <K> StreamOffset<K> latest(K name) {

            LettuceAssert.notNull(name, "Stream must not be null");

            return new StreamOffset<>(name, "$");
        }

        /**
         * Read all new arriving elements from the stream identified by {@code name}.
         *
         * @param name must not be {@literal null}.
         * @return the {@link StreamOffset} object without a specific offset.
         */
        public static <K> StreamOffset<K> latestConsumer(K name) {

            LettuceAssert.notNull(name, "Stream must not be null");

            return new StreamOffset<>(name, ">");
        }

        /**
         * Read all arriving elements from the stream identified by {@code name} starting at {@code offset}.
         *
         * @param name must not be {@literal null}.
         * @param offset the stream offset.
         * @return the {@link StreamOffset} object without a specific offset.
         */
        public static <K> StreamOffset<K> from(K name, String offset) {

            LettuceAssert.notNull(name, "Stream must not be null");
            LettuceAssert.notEmpty(offset, "Offset must not be empty");

            return new StreamOffset<>(name, offset);
        }
    }

}
