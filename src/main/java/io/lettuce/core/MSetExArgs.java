/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static io.lettuce.core.protocol.CommandKeyword.NX;
import static io.lettuce.core.protocol.CommandKeyword.XX;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/msetex">MSETEX</a> command starting from Redis 8.4
 * Static import the methods from {@link Builder} and chain the method calls: {@code ex(10).nx()}.
 * <p>
 * {@link MSetExArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Aleksandar Todorov
 * @since 7.1
 */
public class MSetExArgs implements CompositeArgument {

    private Long ex;

    private Long exAt;

    private Long px;

    private Long pxAt;

    private boolean nx = false;

    private boolean xx = false;

    private boolean keepttl = false;

    /**
     * Builder entry points for {@link MSetExArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link MSetExArgs} and enable {@literal EX}.
         *
         * @param timeout expire time as duration.
         * @return new {@link MSetExArgs} with {@literal EX} enabled.
         * @see MSetExArgs#ex(long)
         * @since 7.1
         */
        public static MSetExArgs ex(Duration timeout) {
            return new MSetExArgs().ex(timeout);
        }

        /**
         * Creates new {@link MSetExArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the timestamp type: posix time in seconds.
         * @return new {@link MSetExArgs} with {@literal EXAT} enabled.
         * @see MSetExArgs#exAt(Instant)
         * @since 7.1
         */
        public static MSetExArgs exAt(Instant timestamp) {
            return new MSetExArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link MSetExArgs} and enable {@literal PX}.
         *
         * @param timeout expire time in milliseconds.
         * @return new {@link MSetExArgs} with {@literal PX} enabled.
         * @see MSetExArgs#px(long)
         * @since 7.1
         */
        public static MSetExArgs px(Duration timeout) {
            return new MSetExArgs().px(timeout);
        }

        /**
         * Creates new {@link MSetExArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the timestamp type: posix time.
         * @return new {@link MSetExArgs} with {@literal PXAT} enabled.
         * @see MSetExArgs#pxAt(Instant)
         * @since 7.1
         */
        public static MSetExArgs pxAt(Instant timestamp) {
            return new MSetExArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link MSetExArgs} and enable {@literal NX}.
         *
         * @return new {@link MSetExArgs} with {@literal NX} enabled.
         * @see MSetExArgs#nx()
         * @since 7.1
         */
        public static MSetExArgs nx() {
            return new MSetExArgs().nx();
        }

        /**
         * Creates new {@link MSetExArgs} and enable {@literal XX}.
         *
         * @return new {@link MSetExArgs} with {@literal XX} enabled.
         * @see MSetExArgs#xx()
         */
        public static MSetExArgs xx() {
            return new MSetExArgs().xx();
        }

        /**
         * Creates new {@link MSetExArgs} and enable {@literal KEEPTTL}.
         *
         * @return new {@link MSetExArgs} with {@literal KEEPTTL} enabled.
         * @see MSetExArgs#keepttl()
         * @since 7.1
         */
        public static MSetExArgs keepttl() {
            return new MSetExArgs().keepttl();
        }

    }

    /**
     * Set the specified expire time, in seconds.
     *
     * @param timeout expire time in seconds.
     * @return {@code this} {@link MSetExArgs}.
     * @since 7.1
     */
    public MSetExArgs ex(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.ex = timeout.toMillis() / 1000;
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return {@code this} {@link MSetExArgs}.
     * @since 7.1
     */
    public MSetExArgs exAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        this.exAt = timestamp.toEpochMilli() / 1000;
        return this;
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param timeout expire time in milliseconds.
     * @return {@code this} {@link MSetExArgs}.
     * @since 7.1
     */
    public MSetExArgs px(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.px = timeout.toMillis();
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in milliseconds.
     * @return {@code this} {@link MSetExArgs}.
     * @since 7.1
     */
    public MSetExArgs pxAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        this.pxAt = timestamp.toEpochMilli();
        return this;
    }

    /**
     * Only set the key if it does not already exist.
     *
     * @return {@code this} {@link MSetExArgs}.
     * @since 7.1
     */
    public MSetExArgs nx() {

        this.nx = true;
        return this;
    }

    /**
     * Set the value and retain the existing TTL.
     *
     * @return {@code this} {@link MSetExArgs}.
     * @since 7.1
     */
    public MSetExArgs keepttl() {

        this.keepttl = true;
        return this;
    }

    /**
     * Only set the key if it already exists.
     *
     * @return {@code this} {@link MSetExArgs}.
     * @since 7.1
     */
    public MSetExArgs xx() {

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
