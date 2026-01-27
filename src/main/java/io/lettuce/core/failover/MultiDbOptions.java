package io.lettuce.core.failover;

import io.lettuce.core.internal.LettuceAssert;

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

    private MultiDbOptions(Builder builder) {
        this.failbackSupported = builder.failbackSupported;
        this.failbackCheckInterval = builder.failbackCheckInterval;
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

        /** Whether automatic failback to higher-priority databases is supported. */
        private boolean failbackSupported = true;

        private long failbackCheckInterval = FAILBACK_CHECK_INTERVAL_DEFAULT;

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
            LettuceAssert.isTrue(failbackCheckInterval > 0,
                    "failbackCheckInterval must be greater than 0, got: " + failbackCheckInterval);
            this.failbackCheckInterval = failbackCheckInterval;
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
