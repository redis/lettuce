package io.lettuce.core.failover.api;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Configuration for circuit breaker behavior.
 * <p>
 * This class contains configuration options for circuit breaker thresholds, tracked exceptions, and metrics window size.
 * </p>
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public class CircuitBreakerConfig {

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

    public static final CircuitBreakerConfig DEFAULT = builder().build();

    private final Set<Class<? extends Throwable>> trackedExceptions;

    private final float failureThreshold;

    private final int minimumNumberOfFailures;

    private final int metricsWindowSize;

    /**
     * Create a new circuit breaker configuration from a builder. Use {@link #builder()} instead.
     *
     * @param builder the builder
     */
    CircuitBreakerConfig(Builder builder) {
        this.trackedExceptions = new HashSet<>(builder.trackedExceptions);
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

    /**
     * Get the set of exception classes that are tracked for circuit breaker metrics.
     *
     * @return the set of tracked exception classes
     */
    public Set<Class<? extends Throwable>> getTrackedExceptions() {
        return trackedExceptions;
    }

    /**
     * Get the failure rate threshold percentage (0-100).
     *
     * @return the failure rate threshold
     */
    public float getFailureRateThreshold() {
        return failureThreshold;
    }

    /**
     * Get the minimum number of failures required before the circuit breaker can open.
     *
     * @return the minimum number of failures
     */
    public int getMinimumNumberOfFailures() {
        return minimumNumberOfFailures;
    }

    /**
     * Get the metrics window size in seconds.
     *
     * @return the metrics window size
     */
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

        private Set<Class<? extends Throwable>> trackedExceptions = DEFAULT_TRACKED_EXCEPTIONS;

        private float failureThreshold = DEFAULT_FAILURE_RATE_THRESHOLD;

        private int minimumNumberOfFailures = DEFAULT_MINIMUM_NUMBER_OF_FAILURES;

        private int metricsWindowSize = DEFAULT_METRICS_WINDOW_SIZE;

        private Builder() {
        }

        /**
         * Set the failure rate threshold percentage (0-100). The circuit breaker will open when the failure rate exceeds this
         * threshold.
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
         * Set the exceptions to track for circuit breaker metrics. Only these exceptions (and their subclasses) will be counted
         * as failures. This replaces any previously configured tracked exceptions.
         *
         * @param trackedExceptions the set of exception classes to track, must not be {@code null}
         * @return {@code this} builder
         */
        public Builder trackedExceptions(Set<Class<? extends Throwable>> trackedExceptions) {
            LettuceAssert.notNull(trackedExceptions, "Tracked exceptions must not be null");
            this.trackedExceptions = trackedExceptions;
            return this;
        }

        /**
         * Add one or more exception classes to track for circuit breaker metrics. Only these exceptions (and their subclasses)
         * will be counted as failures. This adds to the existing tracked exceptions.
         *
         * @param exceptionClasses one or more exception classes to track, must not be {@code null} and must not contain
         *        {@code null} elements
         * @return {@code this} builder
         * @since 7.4
         */
        @SafeVarargs
        public final Builder addTrackedExceptions(Class<? extends Throwable>... exceptionClasses) {
            LettuceAssert.notNull(exceptionClasses, "Exception classes must not be null");
            LettuceAssert.noNullElements(exceptionClasses, "Exception classes must not contain null elements");
            LettuceAssert.notEmpty(exceptionClasses, "Exception classes must contain at least one element");

            // Ensure we have a mutable copy
            if (this.trackedExceptions == DEFAULT_TRACKED_EXCEPTIONS) {
                this.trackedExceptions = new HashSet<>(DEFAULT_TRACKED_EXCEPTIONS);
            }

            this.trackedExceptions.addAll(Arrays.asList(exceptionClasses));
            return this;
        }

        /**
         * Remove one or more exception classes from the tracked exceptions for circuit breaker metrics.
         *
         * @param exceptionClasses one or more exception classes to remove, must not be {@code null} and must not contain
         *        {@code null} elements
         * @return {@code this} builder
         * @since 7.4
         */
        @SafeVarargs
        public final Builder removeTrackedExceptions(Class<? extends Throwable>... exceptionClasses) {
            LettuceAssert.notNull(exceptionClasses, "Exception classes must not be null");
            LettuceAssert.noNullElements(exceptionClasses, "Exception classes must not contain null elements");
            LettuceAssert.notEmpty(exceptionClasses, "Exception classes must contain at least one element");

            // Ensure we have a mutable copy
            if (this.trackedExceptions == DEFAULT_TRACKED_EXCEPTIONS) {
                this.trackedExceptions = new HashSet<>(DEFAULT_TRACKED_EXCEPTIONS);
            }

            Arrays.asList(exceptionClasses).forEach(this.trackedExceptions::remove);
            return this;
        }

        /**
         * Set the metrics window size in seconds. Metrics are collected over this time window.
         *
         * @param metricsWindowSize the metrics window size in seconds, must be >= 2
         * @return {@code this} builder
         */
        public Builder metricsWindowSize(int metricsWindowSize) {
            LettuceAssert.isTrue(metricsWindowSize >= 2, "Metrics window size must be at least 2 seconds");
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

