package io.lettuce.core.failover;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import io.lettuce.core.internal.LettuceAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
class CircuitBreakerImpl implements CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    private final CircuitBreakerConfig config;

    private final AtomicReference<CircuitBreakerStateHolder> stateRef;

    private final Set<CircuitBreakerStateListener> listeners = ConcurrentHashMap.newKeySet();

    private final Set<Class<? extends Throwable>> trackedExceptions;

    private final MetricsFactory metricsFactory;

    /**
     * Create a circuit breaker instance.
     */
    public CircuitBreakerImpl(CircuitBreakerConfig config) {
        this(config, MetricsFactory.DEFAULT);
    }

    CircuitBreakerImpl(CircuitBreakerConfig config, MetricsFactory metricsFactory) {
        LettuceAssert.notNull(config, "CircuitBreakerConfig must not be null");

        this.config = config;
        this.trackedExceptions = new HashSet<>(config.getTrackedExceptions());
        this.metricsFactory = metricsFactory;
        this.stateRef = new AtomicReference<>(new CircuitBreakerStateHolder(this,
                metricsFactory.createDefaultMetrics(config.getMetricsWindowSize()), State.CLOSED));
    }

    /**
     * Get a snapshot of the current metrics within the time window. Use the snapshot to access success count, failure count,
     * total count, and failure rate.
     *
     * @return an immutable snapshot of current metrics
     */
    @Override
    public MetricsSnapshot getSnapshot() {
        return stateRef.get().metrics.getSnapshot();
    }

    @Override
    public CircuitBreakerGeneration getGeneration() {
        return stateRef.get();
    }

    @Override
    public String toString() {
        CircuitBreakerStateHolder current = stateRef.get();
        return "CircuitBreaker{" + "state=" + current.state + ", metrics=" + current.metrics + ", config=" + config + '}';
    }

    boolean isCircuitBreakerTrackedException(Throwable throwable) {
        Class<? extends Throwable> errorClass = throwable.getClass();
        for (Class<? extends Throwable> trackedException : trackedExceptions) {
            if (trackedException.isAssignableFrom(errorClass)) {
                return true;
            }
        }
        return false;
    }

    void recordResult(CircuitBreakerStateHolder generation, Throwable error) {
        if (error != null && isCircuitBreakerTrackedException(error)) {
            recordFailure(generation);
        } else {
            recordSuccess(generation);
        }
    }

    /**
     * ONLY FOR THE PURPOSE OF TESTING. Record a successful command execution.
     */
    void recordFailure() {
        recordFailure(stateRef.get());
    }

    void recordFailure(CircuitBreakerStateHolder state) {
        state.metrics.recordFailure();
        evaluateMetrics(state);
    }

    /**
     * ONLY FOR THE PURPOSE OF TESTING. Record a successful command execution.
     */
    void recordSuccess() {
        recordSuccess(stateRef.get());
    }

    void recordSuccess(CircuitBreakerStateHolder state) {
        state.metrics.recordSuccess();
    }

    /**
     * ONLY FOR THE PURPOSE OF TESTING. Evaluate the current metrics to determine if the circuit breaker should transition to a
     * new state.
     *
     * <p>
     * This method checks the failure rate and failure count against the configured thresholds. If the thresholds are met, the
     * circuit breaker transitions to the OPEN state. Metrics are reset when the state changes.
     * </p>
     *
     * @return an immutable snapshot of current metrics
     */
    MetricsSnapshot evaluateMetrics() {
        return evaluateMetrics(stateRef.get());
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
    MetricsSnapshot evaluateMetrics(CircuitBreakerImpl.CircuitBreakerStateHolder current) {
        MetricsSnapshot snapshot = current.metrics.getSnapshot();
        boolean evaluationResult = snapshot.getFailureRate() >= config.getFailureRateThreshold()
                && snapshot.getFailureCount() >= config.getMinimumNumberOfFailures();
        if (evaluationResult) {
            stateTransitionTo(State.OPEN);
        }
        return snapshot;
    }

    /**
     * ONLY FOR THE PURPOSE OF TESTING. Switch the circuit breaker to the specified state. This method is used to force the
     * circuit breaker to a specific state.
     *
     * <p>
     * This method does not evaluate the metrics to determine if the state transition is valid. It simply transitions to the
     * specified state. Metrics are reset when the state changes.
     * </p>
     *
     * @param newState the target state
     */
    void transitionTo(State newState) {
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
            CircuitBreakerMetrics nextMetrics = metricsFactory.createDefaultMetrics(config.getMetricsWindowSize());

            CircuitBreakerStateHolder next = new CircuitBreakerStateHolder(this, nextMetrics, newState);

            // Atomically swap if current state hasn't changed
            if (stateRef.compareAndSet(current, next)) {
                fireStateChanged(current.state, newState);
                return;
            }
            // CAS failed, retry with updated current state
        }
    }

    /**
     * Get the current state of the circuit breaker.
     *
     * @return the current state
     */
    @Override
    public State getCurrentState() {
        return stateRef.get().state;
    }

    @Override
    public boolean isClosed() {
        return getCurrentState() == State.CLOSED;
    }

    /**
     * Add a listener for circuit breaker state change events.
     *
     * @param listener the listener to add, must not be {@code null}
     */
    @Override
    public void addListener(CircuitBreakerStateListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener for circuit breaker state change events.
     *
     * @param listener the listener to remove, must not be {@code null}
     */
    @Override
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

    /**
     * Immutable holder for circuit breaker state and metrics.
     * <p>
     * This class enables atomic updates of both state and metrics using {@link AtomicReference}. When a state transition
     * occurs, a new instance is created with fresh metrics.
     */
    private static final class CircuitBreakerStateHolder implements CircuitBreakerGeneration {

        final State state;

        final CircuitBreakerMetrics metrics;

        final CircuitBreakerImpl circuitBreaker;

        CircuitBreakerStateHolder(CircuitBreakerImpl circuitBreaker, CircuitBreakerMetrics metrics, State state) {
            this.state = state;
            this.metrics = metrics;
            this.circuitBreaker = circuitBreaker;
        }

        @Override
        public void recordResult(Object output, Throwable error) {
            CircuitBreakerGeneration current = circuitBreaker.getGeneration();
            if (current != this) {
                return;
            }
            circuitBreaker.recordResult(this, error);
        }

    }

}
