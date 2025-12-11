package io.lettuce.core.failover.health;

import io.lettuce.core.annotations.Experimental;

/**
 * Listener for health status changes.
 *
 * @author Ali Takavci
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
@Experimental
public interface HealthStatusListener {

    void onStatusChange(HealthStatusChangeEvent event);

}
