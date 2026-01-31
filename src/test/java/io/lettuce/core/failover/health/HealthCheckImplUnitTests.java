package io.lettuce.core.failover.health;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.lettuce.core.RedisURI;

/**
 * Unit tests for {@link HealthCheckImpl}.
 *
 * @author Ivo Gaydazhiev
 */
class HealthCheckImplUnitTests {

    @Mock
    private HealthCheckStrategy mockStrategy;

    private RedisURI testEndpoint;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testEndpoint = RedisURI.create("redis://localhost:6379");

        // Default stubs for mockStrategy used across tests
        when(mockStrategy.getNumProbes()).thenReturn(1);
        when(mockStrategy.getDelayInBetweenProbes()).thenReturn(100);
        when(mockStrategy.getPolicy()).thenReturn(ProbingPolicy.BuiltIn.ANY_SUCCESS);
    }

    // ========== HealthCheck Tests ==========

    @Test
    void testHealthCheckStatusUpdate() throws InterruptedException {
        when(mockStrategy.getInterval()).thenReturn(1);
        when(mockStrategy.getTimeout()).thenReturn(50);
        when(mockStrategy.doHealthCheck(any(RedisURI.class))).thenReturn(HealthStatus.UNHEALTHY);

        CountDownLatch latch = new CountDownLatch(1);
        HealthStatusListener listener = event -> {
            assertThat(event.getOldStatus()).isEqualTo(HealthStatus.UNKNOWN);
            assertThat(event.getNewStatus()).isEqualTo(HealthStatus.UNHEALTHY);
            latch.countDown();
        };

        HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy);
        healthCheck.addListener(listener);
        healthCheck.start();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        healthCheck.stop();
    }

    @Test
    void testSafeUpdateChecksDoNotTriggerFalseNotifications() {
        AtomicInteger notificationCount = new AtomicInteger(0);
        HealthStatusListener listener = event -> notificationCount.incrementAndGet();

        HealthCheckImpl healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy);
        healthCheck.addListener(listener);

        // Simulate concurrent health checks with different results
        healthCheck.safeUpdate(2000, HealthStatus.HEALTHY); // Newer timestamp
        healthCheck.safeUpdate(1000, HealthStatus.UNHEALTHY); // Older timestamp (should be ignored)

        // Should only have 1 notification (for the first update), not 2
        assertThat(notificationCount.get()).isEqualTo(1);
        assertThat(healthCheck.getStatus()).isEqualTo(HealthStatus.HEALTHY);
    }

    @Test
    void testSafeUpdateWithConcurrentResults() {
        AtomicInteger notificationCount = new AtomicInteger(0);
        HealthStatusListener listener = event -> notificationCount.incrementAndGet();

        HealthCheckImpl healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy);
        healthCheck.addListener(listener);

        // Test the exact scenario: newer result first, then older result
        healthCheck.safeUpdate(2000, HealthStatus.HEALTHY); // Should update and notify
        assertThat(notificationCount.get()).isEqualTo(1);
        assertThat(healthCheck.getStatus()).isEqualTo(HealthStatus.HEALTHY);

        healthCheck.safeUpdate(1000, HealthStatus.UNHEALTHY); // Should NOT update or notify
        assertThat(notificationCount.get()).isEqualTo(1); // Still 1, no additional notification
        assertThat(healthCheck.getStatus()).isEqualTo(HealthStatus.HEALTHY); // Status unchanged
    }

    @Test
    void testHealthCheckStop() {
        when(mockStrategy.getInterval()).thenReturn(1000);
        when(mockStrategy.getTimeout()).thenReturn(500);

        HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy);
        healthCheck.start();

        assertThatCode(healthCheck::stop).doesNotThrowAnyException();
    }

    // ========== HealthStatusManager Tests ==========

    @Test
    void testHealthStatusManagerRegisterListener() throws InterruptedException {
        HealthCheckStrategy alwaysHealthyStrategy = createAlwaysHealthyStrategy();

        HealthStatusManagerImpl manager = new HealthStatusManagerImpl();
        HealthStatusListener listener = mock(HealthStatusListener.class);

        manager.registerListener(listener);

        // Verify listener is registered by triggering an event
        manager.add(testEndpoint, alwaysHealthyStrategy);

        // Give some time for health check to run
        Thread.sleep(100);

        verify(listener, atLeastOnce()).onStatusChange(any(HealthStatusChangeEvent.class));

        manager.close();
    }

    @Test
    void testHealthStatusManagerUnregisterListener() throws InterruptedException {
        HealthCheckStrategy alwaysHealthyStrategy = createAlwaysHealthyStrategy();

        HealthStatusManagerImpl manager = new HealthStatusManagerImpl();
        HealthStatusListener listener = mock(HealthStatusListener.class);

        manager.registerListener(listener);
        manager.unregisterListener(listener);

        manager.add(testEndpoint, alwaysHealthyStrategy);

        // Give some time for potential health check
        Thread.sleep(100);

        verify(listener, never()).onStatusChange(any(HealthStatusChangeEvent.class));

        manager.close();
    }

    @Test
    void testHealthStatusManagerEndpointSpecificListener() throws InterruptedException {
        HealthCheckStrategy alwaysHealthyStrategy = createAlwaysHealthyStrategy();

        HealthStatusManagerImpl manager = new HealthStatusManagerImpl();
        HealthStatusListener listener = mock(HealthStatusListener.class);
        RedisURI otherEndpoint = RedisURI.create("redis://other:6379");

        manager.registerListener(testEndpoint, listener);
        manager.add(testEndpoint, alwaysHealthyStrategy);
        manager.add(otherEndpoint, alwaysHealthyStrategy);

        // Give some time for health checks
        Thread.sleep(100);

        // Listener should only receive events for testEndpoint
        verify(listener, atLeastOnce()).onStatusChange(argThat(event -> event.getEndpoint().equals(testEndpoint)));

        manager.close();
    }

    @Test
    void testHealthStatusManagerLifecycle() throws InterruptedException {
        try (HealthStatusManagerImpl manager = new HealthStatusManagerImpl()) {

            // Before adding health check
            assertThat(manager.getHealthStatus(testEndpoint)).isEqualTo(HealthStatus.UNKNOWN);

            // Set up event listener to wait for initial health check completion
            CountDownLatch healthCheckCompleteLatch = new CountDownLatch(1);
            HealthStatusListener listener = event -> healthCheckCompleteLatch.countDown();

            // Register listener before adding health check to capture the initial event
            manager.registerListener(testEndpoint, listener);

            HealthCheckStrategy delayedStrategy = new TestHealthCheckStrategy(
                    HealthCheckStrategy.Config.builder().interval(2000).timeout(1000).delayInBetweenProbes(100).numProbes(3)
                            .policy(ProbingPolicy.BuiltIn.ALL_SUCCESS).build(),
                    e -> HealthStatus.HEALTHY);

            // Add health check - this will start async health checking
            manager.add(testEndpoint, delayedStrategy);

            // Initially should still be UNKNOWN until first check completes
            assertThat(manager.getHealthStatus(testEndpoint)).isEqualTo(HealthStatus.UNKNOWN);

            // Wait for initial health check to complete
            assertTrue(healthCheckCompleteLatch.await(2, TimeUnit.SECONDS),
                    "Initial health check should complete within timeout");

            // Now should be HEALTHY after initial check
            assertThat(manager.getHealthStatus(testEndpoint)).isEqualTo(HealthStatus.HEALTHY);

            // Clean up and verify removal
            manager.remove(testEndpoint);
            assertThat(manager.getHealthStatus(testEndpoint)).isEqualTo(HealthStatus.UNKNOWN);
        }
    }

    @Test
    void testHealthStatusManagerAddAndRemove() throws InterruptedException {
        HealthCheckStrategy delayedStrategy = createDelayedHealthyStrategy();

        HealthStatusManagerImpl manager = new HealthStatusManagerImpl();

        // Add health check - this will start async health checking
        manager.add(testEndpoint, delayedStrategy);

        // Initially should still be UNKNOWN until first check completes
        assertThat(manager.getHealthStatus(testEndpoint)).isEqualTo(HealthStatus.UNKNOWN);

        // Wait for initial health check to complete
        Thread.sleep(200);

        // Now should be HEALTHY after initial check
        assertThat(manager.getHealthStatus(testEndpoint)).isEqualTo(HealthStatus.HEALTHY);

        // Clean up and verify removal
        manager.remove(testEndpoint);
        assertThat(manager.getHealthStatus(testEndpoint)).isEqualTo(HealthStatus.UNKNOWN);

        manager.close();
    }

    @Test
    void testHealthStatusManagerClose() {
        HealthCheckStrategy closeableStrategy = mock(HealthCheckStrategy.class);
        when(closeableStrategy.getNumProbes()).thenReturn(1);
        when(closeableStrategy.getInterval()).thenReturn(1000);
        when(closeableStrategy.getTimeout()).thenReturn(500);
        when(closeableStrategy.getPolicy()).thenReturn(ProbingPolicy.BuiltIn.ANY_SUCCESS);
        when(closeableStrategy.getDelayInBetweenProbes()).thenReturn(100);
        when(closeableStrategy.doHealthCheck(any(RedisURI.class))).thenReturn(HealthStatus.HEALTHY);

        HealthStatusManagerImpl manager = new HealthStatusManagerImpl();

        // Add health check
        manager.add(testEndpoint, closeableStrategy);

        // Close manager
        manager.close();

        // Verify health check is stopped (close is called on strategy if it's AutoCloseable)
        verify(closeableStrategy, atLeastOnce()).close();
    }

    // ========== Helper Methods ==========

    private HealthCheckStrategy createAlwaysHealthyStrategy() {
        return new TestHealthCheckStrategy(
                HealthCheckStrategy.Config.builder().interval(10).timeout(50).numProbes(1).delayInBetweenProbes(10).build(),
                endpoint -> HealthStatus.HEALTHY);
    }

    private HealthCheckStrategy createDelayedHealthyStrategy() {
        return new TestHealthCheckStrategy(
                HealthCheckStrategy.Config.builder().interval(100).timeout(200).numProbes(1).delayInBetweenProbes(10).build(),
                endpoint -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return HealthStatus.HEALTHY;
                });
    }

}
