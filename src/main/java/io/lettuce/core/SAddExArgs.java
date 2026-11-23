/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import java.time.Duration;
import java.time.Instant;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

import static io.lettuce.core.protocol.CommandKeyword.EX;
import static io.lettuce.core.protocol.CommandKeyword.EXAT;
import static io.lettuce.core.protocol.CommandKeyword.KEEPTTL;
import static io.lettuce.core.protocol.CommandKeyword.MNX;
import static io.lettuce.core.protocol.CommandKeyword.MXX;
import static io.lettuce.core.protocol.CommandKeyword.PX;
import static io.lettuce.core.protocol.CommandKeyword.PXAT;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/saddex/">SADDEX</a> command starting from
 * Redis 8.12. Static import the methods from {@link Builder} and chain the method calls:
 * {@code ex(Duration.ofSeconds(10)).mnx()}.
 * <p>
 * {@code MNX} and {@code MXX} are mutually exclusive, as are {@code EX}, {@code PX}, {@code EXAT}, {@code PXAT} and
 * {@code KEEPTTL}. Conflicting tokens are sent as given and rejected by the server.
 * <p>
 * {@link SAddExArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @since 8.0
 */
public class SAddExArgs implements CompositeArgument {

    private Long ex;

    private Long exAt;

    private Long px;

    private Long pxAt;

    private boolean keepttl = false;

    private boolean mnx = false;

    private boolean mxx = false;

    /**
     * Builder entry points for {@link SAddExArgs}.
     *
     * @since 8.0
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link SAddExArgs} and enable {@literal EX}.
         *
         * @param timeout expire time as duration, must not be {@code null}.
         * @return new {@link SAddExArgs} with {@literal EX} enabled.
         * @see SAddExArgs#ex(Duration)
         * @since 8.0
         */
        public static SAddExArgs ex(Duration timeout) {
            return new SAddExArgs().ex(timeout);
        }

        /**
         * Creates new {@link SAddExArgs} and enable {@literal PX}.
         *
         * @param timeout expire time as duration, must not be {@code null}.
         * @return new {@link SAddExArgs} with {@literal PX} enabled.
         * @see SAddExArgs#px(Duration)
         * @since 8.0
         */
        public static SAddExArgs px(Duration timeout) {
            return new SAddExArgs().px(timeout);
        }

        /**
         * Creates new {@link SAddExArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the point in time at which the members expire, must not be {@code null}.
         * @return new {@link SAddExArgs} with {@literal EXAT} enabled.
         * @see SAddExArgs#exAt(Instant)
         * @since 8.0
         */
        public static SAddExArgs exAt(Instant timestamp) {
            return new SAddExArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link SAddExArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the point in time at which the members expire, must not be {@code null}.
         * @return new {@link SAddExArgs} with {@literal PXAT} enabled.
         * @see SAddExArgs#pxAt(Instant)
         * @since 8.0
         */
        public static SAddExArgs pxAt(Instant timestamp) {
            return new SAddExArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link SAddExArgs} and enable {@literal KEEPTTL}.
         *
         * @return new {@link SAddExArgs} with {@literal KEEPTTL} enabled.
         * @see SAddExArgs#keepttl()
         * @since 8.0
         */
        public static SAddExArgs keepttl() {
            return new SAddExArgs().keepttl();
        }

        /**
         * Creates new {@link SAddExArgs} and enable {@literal MNX}.
         *
         * @return new {@link SAddExArgs} with {@literal MNX} enabled.
         * @see SAddExArgs#mnx()
         * @since 8.0
         */
        public static SAddExArgs mnx() {
            return new SAddExArgs().mnx();
        }

        /**
         * Creates new {@link SAddExArgs} and enable {@literal MXX}.
         *
         * @return new {@link SAddExArgs} with {@literal MXX} enabled.
         * @see SAddExArgs#mxx()
         * @since 8.0
         */
        public static SAddExArgs mxx() {
            return new SAddExArgs().mxx();
        }

    }

    /**
     * Set the expiration of the members, in seconds ({@literal EX}). The {@link Duration} is truncated to whole seconds; a
     * value below one second is sent as {@code 0}, which the server treats as an expiration in the past and deletes the
     * members.
     *
     * @param timeout expire time as duration, must not be {@code null}.
     * @return {@code this} {@link SAddExArgs}.
     * @since 8.0
     */
    public SAddExArgs ex(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.ex = timeout.getSeconds();
        return this;
    }

    /**
     * Set the expiration of the members, in milliseconds ({@literal PX}).
     *
     * @param timeout expire time as duration, must not be {@code null}.
     * @return {@code this} {@link SAddExArgs}.
     * @since 8.0
     */
    public SAddExArgs px(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.px = timeout.toMillis();
        return this;
    }

    /**
     * Set the point in time at which the members expire, as a Unix timestamp in seconds ({@literal EXAT}). The {@link Instant}
     * is truncated to whole seconds.
     *
     * @param timestamp the point in time at which the members expire, must not be {@code null}.
     * @return {@code this} {@link SAddExArgs}.
     * @since 8.0
     */
    public SAddExArgs exAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        this.exAt = timestamp.getEpochSecond();
        return this;
    }

    /**
     * Set the point in time at which the members expire, as a Unix timestamp in milliseconds ({@literal PXAT}).
     *
     * @param timestamp the point in time at which the members expire, must not be {@code null}.
     * @return {@code this} {@link SAddExArgs}.
     * @since 8.0
     */
    public SAddExArgs pxAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        this.pxAt = timestamp.toEpochMilli();
        return this;
    }

    /**
     * Retain the expiration a member already has ({@literal KEEPTTL}). Without it, and without an expiration option, an
     * existing member loses its expiration.
     *
     * @return {@code this} {@link SAddExArgs}.
     * @since 8.0
     */
    public SAddExArgs keepttl() {

        this.keepttl = true;
        return this;
    }

    /**
     * Only add the members if none of them already exists ({@literal MNX}). The condition applies to the whole command.
     *
     * @return {@code this} {@link SAddExArgs}.
     * @since 8.0
     */
    public SAddExArgs mnx() {

        this.mnx = true;
        return this;
    }

    /**
     * Only apply the command if all of the members already exist ({@literal MXX}). The condition applies to the whole command;
     * on a missing key nothing is created.
     *
     * @return {@code this} {@link SAddExArgs}.
     * @since 8.0
     */
    public SAddExArgs mxx() {

        this.mxx = true;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (mnx) {
            args.add(MNX);
        }

        if (mxx) {
            args.add(MXX);
        }

        if (ex != null) {
            args.add(EX).add(ex);
        }

        if (px != null) {
            args.add(PX).add(px);
        }

        if (exAt != null) {
            args.add(EXAT).add(exAt);
        }

        if (pxAt != null) {
            args.add(PXAT).add(pxAt);
        }

        if (keepttl) {
            args.add(KEEPTTL);
        }
    }

}
