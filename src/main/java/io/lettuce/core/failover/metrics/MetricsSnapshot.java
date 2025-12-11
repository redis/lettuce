package io.lettuce.core.failover.metrics;

import io.lettuce.core.annotations.Experimental;

/**
 * Represents the state of success and failure counts within a specific time window.
 *
 * <p>
 * It captures a consistent view of metrics at the moment of creation and does not change afterward.
 * </p>
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public interface MetricsSnapshot {

    /**
     * Get the number of successful command executions in the time window.
     *
     * @return the success count
     */
    long getSuccessCount();

    /**
     * Get the number of failed command executions in the time window.
     *
     * @return the failure count
     */
    long getFailureCount();

    /**
     * Get the total number of commands (success + failure) in the time window.
     *
     * @return the total count
     */
    long getTotalCount();

    /**
     * Get the failure rate as a percentage (0-100).
     *
     * @return the failure rate, or 0 if no commands have been executed
     */
    double getFailureRate();

    /**
     * Get the success rate as a percentage (0-100).
     *
     * @return the success rate, or 0 if no commands have been executed
     */
    double getSuccessRate();

}
