package io.lettuce.core.failover;

/**
 * Options for multi-database client behavior.
 * <p>
 * This class contains configuration options that apply at the multi-database level, such as failback behavior. Individual
 * database configurations are managed separately via {@link DatabaseConfig}.
 *
 * @author Lettuce Contributors
 */
public class MultiDbOptions {

    private final boolean failbackSupported;

    private final long failbackCheckInterval;

    private final long gracePeriod;

    private MultiDbOptions(Builder builder) {
        this.failbackSupported = builder.failbackSupported;
        this.failbackCheckInterval = builder.failbackCheckInterval;
        this.gracePeriod = builder.gracePeriod;
    }

    /**
     * Returns whether automatic failback to higher-priority databases is supported.
     *
     * @return {@code true} if failback is supported
     */
    public boolean isFailbackSupported() {
        return failbackSupported;
    }

    /**
     * Returns the interval in milliseconds for checking if failed databases have recovered.
     *
     * @return the failback check interval in milliseconds
     */
    public long getFailbackCheckInterval() {
        return failbackCheckInterval;
    }

    /**
     * Returns the grace period duration that starts when a database is failed over from. During this grace period, no failbacks
     * or failovers can trigger a switch back to that database until the duration ends.
     *
     * @return the grace period duration, or {@code null} if no grace period is configured
     */
    public long getGracePeriod() {
        return gracePeriod;
    }

    /**
     * Creates a new builder for {@link MultiDbOptions}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default {@link MultiDbOptions} with failback enabled and default check interval.
     *
     * @return a default options instance
     */
    public static MultiDbOptions create() {
        return builder().build();
    }

    /**
     * Builder for {@link MultiDbOptions}.
     */
    public static class Builder {

        /** Default interval in milliseconds for checking if failed databases have recovered. */
        private static final long FAILBACK_CHECK_INTERVAL_DEFAULT = 120000;

        /** Default grace period duration. */
        private static final long GRACE_PERIOD_DEFAULT = 30000;

        /** Whether automatic failback to higher-priority databases is supported. */
        private boolean failbackSupported = true;

        private long failbackCheckInterval = FAILBACK_CHECK_INTERVAL_DEFAULT;

        private long gracePeriod = GRACE_PERIOD_DEFAULT;

        private Builder() {
        }

        /**
         * Sets whether automatic failback to higher-priority databases is supported.
         *
         * @param failbackSupported {@code true} to enable failback
         * @return this builder
         */
        public Builder failbackSupported(boolean failbackSupported) {
            this.failbackSupported = failbackSupported;
            return this;
        }

        /**
         * Sets the interval in milliseconds for checking if failed databases have recovered.
         *
         * @param failbackCheckInterval the check interval in milliseconds
         * @return this builder
         */
        public Builder failbackCheckInterval(long failbackCheckInterval) {
            this.failbackCheckInterval = failbackCheckInterval;
            return this;
        }

        /**
         * Sets the grace period duration that starts when a database is failed over from. During this grace period, no
         * failbacks or failovers can trigger a switch back to that database until the duration ends.
         * <p>
         * Defaults to 30 seconds. Set to {@code null} to disable grace period.
         *
         * @param gracePeriod the grace period duration in milliseconds, or {@code null} to disable
         * @return this builder
         */
        public Builder gracePeriod(long gracePeriod) {
            this.gracePeriod = gracePeriod;
            return this;
        }

        /**
         * Builds a new {@link MultiDbOptions} instance.
         *
         * @return a new options instance
         */
        public MultiDbOptions build() {
            return new MultiDbOptions(this);
        }

    }

}
