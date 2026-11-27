package io.lettuce.core.failover.health;

/**
 * Listener for health status changes.
 *
 * @author Ali Takavci
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
public interface HealthStatusListener {

    void onStatusChange(HealthStatusChangeEvent event);

}
