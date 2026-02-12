package io.lettuce.core.failover;

import java.io.Closeable;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.failover.api.CircuitBreakerStateListener;
import io.lettuce.core.failover.metrics.MetricsSnapshot;

/**
 * Circuit breaker for tracking command metrics and managing circuit breaker state.
 *
 * <p>
 * This interface provides methods to track command metrics and manage circuit breaker state. Implementations must be
 * thread-safe.
 * </p>
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public interface CircuitBreaker extends Closeable {

    /**
     * Get the ID for this circuit breaker.
     *
     * @return the ID
     */
    String getId();

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
     * Transition the circuit breaker to the specified state. This method is used to force the circuit breaker to a specific
     * state.
     *
     * @param newState the target state
     */
    void transitionTo(State newState);

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

        CLOSED, OPEN;

        public boolean isClosed() {
            return this == CLOSED;
        }

    }

}
