package io.lettuce.core.failover.api;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.failover.CircuitBreaker;

/**
 * Event representing a circuit breaker state change.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public class CircuitBreakerStateChangeEvent {

    private final CircuitBreaker circuitBreaker;

    private final CircuitBreaker.State previousState;

    private final CircuitBreaker.State newState;

    private final long timestamp;

    /**
     * Create a new circuit breaker state change event.
     *
     * @param circuitBreaker the circuit breaker instance
     * @param previousState the previous state
     * @param newState the new state
     */
    public CircuitBreakerStateChangeEvent(CircuitBreaker circuitBreaker, CircuitBreaker.State previousState,
            CircuitBreaker.State newState) {
        this.circuitBreaker = circuitBreaker;
        this.previousState = previousState;
        this.newState = newState;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Get the circuit breaker instance that changed state.
     *
     * @return the circuit breaker instance
     */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * Get the previous state before the transition.
     *
     * @return the previous state
     */
    public CircuitBreaker.State getPreviousState() {
        return previousState;
    }

    /**
     * Get the new state after the transition.
     *
     * @return the new state
     */
    public CircuitBreaker.State getNewState() {
        return newState;
    }

    /**
     * Get the timestamp when the state change occurred.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "CircuitBreakerStateChangeEvent{" + "previousState=" + previousState + ", newState=" + newState + ", timestamp="
                + timestamp + '}';
    }

}
