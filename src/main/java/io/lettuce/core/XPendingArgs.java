/*
 * Copyright 2021-2022 the original author or authors.
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
 * Argument list builder for the Redis <a href="https://redis.io/commands/xpending">XPENDING</a> command. Static import the
 * methods from {@link XPendingArgs.Builder} and call the methods: {@code block(…)} .
 * <p>
 * {@link XPendingArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author dengliming
 * @since 6.1
 */
public class XPendingArgs<K> implements CompositeArgument {

    private K group;

    private K consumer;

    private Range<String> range;

    private Limit limit;

    private Long idle;

    /**
     * Builder entry points for {@link XPendingArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Create a new {@link XPendingArgs} .
         *
         * @param consumer the consumer
         * @param range the range of message Id's
         * @param limit limit {@code COUNT}
         * @return a new {@link XPendingArgs} with {@link Range} and {@link Limit} applied.
         */
        public static <K> XPendingArgs<K> xpending(Consumer<K> consumer, Range<String> range, Limit limit) {
            return new XPendingArgs<K>().consumer(consumer).range(range).limit(limit);
        }

        /**
         * Create a new {@link XPendingArgs} .
         *
         * @param group the group
         * @param range the range of message Id's
         * @param limit limit {@code COUNT}
         * @return a new {@link XPendingArgs} with {@link Range} and {@link Limit} applied.
         * @since 6.1.9
         */
        public static <K> XPendingArgs<K> xpending(K group, Range<String> range, Limit limit) {
            return new XPendingArgs<K>().group(group).range(range).limit(limit);
        }

    }

    public XPendingArgs<K> range(Range<String> range) {

        LettuceAssert.notNull(range, "Range must not be null");

        this.range = range;
        return this;
    }

    public XPendingArgs<K> consumer(Consumer<K> consumer) {

        LettuceAssert.notNull(consumer, "Consumer must not be null");

        this.consumer = consumer.getName();
        return group(consumer.getGroup());
    }

    /**
     *
     * @param group
     * @return
     * @since 6.1.9
     */
    public XPendingArgs<K> group(K group) {

        LettuceAssert.notNull(group, "Group must not be null");

        this.group = group;
        return this;
    }

    public XPendingArgs<K> limit(Limit limit) {

        LettuceAssert.notNull(limit, "Limit must not be null");

        this.limit = limit;
        return this;
    }

    /**
     * Include only entries that are idle for {@link Duration}.
     *
     * @param timeout
     * @return {@code this} {@link XPendingArgs}.
     */
    public XPendingArgs<K> idle(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        return idle(timeout.toMillis());
    }

    /**
     * Include only entries that are idle for {@code milliseconds}.
     *
     * @param milliseconds
     * @return {@code this} {@link XPendingArgs}.
     */
    public XPendingArgs<K> idle(long milliseconds) {
        this.idle = milliseconds;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        args.addKey((K) group);

        if (idle != null) {
            args.add(CommandKeyword.IDLE).add(idle);
        }

        if (range.getLower().equals(Range.Boundary.unbounded())) {
            args.add("-");
        } else {
            args.add(range.getLower().getValue());
        }

        if (range.getUpper().equals(Range.Boundary.unbounded())) {
            args.add("+");
        } else {
            args.add(range.getUpper().getValue());
        }

        args.add(limit.isLimited() ? limit.getCount() : Long.MAX_VALUE);

        if (consumer != null) {
            args.addKey((K) consumer);
        }
    }

}
