package io.lettuce.core.failover;

/**
 * Represents a specific generation of a circuit breaker. This interface provides methods to record the result of a command
 * execution for the metrics tracking of that specific generation.
 *
 * @author Ali Takavci
 * @since 7.1
 */
interface CircuitBreakerGeneration {

    /**
     * Record the result of a command execution for the metrics tracking of this generation.
     *
     * @param output the command output
     * @param error the error, if any
     */
    void recordResult(Object output, Throwable error);

}
