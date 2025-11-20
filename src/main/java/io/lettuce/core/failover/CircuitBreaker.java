package io.lettuce.core.failover;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import io.lettuce.core.failover.metrics.CircuitBreakerMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.failover.api.CircuitBreakerStateListener;
import io.lettuce.core.failover.metrics.CircuitBreakerMetricsFactory;

/**
 * Circuit breaker for tracking command metrics and managing circuit breaker state. Wraps CircuitBreakerMetrics and exposes it
 * via {@link #getMetrics()}.
 *
 * @author Ali Takavci
 * @since 7.1
 */
public class CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    private final CircuitBreakerMetrics metrics;

    private final CircuitBreakerConfig config;

    private volatile State currentState = State.CLOSED;

    private Predicate<Throwable> exceptionsPredicate;

    private final Set<CircuitBreakerStateListener> listeners = ConcurrentHashMap.newKeySet();

    /**
     * Create a circuit breaker instance.
     */
    public CircuitBreaker(CircuitBreakerConfig config) {
        this.metrics = CircuitBreakerMetricsFactory.createLockFree();
        this.config = config;
        this.exceptionsPredicate = createExceptionsPredicate(config.trackedExceptions);
    }

    /**
     * Get the metrics tracked by this circuit breaker.
     *
     * @return the circuit breaker metrics
     */
    public CircuitBreakerMetrics getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "CircuitBreaker{" + "metrics=" + metrics + ", config=" + config + '}';
    }

    public boolean isCircuitBreakerTrackedException(Throwable error) {
        return exceptionsPredicate.test(error);
    }

    private static Predicate<Throwable> createExceptionsPredicate(Set<Class<? extends Throwable>> trackedExceptions) {
        return throwable -> {
            Class<? extends Throwable> errorClass = throwable.getClass();
            for (Class<? extends Throwable> trackedException : trackedExceptions) {
                if (trackedException.isAssignableFrom(errorClass)) {
                    return true;
                }
            }
            return false;
        };
    }

    public void evaluateMetrics() {
        boolean evaluationResult = metrics.getSnapshot().getFailureRate() >= config.getFailureRateThreshold()
                && metrics.getSnapshot().getFailureCount() >= config.getMinimumNumberOfFailures();
        if (evaluationResult) {
            stateTransitionTo(State.OPEN);
        }
    }

    private void stateTransitionTo(State newState) {
        State previousState = this.currentState;
        if (previousState != newState) {
            this.currentState = newState;
            fireStateChanged(previousState, newState);
        }
    }

    public State getCurrentState() {
        return currentState;
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

    public static enum State {
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
