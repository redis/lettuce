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
    public MetricsSnapshot getSnapshot();

    /**
     * Get the current generation of the circuit breaker. This is used to track the state and metrics of the circuit breaker at
     * the time of command execution.
     *
     * @return the current generation of the circuit breaker
     */
    public CircuitBreakerGeneration getGeneration();

    /**
     * Record the result of a command execution. This is used to track the success/failure of commands for metrics and circuit
     * breaker state evaluation.
     *
     * @param error the error, if any
     */
    public void recordResult(Throwable error);

    /**
     * Record a failure. This is used to track the failure of commands for metrics and circuit breaker state evaluation.
     */
    public void recordFailure();

    /**
     * Record a success. This is used to track the success of commands for metrics and circuit breaker state evaluation.
     */
    public void recordSuccess();

    /**
     * Get the current state of the circuit breaker.
     *
     * @return the current state
     */
    public State getCurrentState();

    /**
     * Check if the circuit breaker is in the closed state.
     *
     * @return {@code true} if the circuit breaker is in the closed state
     */
    public boolean isClosed();

    /**
     * Add a listener for circuit breaker state change events.
     *
     * @param listener the listener to add, must not be {@code null}
     */
    public void addListener(CircuitBreakerStateListener listener);

    /**
     * Remove a listener for circuit breaker state change events.
     *
     * @param listener the listener to remove, must not be {@code null}
     */
    public void removeListener(CircuitBreakerStateListener listener);

    @Override
    public void close();

    public enum State {
        CLOSED, OPEN
    }

    public static class CircuitBreakerConfig {

        private final static float DEFAULT_FAILURE_RATE_THRESHOLD = 10;

        private final static int DEFAULT_MINIMUM_NUMBER_OF_FAILURES = 1000;

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

        private CircuitBreakerConfig() {
            this(DEFAULT_FAILURE_RATE_THRESHOLD, DEFAULT_MINIMUM_NUMBER_OF_FAILURES, DEFAULT_TRACKED_EXCEPTIONS);
        }

        public CircuitBreakerConfig(float failureThreshold, int minimumNumberOfFailures,
                Set<Class<? extends Throwable>> trackedExceptions) {
            this.trackedExceptions = trackedExceptions;
            this.failureThreshold = failureThreshold;
            this.minimumNumberOfFailures = minimumNumberOfFailures;
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

    }

}
