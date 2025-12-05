package io.lettuce.core.failover;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import io.lettuce.core.internal.LettuceAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.failover.api.CircuitBreakerStateListener;
import io.lettuce.core.failover.metrics.CircuitBreakerMetrics;
import io.lettuce.core.failover.metrics.MetricsFactory;
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
public class CircuitBreaker implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    private final CircuitBreakerConfig config;

    private final AtomicReference<CircuitBreakerStateHolder> stateRef;

    private final Set<CircuitBreakerStateListener> listeners = ConcurrentHashMap.newKeySet();

    private final Set<Class<? extends Throwable>> trackedExceptions;

    /**
     * Create a circuit breaker instance.
     */
    public CircuitBreaker(CircuitBreakerConfig config) {
        LettuceAssert.notNull(config, "CircuitBreakerConfig must not be null");

        this.config = config;
        this.trackedExceptions = new HashSet<>(config.trackedExceptions);
        this.stateRef = new AtomicReference<>(
                new CircuitBreakerStateHolder(State.CLOSED, MetricsFactory.createDefaultMetrics()));
    }

    /**
     * Get the metrics tracked by this circuit breaker.
     * <p>
     * This is only for internal use and testing purposes.
     *
     * @return the circuit breaker metrics
     */
    CircuitBreakerMetrics getMetrics() {
        return stateRef.get().metrics;
    }

    /**
     * Get a snapshot of the current metrics within the time window. Use the snapshot to access success count, failure count,
     * total count, and failure rate.
     *
     * @return an immutable snapshot of current metrics
     */
    public MetricsSnapshot getSnapshot() {
        return stateRef.get().metrics.getSnapshot();
    }

    @Override
    public String toString() {
        CircuitBreakerStateHolder current = stateRef.get();
        return "CircuitBreaker{" + "state=" + current.state + ", metrics=" + current.metrics + ", config=" + config + '}';
    }

    public boolean isCircuitBreakerTrackedException(Throwable throwable) {
        Class<? extends Throwable> errorClass = throwable.getClass();
        for (Class<? extends Throwable> trackedException : trackedExceptions) {
            if (trackedException.isAssignableFrom(errorClass)) {
                return true;
            }
        }
        return false;
    }

    public void recordResult(Throwable error) {
        if (error != null && isCircuitBreakerTrackedException(error)) {
            recordFailure();
        } else {
            recordSuccess();
        }
    }

    public void recordFailure() {
        stateRef.get().metrics.recordFailure();
        evaluateMetrics();
    }

    public void recordSuccess() {
        stateRef.get().metrics.recordSuccess();
    }

    /**
     * Evaluate the current metrics to determine if the circuit breaker should transition to a new state.
     *
     * <p>
     * This method checks the failure rate and failure count against the configured thresholds. If the thresholds are met, the
     * circuit breaker transitions to the OPEN state. Metrics are reset when the state changes.
     * </p>
     *
     * @return an immutable snapshot of current metrics
     */
    MetricsSnapshot evaluateMetrics() {
        CircuitBreakerStateHolder current = stateRef.get();
        MetricsSnapshot snapshot = current.metrics.getSnapshot();
        boolean evaluationResult = snapshot.getFailureRate() >= config.getFailureRateThreshold()
                && snapshot.getFailureCount() >= config.getMinimumNumberOfFailures();
        if (evaluationResult) {
            stateTransitionTo(State.OPEN);
        }
        return snapshot;
    }

    /**
     * Switch the circuit breaker to the specified state. This method is used to force the circuit breaker to a specific state.
     *
     * <p>
     * This method does not evaluate the metrics to determine if the state transition is valid. It simply transitions to the
     * specified state. Metrics are reset when the state changes.
     * </p>
     *
     * @param newState the target state
     */
    public void transitionTo(State newState) {
        stateTransitionTo(newState);
    }

    /**
     * Atomically transition to a new state with fresh metrics.
     * <p>
     * This method uses lock-free CAS to ensure that state change and metrics reset happen atomically. Whenever the circuit
     * breaker transitions to a new state, a fresh metrics instance is created, providing a clean slate for tracking metrics in
     * the new state.
     * <p>
     * If the state is already the target state, no transition occurs.
     *
     * @param newState the target state
     */
    private void stateTransitionTo(State newState) {
        while (true) {
            CircuitBreakerStateHolder current = stateRef.get();

            // No transition needed if already in target state
            if (current.state == newState) {
                return;
            }

            // Always create fresh metrics on state transition
            CircuitBreakerMetrics nextMetrics = MetricsFactory.createDefaultMetrics();

            CircuitBreakerStateHolder next = new CircuitBreakerStateHolder(newState, nextMetrics);

            // Atomically swap if current state hasn't changed
            if (stateRef.compareAndSet(current, next)) {
                fireStateChanged(current.state, newState);
                return;
            }
            // CAS failed, retry with updated current state
        }
    }

    public State getCurrentState() {
        return stateRef.get().state;
    }

    /**
     * Add a listener for circuit breaker state change events.
     *
     * @param listener the listener to add, must not be {@code null}
     */
    public void addListener(CircuitBreakerStateListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener for circuit breaker state change events.
     *
     * @param listener the listener to remove, must not be {@code null}
     */
    public void removeListener(CircuitBreakerStateListener listener) {
        listeners.remove(listener);
    }

    /**
     * Fire a state change event to all registered listeners.
     *
     * @param previousState the previous state
     * @param newState the new state
     */
    private void fireStateChanged(State previousState, State newState) {
        CircuitBreakerStateChangeEvent event = new CircuitBreakerStateChangeEvent(this, previousState, newState);
        for (CircuitBreakerStateListener listener : listeners) {
            try {
                listener.onCircuitBreakerStateChange(event);
            } catch (Exception e) {
                // Ignore listener exceptions to prevent one bad listener from affecting others
                log.error("Error notifying listener " + listener + " of state change " + event, e);
            }
        }
    }

    @Override
    public void close() {
        listeners.clear();
    }

    public enum State {
        CLOSED, OPEN
    }

    /**
     * Immutable holder for circuit breaker state and metrics.
     * <p>
     * This class enables atomic updates of both state and metrics using {@link AtomicReference}. When a state transition
     * occurs, a new instance is created with fresh metrics.
     */
    private static final class CircuitBreakerStateHolder {

        final State state;

        final CircuitBreakerMetrics metrics;

        CircuitBreakerStateHolder(State state, CircuitBreakerMetrics metrics) {
            this.state = state;
            this.metrics = metrics;
        }

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
