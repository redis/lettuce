/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static io.lettuce.core.protocol.CommandKeyword.FNX;
import static io.lettuce.core.protocol.CommandKeyword.FXX;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/set">HSETEX</a> command starting from Redis 8.0.
 * Static import the methods from {@link Builder} and chain the method calls: {@code ex(10).nx()}.
 * <p>
 * {@link HSetExArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Ivo Gaydajiev
 * @since 6.6
 */
public class HSetExArgs implements CompositeArgument {

    private Long ex;

    private Long exAt;

    private Long px;

    private Long pxAt;

    private boolean fnx = false;

    private boolean fxx = false;

    private boolean keepttl = false;

    /**
     * Builder entry points for {@link HSetExArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link HSetExArgs} and enable {@literal EX}.
         *
         * @param timeout expire time as duration.
         * @return new {@link HSetExArgs} with {@literal EX} enabled.
         * @see HSetExArgs#ex(long)
         * @since 6.6
         */
        public static HSetExArgs ex(Duration timeout) {
            return new HSetExArgs().ex(timeout);
        }

        /**
         * Creates new {@link HSetExArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the timestamp type: posix time in seconds.
         * @return new {@link HSetExArgs} with {@literal EXAT} enabled.
         * @see HSetExArgs#exAt(Instant)
         * @since 6.6
         */
        public static HSetExArgs exAt(Instant timestamp) {
            return new HSetExArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link HSetExArgs} and enable {@literal PX}.
         *
         * @param timeout expire time in milliseconds.
         * @return new {@link HSetExArgs} with {@literal PX} enabled.
         * @see HSetExArgs#px(long)
         * @since 6.6
         */
        public static HSetExArgs px(Duration timeout) {
            return new HSetExArgs().px(timeout);
        }

        /**
         * Creates new {@link HSetExArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the timestamp type: posix time.
         * @return new {@link HSetExArgs} with {@literal PXAT} enabled.
         * @see HSetExArgs#pxAt(Instant)
         * @since 6.6
         */
        public static HSetExArgs pxAt(Instant timestamp) {
            return new HSetExArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link HSetExArgs} and enable {@literal NX}.
         *
         * @return new {@link HSetExArgs} with {@literal NX} enabled.
         * @see HSetExArgs#fnx()
         */
        public static HSetExArgs fnx() {
            return new HSetExArgs().fnx();
        }

        /**
         * Creates new {@link HSetExArgs} and enable {@literal XX}.
         *
         * @return new {@link HSetExArgs} with {@literal XX} enabled.
         * @see HSetExArgs#fxx()
         */
        public static HSetExArgs xx() {
            return new HSetExArgs().fxx();
        }

        /**
         * Creates new {@link HSetExArgs} and enable {@literal KEEPTTL}.
         *
         * @return new {@link HSetExArgs} with {@literal KEEPTTL} enabled.
         * @see HSetExArgs#keepttl()
         * @since 6.6
         */
        public static HSetExArgs keepttl() {
            return new HSetExArgs().keepttl();
        }

    }

    /**
     * Set the specified expire time, in seconds.
     *
     * @param timeout expire time in seconds.
     * @return {@code this} {@link HSetExArgs}.
     * @since 6.6
     */
    public HSetExArgs ex(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.ex = timeout.getSeconds();
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return {@code this} {@link HSetExArgs}.
     * @since 6.6
     */
    public HSetExArgs exAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        this.exAt = timestamp.getEpochSecond();
        return this;
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param timeout expire time in milliseconds.
     * @return {@code this} {@link HSetExArgs}.
     */
    public HSetExArgs px(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.px = timeout.toMillis();
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in milliseconds.
     * @return {@code this} {@link HSetExArgs}.
     * @since 6.6
     */
    public HSetExArgs pxAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        this.pxAt = timestamp.toEpochMilli();
        return this;
    }

    /**
     * Only set the key if it does not already exist.
     *
     * @return {@code this} {@link HSetExArgs}.
     */
    public HSetExArgs fnx() {

        this.fnx = true;
        return this;
    }

    /**
     * Set the value and retain the existing TTL.
     *
     * @return {@code this} {@link HSetExArgs}.
     * @since 6.6
     */
    public HSetExArgs keepttl() {

        this.keepttl = true;
        return this;
    }

    /**
     * Only set the key if it already exists.
     *
     * @return {@code this} {@link HSetExArgs}.
     */
    public HSetExArgs fxx() {

        this.fxx = true;
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

        if (fnx) {
            args.add(FNX);
        }

        if (fxx) {
            args.add(FXX);
        }

        if (keepttl) {
            args.add("KEEPTTL");
        }
    }

}
