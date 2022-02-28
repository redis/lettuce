/*
 * Copyright 2011-2022 the original author or authors.
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

import static io.lettuce.core.protocol.CommandKeyword.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/set">SET</a> command starting from Redis 2.6.12. Static
 * import the methods from {@link Builder} and chain the method calls: {@code ex(10).nx()}.
 * <p>
 * {@link SetArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Will Glozer
 * @author Vincent Rischmann
 * @author Mark Paluch
 */
public class SetArgs implements CompositeArgument {

    private Long ex;

    private Long exAt;

    private Long px;

    private Long pxAt;

    private boolean nx = false;

    private boolean xx = false;

    private boolean keepttl = false;

    /**
     * Builder entry points for {@link SetArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal EX}.
         *
         * @param timeout expire time in seconds.
         * @return new {@link SetArgs} with {@literal EX} enabled.
         * @see SetArgs#ex(long)
         */
        public static SetArgs ex(long timeout) {
            return new SetArgs().ex(timeout);
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal EX}.
         *
         * @param timeout expire time in seconds.
         * @return new {@link SetArgs} with {@literal EX} enabled.
         * @see SetArgs#ex(long)
         * @since 6.1
         */
        public static SetArgs ex(Duration timeout) {
            return new SetArgs().ex(timeout);
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the timestamp type: posix time in seconds.
         * @return new {@link SetArgs} with {@literal EXAT} enabled.
         * @see SetArgs#exAt(long)
         */
        public static SetArgs exAt(long timestamp) {
            return new SetArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the timestamp type: posix time in seconds.
         * @return new {@link SetArgs} with {@literal EXAT} enabled.
         * @see SetArgs#exAt(Date)
         * @since 6.1
         */
        public static SetArgs exAt(Date timestamp) {
            return new SetArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the timestamp type: posix time in seconds.
         * @return new {@link SetArgs} with {@literal EXAT} enabled.
         * @see SetArgs#exAt(Instant)
         * @since 6.1
         */
        public static SetArgs exAt(Instant timestamp) {
            return new SetArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal PX}.
         *
         * @param timeout expire time in milliseconds.
         * @return new {@link SetArgs} with {@literal PX} enabled.
         * @see SetArgs#px(long)
         */
        public static SetArgs px(long timeout) {
            return new SetArgs().px(timeout);
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal PX}.
         *
         * @param timeout expire time in milliseconds.
         * @return new {@link SetArgs} with {@literal PX} enabled.
         * @see SetArgs#px(long)
         * @since 6.1
         */
        public static SetArgs px(Duration timeout) {
            return new SetArgs().px(timeout);
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the timestamp type: posix time.
         * @return new {@link SetArgs} with {@literal PXAT} enabled.
         * @see SetArgs#pxAt(long)
         */
        public static SetArgs pxAt(long timestamp) {
            return new SetArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the timestamp type: posix time.
         * @return new {@link SetArgs} with {@literal PXAT} enabled.
         * @see SetArgs#pxAt(Date)
         * @since 6.1
         */
        public static SetArgs pxAt(Date timestamp) {
            return new SetArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the timestamp type: posix time.
         * @return new {@link SetArgs} with {@literal PXAT} enabled.
         * @see SetArgs#pxAt(Instant)
         * @since 6.1
         */
        public static SetArgs pxAt(Instant timestamp) {
            return new SetArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal NX}.
         *
         * @return new {@link SetArgs} with {@literal NX} enabled.
         * @see SetArgs#nx()
         */
        public static SetArgs nx() {
            return new SetArgs().nx();
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal XX}.
         *
         * @return new {@link SetArgs} with {@literal XX} enabled.
         * @see SetArgs#xx()
         */
        public static SetArgs xx() {
            return new SetArgs().xx();
        }

        /**
         * Creates new {@link SetArgs} and enable {@literal KEEPTTL}.
         *
         * @return new {@link SetArgs} with {@literal KEEPTTL} enabled.
         * @see SetArgs#keepttl()
         * @since 5.3
         */
        public static SetArgs keepttl() {
            return new SetArgs().keepttl();
        }

    }

    /**
     * Set the specified expire time, in seconds.
     *
     * @param timeout expire time in seconds.
     * @return {@code this} {@link SetArgs}.
     */
    public SetArgs ex(long timeout) {

        this.ex = timeout;
        return this;
    }

    /**
     * Set the specified expire time, in seconds.
     *
     * @param timeout expire time in seconds.
     * @return {@code this} {@link SetArgs}.
     * @since 6.1
     */
    public SetArgs ex(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.ex = timeout.toMillis() / 1000;
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return {@code this} {@link SetArgs}.
     * @since 6.1
     */
    public SetArgs exAt(long timestamp) {

        this.exAt = timestamp;
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return {@code this} {@link SetArgs}.
     * @since 6.1
     */
    public SetArgs exAt(Date timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return exAt(timestamp.getTime() / 1000);
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return {@code this} {@link SetArgs}.
     * @since 6.1
     */
    public SetArgs exAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return exAt(timestamp.toEpochMilli() / 1000);
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param timeout expire time in milliseconds.
     * @return {@code this} {@link SetArgs}.
     */
    public SetArgs px(long timeout) {

        this.px = timeout;
        return this;
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param timeout expire time in milliseconds.
     * @return {@code this} {@link SetArgs}.
     */
    public SetArgs px(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.px = timeout.toMillis();
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in milliseconds.
     * @return {@code this} {@link SetArgs}.
     * @since 6.1
     */
    public SetArgs pxAt(long timestamp) {

        this.pxAt = timestamp;
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in milliseconds.
     * @return {@code this} {@link SetArgs}.
     * @since 6.1
     */
    public SetArgs pxAt(Date timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return pxAt(timestamp.getTime());
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in milliseconds.
     * @return {@code this} {@link SetArgs}.
     * @since 6.1
     */
    public SetArgs pxAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return pxAt(timestamp.toEpochMilli());
    }

    /**
     * Only set the key if it does not already exist.
     *
     * @return {@code this} {@link SetArgs}.
     */
    public SetArgs nx() {

        this.nx = true;
        return this;
    }

    /**
     * Set the value and retain the existing TTL.
     *
     * @return {@code this} {@link SetArgs}.
     * @since 5.3
     */
    public SetArgs keepttl() {

        this.keepttl = true;
        return this;
    }

    /**
     * Only set the key if it already exists.
     *
     * @return {@code this} {@link SetArgs}.
     */
    public SetArgs xx() {

        this.xx = true;
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

        if (nx) {
            args.add(NX);
        }

        if (xx) {
            args.add(XX);
        }

        if (keepttl) {
            args.add("KEEPTTL");
        }
    }

}
