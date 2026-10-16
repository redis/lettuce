package io.lettuce.core.failover.api;

/**
 * Listener interface for circuit breaker state change events.
 *
 * @author Ali Takavci
 * @since 7.4
 */
public interface CircuitBreakerStateListener {

    /**
     * Event handler for circuit breaker state change events.
     *
     * @param event the state change event containing previous state, new state, and the circuit breaker instance
     */
    void onCircuitBreakerStateChange(CircuitBreakerStateChangeEvent event);

}
