package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/getex">HGETEX</a> command starting from Redis 8.0.
 * Static import the methods from {@link Builder} and chain the method calls: {@code ex(10).nx()}.
 * <p>
 * {@link HGetExArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Ivo Gaydajiev
 * @since 6.7.0
 */
public class HGetExArgs implements CompositeArgument {

    private Long ex;

    private Long exAt;

    private Long px;

    private Long pxAt;

    private boolean persist = false;

    /**
     * Builder entry points for {@link HGetExArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link HGetExArgs} and enable {@literal EX}.
         *
         * @param timeout expire time in seconds.
         * @return new {@link HGetExArgs} with {@literal EX} enabled.
         * @see HGetExArgs#ex(long)
         */
        public static HGetExArgs ex(long timeout) {
            return new HGetExArgs().ex(timeout);
        }

        /**
         * Creates new {@link HGetExArgs} and enable {@literal EX}.
         *
         * @param timeout expire time in seconds.
         * @return new {@link HGetExArgs} with {@literal EX} enabled.
         * @see HGetExArgs#ex(long)
         * @since 6.7.0
         */
        public static HGetExArgs ex(Duration timeout) {
            return new HGetExArgs().ex(timeout);
        }

        /**
         * Creates new {@link HGetExArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the timestamp type: posix time in seconds.
         * @return new {@link HGetExArgs} with {@literal EXAT} enabled.
         * @see HGetExArgs#exAt(long)
         */
        public static HGetExArgs exAt(long timestamp) {
            return new HGetExArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link HGetExArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the timestamp type: posix time in seconds.
         * @return new {@link HGetExArgs} with {@literal EXAT} enabled.
         * @see HGetExArgs#exAt(Date)
         * @since 6.7.0
         */
        public static HGetExArgs exAt(Date timestamp) {
            return new HGetExArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link HGetExArgs} and enable {@literal EXAT}.
         *
         * @param timestamp the timestamp type: posix time in seconds.
         * @return new {@link HGetExArgs} with {@literal EXAT} enabled.
         * @see HGetExArgs#exAt(Instant)
         * @since 6.7.0
         */
        public static HGetExArgs exAt(Instant timestamp) {
            return new HGetExArgs().exAt(timestamp);
        }

        /**
         * Creates new {@link HGetExArgs} and enable {@literal PX}.
         *
         * @param timeout expire time in milliseconds.
         * @return new {@link HGetExArgs} with {@literal PX} enabled.
         * @see HGetExArgs#px(long)
         */
        public static HGetExArgs px(long timeout) {
            return new HGetExArgs().px(timeout);
        }

        /**
         * Creates new {@link HGetExArgs} and enable {@literal PX}.
         *
         * @param timeout expire time in milliseconds.
         * @return new {@link HGetExArgs} with {@literal PX} enabled.
         * @see HGetExArgs#px(long)
         * @since 6.7.0
         */
        public static HGetExArgs px(Duration timeout) {
            return new HGetExArgs().px(timeout);
        }

        /**
         * Creates new {@link HGetExArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the timestamp type: posix time.
         * @return new {@link HGetExArgs} with {@literal PXAT} enabled.
         * @see HGetExArgs#pxAt(long)
         */
        public static HGetExArgs pxAt(long timestamp) {
            return new HGetExArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link HGetExArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the timestamp type: posix time.
         * @return new {@link HGetExArgs} with {@literal PXAT} enabled.
         * @see HGetExArgs#pxAt(Date)
         * @since 6.7.0
         */
        public static HGetExArgs pxAt(Date timestamp) {
            return new HGetExArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link HGetExArgs} and enable {@literal PXAT}.
         *
         * @param timestamp the timestamp type: posix time.
         * @return new {@link HGetExArgs} with {@literal PXAT} enabled.
         * @see HGetExArgs#pxAt(Instant)
         * @since 6.7.0
         */
        public static HGetExArgs pxAt(Instant timestamp) {
            return new HGetExArgs().pxAt(timestamp);
        }

        /**
         * Creates new {@link HGetExArgs} and enable {@literal PERSIST}.
         *
         * @return new {@link HGetExArgs} with {@literal PERSIST} enabled.
         * @see HGetExArgs#persist()
         */
        public static HGetExArgs persist() {
            return new HGetExArgs().persist();
        }

    }

    /**
     * Set the specified expire time, in seconds.
     *
     * @param timeout expire time in seconds.
     * @return {@code this} {@link HGetExArgs}.
     */
    public HGetExArgs ex(long timeout) {

        this.ex = timeout;
        return this;
    }

    /**
     * Set the specified expire time, in seconds.
     *
     * @param timeout expire time in seconds.
     * @return {@code this} {@link HGetExArgs}.
     * @since 6.7.0
     */
    public HGetExArgs ex(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.ex = timeout.toMillis() / 1000;
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return {@code this} {@link HGetExArgs}.
     * @since 6.7.0
     */
    public HGetExArgs exAt(long timestamp) {

        this.exAt = timestamp;
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return {@code this} {@link HGetExArgs}.
     * @since 6.7.0
     */
    public HGetExArgs exAt(Date timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return exAt(timestamp.getTime() / 1000);
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return {@code this} {@link HGetExArgs}.
     * @since 6.7.0
     */
    public HGetExArgs exAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return exAt(timestamp.toEpochMilli() / 1000);
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param timeout expire time in milliseconds.
     * @return {@code this} {@link HGetExArgs}.
     */
    public HGetExArgs px(long timeout) {

        this.px = timeout;
        return this;
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param timeout expire time in milliseconds.
     * @return {@code this} {@link HGetExArgs}.
     */
    public HGetExArgs px(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");

        this.px = timeout.toMillis();
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in milliseconds.
     * @return {@code this} {@link HGetExArgs}.
     * @since 6.7.0
     */
    public HGetExArgs pxAt(long timestamp) {

        this.pxAt = timestamp;
        return this;
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in milliseconds.
     * @return {@code this} {@link HGetExArgs}.
     * @since 6.7.0
     */
    public HGetExArgs pxAt(Date timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return pxAt(timestamp.getTime());
    }

    /**
     * Set the specified expire at time using a posix {@code timestamp}.
     *
     * @param timestamp the timestamp type: posix time in milliseconds.
     * @return {@code this} {@link HGetExArgs}.
     * @since 6.7.0
     */
    public HGetExArgs pxAt(Instant timestamp) {

        LettuceAssert.notNull(timestamp, "Timestamp must not be null");

        return pxAt(timestamp.toEpochMilli());
    }

    /**
     * Remove the time to live associated with the key.
     *
     * @return {@code this} {@link HGetExArgs}.
     */
    public HGetExArgs persist() {

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
