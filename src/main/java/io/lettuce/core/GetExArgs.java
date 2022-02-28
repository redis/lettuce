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
import java.time.Instant;
import java.util.Date;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/getex">GETEX</a> command starting from Redis 6.2.
 * Static import the methods from {@link Builder} and chain the method calls: {@code ex(10).nx()}.
 * <p>
 * {@link GetExArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class GetExArgs implements CompositeArgument {

    private Long ex;

    private Long exAt;

    private Long px;

    private Long pxAt;

    private boolean persist = false;

    /**
     * Builder entry points for {@link GetExArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link GetExArgs} and enable {@literal EX}.
         *
         * @param timeout expire time in seconds.
         * @return new {@link GetExArgs} with {@literal EX} enabled.
         * @see GetExArgs#ex(long)
         */
        public static GetExArgs ex(long timeout) {
            return new GetExArgs().ex(timeout);
        }

        /**
         * Creates new {@link GetExArgs} and enable {@literal EX}.
         *
         * @param timeout expire time in seconds.
         * @return new {@link GetExArgs} with {@literal EX} enabled.
         * @see GetExArgs#ex(long)
         * @since 6.1
         */
        public static GetExArgs ex(Duration timeout) {
            return new GetExArgs().ex(timeout);
        }

        /**
         * Creates new {@link GetExArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the timestamp type: posix time in seconds.
         * @return new {@link GetExArgs} with {@literal EXAT} enabled.
         * @see GetExArgs#exAt(long)
         */
        public static GetExArgs exAt(long timestamp) {
            return new GetExArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link GetExArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the timestamp type: posix time in seconds.
         * @return new {@link GetExArgs} with {@literal EXAT} enabled.
         * @see GetExArgs#exAt(Date)
         * @since 6.1
         */
        public static GetExArgs exAt(Date timestamp) {
            return new GetExArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link GetExArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the timestamp type: posix time in seconds.
         * @return new {@link GetExArgs} with {@literal EXAT} enabled.
         * @see GetExArgs#exAt(Instant)
         * @since 6.1
         */
        public static GetExArgs exAt(Instant timestamp) {
            return new GetExArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link GetExArgs} and enable {@literal PX}.
         *
         * @param timeout expire time in milliseconds.
         * @return new {@link GetExArgs} with {@literal PX} enabled.
         * @see GetExArgs#px(long)
         */
        public static GetExArgs px(long timeout) {
            return new GetExArgs().px(timeout);
        }

        /**
         * Creates new {@link GetExArgs} and enable {@literal PX}.
         *
         * @param timeout expire time in milliseconds.
         * @return new {@link GetExArgs} with {@literal PX} enabled.
         * @see GetExArgs#px(long)
         * @since 6.1
         */
        public static GetExArgs px(Duration timeout) {
            return new GetExArgs().px(timeout);
        }

        /**
         * Creates new {@link GetExArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the timestamp type: posix time.
         * @return new {@link GetExArgs} with {@literal PXAT} enabled.
         * @see GetExArgs#pxAt(long)
         */
        public static GetExArgs pxAt(long timestamp) {
            return new GetExArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link GetExArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the timestamp type: posix time.
         * @return new {@link GetExArgs} with {@literal PXAT} enabled.
         * @see GetExArgs#pxAt(Date)
         * @since 6.1
         */
        public static GetExArgs pxAt(Date timestamp) {
            return new GetExArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link GetExArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the timestamp type: posix time.
         * @return new {@link GetExArgs} with {@literal PXAT} enabled.
         * @see GetExArgs#pxAt(Instant)
         * @since 6.1
         */
        public static GetExArgs pxAt(Instant timestamp) {
            return new GetExArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link GetExArgs} and enable {@literal PERSIST}.
         *
         * @return new {@link GetExArgs} with {@literal PERSIST} enabled.
         * @see GetExArgs#persist()
         */
        public static GetExArgs persist() {
            return new GetExArgs().persist();
        }

    }

    /**
     * Set the specified expire time, in seconds.
     *
     * @param timeout expire time in seconds.
     * @return {@code this} {@link GetExArgs}.
     */
    public GetExArgs ex(long timeout) {

        this.ex = timeout;
        return this;
    }

    /**
     * Set the specified expire time, in seconds.
     *
     * @param timeout expire time in seconds.
     * @return {@code this} {@link GetExArgs}.
     * @since 6.1
     */
    public GetExArgs ex(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.ex = timeout.toMillis() / 1000;
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return {@code this} {@link GetExArgs}.
     * @since 6.1
     */
    public GetExArgs exAt(long timestamp) {

        this.exAt = timestamp;
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return {@code this} {@link GetExArgs}.
     * @since 6.1
     */
    public GetExArgs exAt(Date timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return exAt(timestamp.getTime() / 1000);
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return {@code this} {@link GetExArgs}.
     * @since 6.1
     */
    public GetExArgs exAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return exAt(timestamp.toEpochMilli() / 1000);
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param timeout expire time in milliseconds.
     * @return {@code this} {@link GetExArgs}.
     */
    public GetExArgs px(long timeout) {

        this.px = timeout;
        return this;
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param timeout expire time in milliseconds.
     * @return {@code this} {@link GetExArgs}.
     */
    public GetExArgs px(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.px = timeout.toMillis();
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in milliseconds.
     * @return {@code this} {@link GetExArgs}.
     * @since 6.1
     */
    public GetExArgs pxAt(long timestamp) {

        this.pxAt = timestamp;
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in milliseconds.
     * @return {@code this} {@link GetExArgs}.
     * @since 6.1
     */
    public GetExArgs pxAt(Date timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return pxAt(timestamp.getTime());
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in milliseconds.
     * @return {@code this} {@link GetExArgs}.
     * @since 6.1
     */
    public GetExArgs pxAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return pxAt(timestamp.toEpochMilli());
    }

    /**
     * Remove the time to live associated with the key.
     *
     * @return {@code this} {@link GetExArgs}.
     */
    public GetExArgs persist() {

        this.persist = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (ex != null) {
            args.add("EX").add(ex);
        }

        if (exAt != null) {
            args.add("EXAT").add(exAt);
        }

        if (px != null) {
            args.add("PX").add(px);
        }

        if (pxAt != null) {
            args.add("PXAT").add(pxAt);
        }

        if (persist) {
            args.add(CommandType.PERSIST);
        }
    }

}
