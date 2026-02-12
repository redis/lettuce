package io.lettuce.core.failover.api;

import io.lettuce.core.RedisException;
import io.lettuce.core.annotations.Experimental;

/**
 * Exception thrown when a circuit breaker is not in closed state.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public class RedisCircuitBreakerException extends RedisException {

    public static final RedisCircuitBreakerException INSTANCE = new RedisCircuitBreakerException();

    public RedisCircuitBreakerException() {
        super("Circuit breaker is not in closed state, request cannot be processed!");
    }

}
