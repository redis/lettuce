package io.lettuce.core.failover;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.HealthStatusChangeEvent;
import io.lettuce.core.failover.health.HealthStatusListener;
import io.lettuce.core.failover.health.HealthStatusManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for tracking health status changes for specific endpoints.
 *
 * StatusTracker is responsible for tracking and waiting for health status changes for specific endpoints. It provides an
 * event-driven approach to wait for health status transitions from UNKNOWN to either HEALTHY or UNHEALTHY.
 */
class StatusTracker {

    private final HealthStatusManager healthStatusManager;

    public StatusTracker(HealthStatusManager healthStatusManager) {
        this.healthStatusManager = healthStatusManager;
    }

    /**
     * Waits for a specific endpoint's health status to be determined (not UNKNOWN). Uses event-driven approach with
     * CountDownLatch to avoid polling.
     * 
     * @param endpoint the endpoint to wait for
     * @return the determined health status (HEALTHY or UNHEALTHY)
     * @throws RedisConnectionException if interrupted while waiting or if a timeout occurs
     */
    public HealthStatus waitForHealthStatus(RedisURI endpoint) {
        // First check if status is already determined
        HealthStatus currentStatus = healthStatusManager.getHealthStatus(endpoint);
        if (currentStatus != HealthStatus.UNKNOWN) {
            return currentStatus;
        }

        // Set up event-driven waiting
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<HealthStatus> resultStatus = new AtomicReference<>();

        // Create a temporary listener for this specific endpoint
        HealthStatusListener tempListener = new HealthStatusListener() {

            @Override
            public void onStatusChange(HealthStatusChangeEvent event) {
                if (event.getEndpoint().equals(endpoint) && event.getNewStatus() != HealthStatus.UNKNOWN) {
                    resultStatus.set(event.getNewStatus());
                    latch.countDown();
                }
            }

        };

        // Register the temporary listener
        healthStatusManager.registerListener(endpoint, tempListener);

        try {
            // Double-check status after registering listener (race condition protection)
            currentStatus = healthStatusManager.getHealthStatus(endpoint);
            if (currentStatus != HealthStatus.UNKNOWN) {
                return currentStatus;
            }

            // Wait for the health status change event
            // just for safety to not block indefinitely
            boolean completed = latch.await(healthStatusManager.getMaxWaitFor(endpoint), TimeUnit.MILLISECONDS);
            if (!completed) {
                throw new RedisConnectionException("Timeout while waiting for health check result");
            }
            return resultStatus.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisConnectionException("Interrupted while waiting for health check result", e);
        } finally {
            // Clean up: unregister the temporary listener
            healthStatusManager.unregisterListener(endpoint, tempListener);
        }
    }

}
