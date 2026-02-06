package io.lettuce.core.failover;

import io.lettuce.core.annotations.Experimental;

/**
 * Represents a specific generation of a circuit breaker. This interface provides methods to record the result of a command
 * execution for the metrics tracking of that specific generation.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
interface CircuitBreakerGeneration {

    /**
     * Record the result of a command execution for the metrics tracking of this generation. * @param error the error, if any
     */
    void recordResult(Throwable error);

}
