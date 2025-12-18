package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import java.time.Duration;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/xread">XREAD</a> and {@literal XREADGROUP} commands.
 * Static import the methods from {@link XReadArgs.Builder} and call the methods: {@code block(…)} .
 * <p>
 * {@link XReadArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public class XReadArgs implements CompositeArgument {

    private Long block;

    private Long count;

    private boolean noack;

    private Long claimMinIdleTime;

    /**
     * Builder entry points for {@link XReadArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Create a new {@link XReadArgs} and set {@literal BLOCK}.
         *
         * @param milliseconds time to block.
         * @return new {@link XReadArgs} with {@literal BLOCK} set.
         * @see XReadArgs#block(long)
         */
        public static XReadArgs block(long milliseconds) {
            return new XReadArgs().block(milliseconds);
        }

        /**
         * Create a new {@link XReadArgs} and set {@literal BLOCK}.
         *
         * @param timeout time to block.
         * @return new {@link XReadArgs} with {@literal BLOCK} set.
         * @see XReadArgs#block(Duration)
         */
        public static XReadArgs block(Duration timeout) {

            LettuceAssert.notNull(timeout, "Block timeout must not be null");

            return block(timeout.toMillis());
        }

        /**
         * Create a new {@link XReadArgs} and set {@literal COUNT}.
         *
         * @param count
         * @return new {@link XReadArgs} with {@literal COUNT} set.
         */
        public static XReadArgs count(long count) {
            return new XReadArgs().count(count);
        }

        /**
         * Create a new {@link XReadArgs} and set {@literal NOACK}.
         *
         * @return new {@link XReadArgs} with {@literal NOACK} set.
         * @see XReadArgs#noack(boolean)
         */
        public static XReadArgs noack() {
            return noack(true);
        }

        /**
         * Create a new {@link XReadArgs} and set {@literal NOACK}.
         *
         * @param noack
         * @return new {@link XReadArgs} with {@literal NOACK} set.
         * @see XReadArgs#noack(boolean)
         */
        public static XReadArgs noack(boolean noack) {
            return new XReadArgs().noack(noack);
        }

        /**
         * Create a new {@link XReadArgs} and set CLAIM min-idle-time.
         * 
         * @implNote Only valid for XREADGROUP.
         * @param milliseconds minimum idle time.
         * @return new {@link XReadArgs} with CLAIM set
         * @since 7.1
         */
        public static XReadArgs claim(long milliseconds) {
            return new XReadArgs().claim(milliseconds);
        }

        /**
         * Create a new {@link XReadArgs} and set CLAIM min-idle-time.
         * 
         * @implNote Only valid for XREADGROUP.
         * @param timeout minimum idle time.
         * @return new {@link XReadArgs} with CLAIM set
         * @since 7.1
         */
        public static XReadArgs claim(Duration timeout) {
            LettuceAssert.notNull(timeout, "Claim timeout must not be null");
            return claim(timeout.toMillis());
        }

    }

    /**
     * Perform a blocking read and wait up to {@code milliseconds} for a new stream message.
     *
     * @param milliseconds max time to wait.
     * @return {@code this}.
     */
    public XReadArgs block(long milliseconds) {

        this.block = milliseconds;
        return this;
    }

    /**
     * Perform a blocking read and wait up to a {@link Duration timeout} for a new stream message.
     *
     * @param timeout max time to wait.
     * @return {@code this}.
     */
    public XReadArgs block(Duration timeout) {

        LettuceAssert.notNull(timeout, "Block timeout must not be null");

        return block(timeout.toMillis());
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
     * Use NOACK option to disable auto-acknowledgement. Only valid for {@literal XREADGROUP}.
     *
     * @param noack {@code true} to disable auto-ack.
     * @return {@code this}.
     */
    public XReadArgs noack(boolean noack) {

        this.noack = noack;
        return this;
    }

    /**
     * Claim idle pending messages first with a minimum idle time (milliseconds).
     * 
     * @implNote Only valid for XREADGROUP.
     * @param milliseconds minimum idle time.
     * @return {@code this}.
     * @since 7.1
     */
    public XReadArgs claim(long milliseconds) {

        this.claimMinIdleTime = milliseconds;
        return this;
    }

    /**
     * Claim idle pending messages first with a minimum idle time (milliseconds).
     * 
     * @implNote Only valid for XREADGROUP.
     * @param timeout minimum idle time.
     * @return {@code this}.
     * @since 7.1
     */
    public XReadArgs claim(Duration timeout) {

        LettuceAssert.notNull(timeout, "Claim timeout must not be null");

        return claim(timeout.toMillis());
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

        if (claimMinIdleTime != null) {
            args.add(CommandKeyword.CLAIM).add(claimMinIdleTime);
        }
    }

    /**
     * Value object representing a Stream with its offset.
     */
    public static class StreamOffset<K> {

        final K name;

        final String offset;

        private StreamOffset(K name, String offset) {
            this.name = name;
            this.offset = offset;
        }

        /**
         * Read all new arriving elements from the stream identified by {@code name} excluding any elements before this call
         *
         * @param name must not be {@code null}.
         * @return the {@link StreamOffset} object without a specific offset.
         */
        public static <K> StreamOffset<K> latest(K name) {

            LettuceAssert.notNull(name, "Stream must not be null");

            return new StreamOffset<>(name, "$");
        }

        /**
         * Read all new arriving elements from the stream identified by {@code name} including the last element added before
         * this call
         *
         * @param name must not be {@code null}.
         * @return the {@link StreamOffset} object without a specific offset.
         * @since 6.4
         */
        public static <K> StreamOffset<K> last(K name) {

            LettuceAssert.notNull(name, "Stream must not be null");

            return new StreamOffset<>(name, "+");
        }

        /**
         * Read all new arriving elements from the stream identified by {@code name} with ids greater than the last one consumed
         * by the consumer group.
         *
         * @param name must not be {@code null}.
         * @return the {@link StreamOffset} object without a specific offset.
         */
        public static <K> StreamOffset<K> lastConsumed(K name) {

            LettuceAssert.notNull(name, "Stream must not be null");

            return new StreamOffset<>(name, ">");
        }

        /**
         * Read all arriving elements from the stream identified by {@code name} starting at {@code offset}.
         *
         * @param name must not be {@code null}.
         * @param offset the stream offset.
         * @return the {@link StreamOffset} object without a specific offset.
         */
        public static <K> StreamOffset<K> from(K name, String offset) {

            LettuceAssert.notNull(name, "Stream must not be null");
            LettuceAssert.notEmpty(offset, "Offset must not be empty");

            return new StreamOffset<>(name, offset);
        }

        public K getName() {
            return name;
        }

        public String getOffset() {
            return offset;
        }

        @Override
        public String toString() {
            return String.format("%s:%s", name, offset);
        }

    }

}
