package io.lettuce.core.failover.event;

/**
 * Reasons for database switch.
 *
 * @see DatabaseSwitchEvent
 * @author Ivo Gaydajiev
 * @since 7.4
 */
public enum SwitchReason {
    /**
     * Switch was triggered by health check.
     */
    HEALTH_CHECK,
    /**
     * Switch was triggered by circuit breaker.
     */
    CIRCUIT_BREAKER,
    /**
     * Switch was triggered to failback to a more weighted database.
     */
    FAILBACK,
    /**
     * Switch was triggered by user request.
     */
    FORCED
}
