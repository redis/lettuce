package io.lettuce.core.failover;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.HealthStatusChangeEvent;
import io.lettuce.core.failover.health.HealthStatusListener;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.resource.ClientResources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for tracking health status changes for specific endpoints.
 *
 * StatusTracker is responsible for tracking and waiting for health status changes for specific endpoints. It provides an
 * event-driven approach to wait for health status transitions from UNKNOWN to either HEALTHY or UNHEALTHY.
 */
class StatusTracker {

    private final HealthStatusManager healthStatusManager;

    private final ScheduledExecutorService scheduler;

    public StatusTracker(HealthStatusManager healthStatusManager, ClientResources clientResources) {
        this.healthStatusManager = healthStatusManager;
        this.scheduler = clientResources.eventExecutorGroup();
    }

    /**
     * Asynchronously waits for a specific endpoint's health status to be determined (not UNKNOWN). Uses event-driven approach
     * with CompletableFuture to avoid blocking.
     *
     * @param endpoint the endpoint to wait for
     * @return CompletableFuture that completes with the determined health status (HEALTHY or UNHEALTHY)
     */
    public CompletableFuture<HealthStatus> waitForHealthStatusAsync(RedisURI endpoint) {
        // First check if status is already determined
        HealthStatus currentStatus = healthStatusManager.getHealthStatus(endpoint);
        if (currentStatus != HealthStatus.UNKNOWN) {
            return CompletableFuture.completedFuture(currentStatus);
        }

        // Create a CompletableFuture to return
        CompletableFuture<HealthStatus> future = new CompletableFuture<>();
        AtomicBoolean listenerRemoved = new AtomicBoolean(false);

        // Create a temporary listener for this specific endpoint
        HealthStatusListener tempListener = new HealthStatusListener() {

            @Override
            public void onStatusChange(HealthStatusChangeEvent event) {
                if (event.getEndpoint().equals(endpoint) && event.getNewStatus() != HealthStatus.UNKNOWN) {
                    // Complete the future with the new status
                    if (future.complete(event.getNewStatus())) {
                        // Successfully completed, clean up listener
                        if (listenerRemoved.compareAndSet(false, true)) {
                            healthStatusManager.unregisterListener(endpoint, this);
                        }
                    }
                }
            }

        };

        // Register the temporary listener
        healthStatusManager.registerListener(endpoint, tempListener);

        // Double-check status after registering listener (race condition protection)
        currentStatus = healthStatusManager.getHealthStatus(endpoint);
        if (currentStatus != HealthStatus.UNKNOWN) {
            // Status already determined, complete immediately
            if (listenerRemoved.compareAndSet(false, true)) {
                healthStatusManager.unregisterListener(endpoint, tempListener);
            }
            future.complete(currentStatus);
            return future;
        }

        // Set up timeout manually
        long timeoutMs = healthStatusManager.getMaxWaitFor(endpoint);

        scheduler.schedule(() -> {
            // Try to complete exceptionally with timeout
            if (future.completeExceptionally(
                    new RedisConnectionException("Timeout while waiting for health check result for " + endpoint))) {
                // Successfully completed with timeout, clean up listener
                if (listenerRemoved.compareAndSet(false, true)) {
                    healthStatusManager.unregisterListener(endpoint, tempListener);
                }
            }
            scheduler.shutdown();
        }, timeoutMs, TimeUnit.MILLISECONDS);

        // Clean up scheduler when future completes (either successfully or exceptionally)
        future.whenComplete((status, throwable) -> {
            // Ensure listener is removed
            if (listenerRemoved.compareAndSet(false, true)) {
                healthStatusManager.unregisterListener(endpoint, tempListener);
            }
        });

        return future;
    }

}
