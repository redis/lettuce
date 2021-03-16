/*
 * Copyright 2021 the original author or authors.
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
 * Argument list builder for the Redis <a href="http://redis.io/commands/xautoclaim">XAUTOCLAIM</a> command. Static import the
 * methods from {@link XAutoClaimArgs.Builder} and call the methods: {@code xautoclaim(…)} .
 * <p>
 * {@link XAutoClaimArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author dengliming
 * @since 6.1
 */
public class XAutoClaimArgs<K> {

    private Consumer<K> consumer;

    private long minIdleTime;

    private String startId;

    private Long count;

    private boolean justid;

    /**
     * Builder entry points for {@link XAutoClaimArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link XAutoClaimArgs} and set the {@code JUSTID} flag to return just the message id and do not increment
         * the retry counter. The message body is not returned when calling {@code XAUTOCLAIM}.
         *
         * @param consumer
         * @param minIdleTime
         * @param startId
         * @param <K>
         * @return
         */
        public static <K> XAutoClaimArgs<K> justid(Consumer<K> consumer, long minIdleTime, String startId) {
            return new XAutoClaimArgs().justid().consumer(consumer).minIdleTime(minIdleTime).startId(startId);
        }

        /**
         * Creates new {@link XAutoClaimArgs}.
         *
         * @param consumer
         * @param minIdleTime
         * @param startId
         * @param <K>
         * @return
         */
        public static <K> XAutoClaimArgs<K> xautoclaim(Consumer<K> consumer, long minIdleTime, String startId) {
            return new XAutoClaimArgs().consumer(consumer).minIdleTime(minIdleTime).startId(startId);
        }

    }

    public XAutoClaimArgs<K> consumer(Consumer<K> consumer) {
        LettuceAssert.notNull(consumer, "Consumer must not be null");

        this.consumer = consumer;
        return this;
    }

    /**
     * The optional <count> argument, which defaults to 100, is the upper limit of the number of entries that the command
     * attempts to claim.
     *
     * @param count
     * @return {@code this}.
     */
    public XAutoClaimArgs count(long count) {

        this.count = count;
        return this;
    }

    /**
     * The optional JUSTID argument changes the reply to return just an array of IDs of messages successfully claimed, without
     * returning the actual message. Using this option means the retry counter is not incremented.
     *
     * @return {@code this}.
     */
    public XAutoClaimArgs justid() {

        this.justid = true;
        return this;
    }

    /**
     * Return only messages that are idle for at least {@code milliseconds}.
     *
     * @param milliseconds min idle time.
     * @return {@code this}.
     */
    public XAutoClaimArgs minIdleTime(long milliseconds) {

        this.minIdleTime = milliseconds;
        return this;
    }

    /**
     * Set the startId.
     *
     * @param startId
     * @return
     */
    public XAutoClaimArgs startId(String startId) {

        LettuceAssert.notNull(startId, "StartId must not be null");
        this.startId = startId;
        return this;
    }

    public boolean isJustid() {
        return justid;
    }

    public <V> void build(CommandArgs<K, V> args) {
        args.addKey(consumer.getGroup());
        args.addKey(consumer.getName());
        args.add(minIdleTime);
        args.add(startId);

        if (count != null) {
            args.add(CommandKeyword.COUNT).add(count);
        }

        if (justid) {
            args.add(CommandKeyword.JUSTID);
        }
    }
}
