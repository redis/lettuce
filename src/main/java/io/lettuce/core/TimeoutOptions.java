package io.lettuce.core;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Options for command timeouts. These options configure how and whether commands time out once they were dispatched. Command
 * timeout begins:
 * <ul>
 * <li>When the command is sent successfully to the transport</li>
 * <li>Queued while the connection was inactive</li>
 * </ul>
 *
 * The timeout is canceled upon command completion/cancellation. Timeouts are not tied to a specific API and expire commands
 * regardless of the synchronization method provided by the API that was used to enqueue the command.
 *
 * @author Mark Paluch
 * @since 5.1
 */
@SuppressWarnings("serial")
public class TimeoutOptions implements Serializable {

    public static final Duration DISABLED_TIMEOUT = Duration.ZERO.minusSeconds(1);

    public static final boolean DEFAULT_TIMEOUT_COMMANDS = false;

    public static final Duration DEFAULT_RELAXED_TIMEOUT = DISABLED_TIMEOUT;

    private final boolean timeoutCommands;

    private final boolean applyConnectionTimeout;

    private final Duration relaxedTimeout;

    private final TimeoutSource source;

    private TimeoutOptions(boolean timeoutCommands, boolean applyConnectionTimeout, TimeoutSource source,
            Duration relaxedTimeout) {

        this.timeoutCommands = timeoutCommands;
        this.applyConnectionTimeout = applyConnectionTimeout;
        this.relaxedTimeout = relaxedTimeout;
        this.source = source;
    }

    /**
     * Returns a new {@link TimeoutOptions.Builder} to construct {@link TimeoutOptions}.
     *
     * @return a new {@link TimeoutOptions.Builder} to construct {@link TimeoutOptions}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new instance of {@link TimeoutOptions} with default settings.
     *
     * @return a new instance of {@link TimeoutOptions} with default settings.
     */
    public static TimeoutOptions create() {
        return builder().build();
    }

    /**
     * Create a new instance of {@link TimeoutOptions} with enabled timeout applying default connection timeouts.
     *
     * @return a new instance of {@link TimeoutOptions} with enabled timeout applying default connection timeouts.
     */
    public static TimeoutOptions enabled() {
        return builder().timeoutCommands().connectionTimeout().build();
    }

    /**
     * Create a new instance of {@link TimeoutOptions} with enabled timeout applying a fixed {@link Duration timeout}.
     *
     * @return a new instance of {@link TimeoutOptions} with enabled timeout applying a fixed {@link Duration timeout}.
     */
    public static TimeoutOptions enabled(Duration timeout) {
        return builder().timeoutCommands().fixedTimeout(timeout).build();
    }

    /**
     * Builder for {@link TimeoutOptions}.
     */
    public static class Builder {

        private boolean timeoutCommands = DEFAULT_TIMEOUT_COMMANDS;

        private boolean applyConnectionTimeout = false;

        private Duration relaxedTimeout = DEFAULT_RELAXED_TIMEOUT;

        private TimeoutSource source;

        /**
         * Enable command timeouts. Disabled by default, see {@link #DEFAULT_TIMEOUT_COMMANDS}.
         *
         * @return {@code this}
         */
        public Builder timeoutCommands() {
            return timeoutCommands(true);
        }

        /**
         * Configure whether commands should timeout. Disabled by default, see {@link #DEFAULT_TIMEOUT_COMMANDS}.
         *
         * @param enabled {@code true} to enable timeout; {@code false} to disable timeouts.
         * @return {@code this}
         */
        public Builder timeoutCommands(boolean enabled) {

            this.timeoutCommands = enabled;
            return this;
        }

        /**
         * Enable timeout relaxing during maintenance events. Disabled by default, see {@link #DEFAULT_RELAXED_TIMEOUT}.
         * <p/>
         * If the Redis server supports sending maintenance events, and the client is set up to use that by the
         * {@link ClientOptions#supportsMaintenanceEvents()} option, the client would listen to notifications that the current
         * endpoint is about to go down (as part of some maintenance activity, for example). In such cases, the driver could
         * extend the existing timeout settings for newly issued commands, or such that are in flight, to make sure they do not
         * time out during this process. These commands could be either a part of the offline buffer or waiting for a reply.
         *
         * @param duration {@link Duration} to relax timeouts proactively, must not be {@code null}.
         * @return {@code this}
         * @since 7.0
         * @see ClientOptions#supportsMaintenanceEvents()
         */
        public Builder timeoutsRelaxingDuringMaintenance(Duration duration) {
            LettuceAssert.notNull(duration, "Duration must not be null");

            this.relaxedTimeout = duration;
            return this;
        }

        /**
         * Set a fixed timeout for all commands.
         *
         * @param duration the timeout {@link Duration}, must not be {@code null}.
         * @return {@code this}
         */
        public Builder fixedTimeout(Duration duration) {

            LettuceAssert.notNull(duration, "Duration must not be null");

            return timeoutSource(new FixedTimeoutSource(duration.toNanos(), TimeUnit.NANOSECONDS));
        }

        /**
         * Configure a {@link TimeoutSource} that applies timeouts configured on the connection/client instance.
         *
         * @return {@code this}
         */
        public Builder connectionTimeout() {
            return timeoutSource(new DefaultTimeoutSource());
        }

        /**
         * Set a {@link TimeoutSource} to obtain the timeout value per {@link RedisCommand}.
         *
         * @param source the timeout source.
         * @return {@code this}
         */
        public Builder timeoutSource(TimeoutSource source) {

            LettuceAssert.notNull(source, "TimeoutSource must not be null");

            timeoutCommands(true);
            this.applyConnectionTimeout = source instanceof DefaultTimeoutSource;
            this.source = source;
            return this;
        }

        /**
         * Create a new instance of {@link TimeoutOptions}.
         *
         * @return new instance of {@link TimeoutOptions}
         */
        public TimeoutOptions build() {

            if (timeoutCommands) {
                if (source == null) {
                    throw new IllegalStateException("TimeoutSource is required for enabled timeouts");
                }
            }

            return new TimeoutOptions(timeoutCommands, applyConnectionTimeout, source, relaxedTimeout);
        }

    }

    /**
     * @return {@code true} if commands should time out.
     */
    public boolean isTimeoutCommands() {
        return timeoutCommands;
    }

    /**
     * @return {@code true} to apply connection timeouts declared on connection level.
     */
    public boolean isApplyConnectionTimeout() {
        return applyConnectionTimeout;
    }

    /**
     * @return the {@link Duration} to relax timeouts proactively, {@link #DISABLED_TIMEOUT} if disabled.
     * @since 7.0
     */
    public Duration getRelaxedTimeout() {
        return relaxedTimeout;
    }

    /**
     * @return the timeout source to determine the timeout for a {@link RedisCommand}. Can be {@code null} if
     *         {@link #isTimeoutCommands()} is {@code false}.
     */
    public TimeoutSource getSource() {
        return source;
    }

    private static class DefaultTimeoutSource extends TimeoutSource {

        private final long timeout = -1;

        @Override
        public long getTimeout(RedisCommand<?, ?, ?> command) {
            return timeout;
        }

    }

    private static class FixedTimeoutSource extends TimeoutSource {

        private final long timeout;

        private final TimeUnit timeUnit;

        FixedTimeoutSource(long timeout, TimeUnit timeUnit) {

            this.timeout = timeout;
            this.timeUnit = timeUnit;
        }

        @Override
        public long getTimeout(RedisCommand<?, ?, ?> command) {
            return timeout;
        }

        @Override
        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

    }

    /**
     * Source for the actual timeout to expire a particular {@link RedisCommand}.
     */
    public static abstract class TimeoutSource {

        /**
         * Obtains the timeout for a {@link RedisCommand}. All timeouts must be specified in {@link #getTimeUnit()}. Values
         * greater zero will timeout the command. Values less or equal to zero do not timeout the command.
         * <p>
         * {@code command} may be null if a timeout is required but the command is not yet known, e.g. when the timeout is
         * required but a connect did not finish yet.
         *
         * @param command can be {@code null}.
         * @return the timeout value. Zero disables the timeout. A value of {@code -1} applies the default timeout configured on
         *         the connection.
         */
        public abstract long getTimeout(RedisCommand<?, ?, ?> command);

        /**
         * @return the {@link TimeUnit} for the timeout.
         */
        public TimeUnit getTimeUnit() {
            return TimeUnit.MILLISECONDS;
        }

    }

}
