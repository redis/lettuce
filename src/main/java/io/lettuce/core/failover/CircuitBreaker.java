package io.lettuce.core.failover;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.failover.api.CircuitBreakerStateListener;
import io.lettuce.core.failover.metrics.MetricsSnapshot;

/**
 * Circuit breaker for tracking command metrics and managing circuit breaker state. Wraps CircuitBreakerMetrics and exposes it
 * via {@link #getMetrics()}.
 * <p>
 * State transitions and metrics replacement are atomic and lock-free using {@link AtomicReference}. When the circuit breaker
 * transitions to a new state, a fresh metrics instance is created atomically.
 *
 * @author Ali Takavci
 * @since 7.1
 */
public interface CircuitBreaker extends Closeable {

    /**
     * Get a snapshot of the current metrics within the time window. Use the snapshot to access success count, failure count,
     * total count, and failure rate.
     *
     * @return an immutable snapshot of current metrics
     */
    MetricsSnapshot getSnapshot();

    /**
     * Get the current generation of the circuit breaker. This is used to track the state and metrics of the circuit breaker at
     * the time of command execution.
     *
     * @return the current generation of the circuit breaker
     */
    CircuitBreakerGeneration getGeneration();

    /**
     * Get the current state of the circuit breaker.
     *
     * @return the current state
     */
    State getCurrentState();

    /**
     * Check if the circuit breaker is in the closed state.
     *
     * @return {@code true} if the circuit breaker is in the closed state
     */
    boolean isClosed();

    /**
     * Add a listener for circuit breaker state change events.
     *
     * @param listener the listener to add, must not be {@code null}
     */
    void addListener(CircuitBreakerStateListener listener);

    /**
     * Remove a listener for circuit breaker state change events.
     *
     * @param listener the listener to remove, must not be {@code null}
     */
    void removeListener(CircuitBreakerStateListener listener);

    @Override
    void close();

    enum State {
        CLOSED, OPEN
    }

    class CircuitBreakerConfig {

        private final static float DEFAULT_FAILURE_RATE_THRESHOLD = 10;

        private final static int DEFAULT_MINIMUM_NUMBER_OF_FAILURES = 1000;

        private final static int DEFAULT_METRICS_WINDOW_SIZE = 2;

        private final static Set<Class<? extends Throwable>> DEFAULT_TRACKED_EXCEPTIONS = new HashSet<>(Arrays.asList(

                // Connection failures
                RedisConnectionException.class, // Connection establishment failures
                IOException.class, // Network I/O failures (includes ClosedChannelException)
                ConnectException.class, // Connection refused, etc.

                // Timeout failures
                RedisCommandTimeoutException.class, // Command execution timeout
                TimeoutException.class // Generic timeout

        ));

        public static final CircuitBreakerConfig DEFAULT = new CircuitBreakerConfig();

        private final Set<Class<? extends Throwable>> trackedExceptions;

        private final float failureThreshold;

        private final int minimumNumberOfFailures;

        private final int metricsWindowSize;

        private CircuitBreakerConfig() {
            this.trackedExceptions = DEFAULT_TRACKED_EXCEPTIONS;
            this.failureThreshold = DEFAULT_FAILURE_RATE_THRESHOLD;
            this.minimumNumberOfFailures = DEFAULT_MINIMUM_NUMBER_OF_FAILURES;
            this.metricsWindowSize = DEFAULT_METRICS_WINDOW_SIZE;
        }

        /**
         * Create a new circuit breaker configuration from a builder. Use {@link #builder()} instead.
         *
         * @param builder the builder
         */
        CircuitBreakerConfig(Builder builder) {
            this.trackedExceptions = builder.trackedExceptions != null ? builder.trackedExceptions : DEFAULT_TRACKED_EXCEPTIONS;
            this.failureThreshold = builder.failureThreshold;
            this.minimumNumberOfFailures = builder.minimumNumberOfFailures;
            this.metricsWindowSize = builder.metricsWindowSize;
        }

        /**
         * Create a new builder for {@link CircuitBreakerConfig}.
         *
         * @return a new builder
         * @since 7.4
         */
        public static Builder builder() {
            return new Builder();
        }

        public Set<Class<? extends Throwable>> getTrackedExceptions() {
            return trackedExceptions;
        }

        public float getFailureRateThreshold() {
            return failureThreshold;
        }

        public int getMinimumNumberOfFailures() {
            return minimumNumberOfFailures;
        }

        public int getMetricsWindowSize() {
            return metricsWindowSize;
        }

        @Override
        public String toString() {
            return "CircuitBreakerConfig{" + "trackedExceptions=" + trackedExceptions + ", failureThreshold=" + failureThreshold
                    + ", minimumNumberOfFailures=" + minimumNumberOfFailures + ", metricsWindowSize=" + metricsWindowSize + '}';
        }

        /**
         * Builder for {@link CircuitBreakerConfig}.
         *
         * @since 7.4
         */
        public static class Builder {

            private Set<Class<? extends Throwable>> trackedExceptions;

            private float failureThreshold = DEFAULT_FAILURE_RATE_THRESHOLD;

            private int minimumNumberOfFailures = DEFAULT_MINIMUM_NUMBER_OF_FAILURES;

            private int metricsWindowSize = DEFAULT_METRICS_WINDOW_SIZE;

            private Builder() {
            }

            /**
             * Set the failure rate threshold percentage (0-100). The circuit breaker will open when the failure rate exceeds
             * this threshold.
             *
             * @param failureThreshold the failure rate threshold percentage, must be >= 0
             * @return {@code this} builder
             */
            public Builder failureRateThreshold(float failureThreshold) {
                this.failureThreshold = failureThreshold;
                return this;
            }

            /**
             * Set the minimum number of failures required before the circuit breaker can open. This prevents the circuit from
             * opening due to a small number of failures.
             *
             * @param minimumNumberOfFailures the minimum number of failures, must be >= 0
             * @return {@code this} builder
             */
            public Builder minimumNumberOfFailures(int minimumNumberOfFailures) {
                this.minimumNumberOfFailures = minimumNumberOfFailures;
                return this;
            }

            /**
             * Set the exceptions to track for circuit breaker metrics. Only these exceptions (and their subclasses) will be
             * counted as failures.
             *
             * @param trackedExceptions the set of exception classes to track, can be {@code null} to use defaults
             * @return {@code this} builder
             */
            public Builder trackedExceptions(Set<Class<? extends Throwable>> trackedExceptions) {
                this.trackedExceptions = trackedExceptions;
                return this;
            }

            /**
             * Set the metrics window size in seconds. Metrics are collected over this time window.
             *
             * @param metricsWindowSize the metrics window size in seconds, must be > 0
             * @return {@code this} builder
             */
            public Builder metricsWindowSize(int metricsWindowSize) {
                this.metricsWindowSize = metricsWindowSize;
                return this;
            }

            /**
             * Build a new {@link CircuitBreakerConfig} instance.
             *
             * @return a new {@link CircuitBreakerConfig}
             */
            public CircuitBreakerConfig build() {
                return new CircuitBreakerConfig(this);
            }

        }

    }

}
