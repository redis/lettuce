package io.lettuce.core.failover.health;

public interface HealthStatusListener {

    void onStatusChange(HealthStatusChangeEvent event);

}
