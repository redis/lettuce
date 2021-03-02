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
 * Argument list builder for the Redis <a href="http://redis.io/commands/xpending">XPENDING</a> command.
 * Static import the methods from {@link XPendingArgs.Builder} and call the methods: {@code block(…)} .
 * <p>
 * {@link XPendingArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author dengliming
 * @since 6.1
 */
public class XPendingArgs<K> {

    private Consumer<K> consumer;

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
         * @param consumer
         * @param range
         * @param limit
         */
        public static <K> XPendingArgs xpending(Consumer<K> consumer, Range<String> range, Limit limit) {
            return new XPendingArgs().consumer(consumer).range(range).limit(limit);
        }
    }

    public XPendingArgs range(Range<String> range) {
        LettuceAssert.notNull(range, "Range must not be null");

        this.range = range;
        return this;
    }

    public XPendingArgs consumer(Consumer<K> consumer) {
        LettuceAssert.notNull(consumer, "Consumer must not be null");

        this.consumer = consumer;
        return this;
    }

    public XPendingArgs limit(Limit limit) {
        LettuceAssert.notNull(limit, "Limit must not be null");

        this.limit = limit;
        return this;
    }

    public XPendingArgs idle(Long idle) {
        this.idle = idle;
        return this;
    }

    public <V> void build(CommandArgs<K, V> args) {
        args.addKey(consumer.group);

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
        args.addKey(consumer.name);
    }
}
