package io.lettuce.core.failover.health;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;

/**
 * Unit tests for {@link HealthCheckImpl} retry logic (numProbes).
 *
 * @author Ivo Gaydazhiev
 */
class HealthCheckImplRetryLogicUnitTests {

    private RedisURI testEndpoint;

    @BeforeEach
    void setUp() {
        testEndpoint = RedisURI.create("redis://localhost:6379");
    }

    @Test
    void testSuccessOnFirstAttempt() throws InterruptedException {
        AtomicInteger callCount = new AtomicInteger(0);
        TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(HealthCheckStrategy.Config.builder().interval(100)
                .timeout(50).numProbes(2).policy(ProbingPolicy.BuiltIn.ANY_SUCCESS).delayInBetweenProbes(10).build(), e -> {
                    callCount.incrementAndGet();
                    return HealthStatus.HEALTHY; // Always succeeds
                });

        CountDownLatch latch = new CountDownLatch(1);
        HealthStatusListener listener = event -> latch.countDown();

        HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, strategy);
        healthCheck.addListener(listener);
        healthCheck.start();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(healthCheck.getStatus()).isEqualTo(HealthStatus.HEALTHY);

        // Should only call doHealthCheck once (no retries needed)
        assertThat(callCount.get()).isEqualTo(1);

        healthCheck.stop();
    }

    @Test
    void testSuccessOnSecondAttempt() throws InterruptedException {
        AtomicInteger callCount = new AtomicInteger(0);
        TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(HealthCheckStrategy.Config.builder().interval(100)
                .timeout(50).numProbes(2).policy(ProbingPolicy.BuiltIn.ANY_SUCCESS).delayInBetweenProbes(10).build(), e -> {
                    int attempt = callCount.incrementAndGet();
                    if (attempt == 1) {
                        throw new RuntimeException("First attempt fails");
                    }
                    return HealthStatus.HEALTHY;
                });

        CountDownLatch latch = new CountDownLatch(1);
        HealthStatusListener listener = event -> {
            if (event.getNewStatus() == HealthStatus.HEALTHY) {
                latch.countDown();
            }
        };

        HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, strategy);
        healthCheck.addListener(listener);
        healthCheck.start();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(healthCheck.getStatus()).isEqualTo(HealthStatus.HEALTHY);

        // Should call doHealthCheck twice (1 failure + 1 success)
        assertThat(callCount.get()).isEqualTo(2);

        healthCheck.stop();
    }

    @Test
    void testExhaustAllProbesAndFailWithAnySuccessPolicy() throws InterruptedException {
        AtomicInteger callCount = new AtomicInteger(0);
        TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(HealthCheckStrategy.Config.builder().interval(100)
                .timeout(50).numProbes(3).policy(ProbingPolicy.BuiltIn.ANY_SUCCESS).delayInBetweenProbes(10).build(), e -> {
                    callCount.incrementAndGet();
                    throw new RuntimeException("Always fails");
                });

        CountDownLatch latch = new CountDownLatch(1);
        HealthStatusListener listener = event -> {
            if (event.getNewStatus() == HealthStatus.UNHEALTHY) {
                latch.countDown();
            }
        };

        HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, strategy);
        healthCheck.addListener(listener);
        healthCheck.start();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(healthCheck.getStatus()).isEqualTo(HealthStatus.UNHEALTHY);

        // Should call doHealthCheck 3 times (all attempts exhausted)
        assertThat(callCount.get()).isEqualTo(3);

        healthCheck.stop();
    }

    @Test
    void zeroProbes() {
        AtomicInteger callCount = new AtomicInteger(0);

        TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(HealthCheckStrategy.Config.builder().interval(100)
                .timeout(50).numProbes(0).policy(ProbingPolicy.BuiltIn.ANY_SUCCESS).delayInBetweenProbes(10).build(), e -> {
                    callCount.incrementAndGet();
                    throw new RuntimeException("Fails");
                });

        assertThrows(IllegalArgumentException.class, () -> new HealthCheckImpl(testEndpoint, strategy));
    }

    @Test
    void negativeProbes() {
        AtomicInteger callCount = new AtomicInteger(0);
        TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(HealthCheckStrategy.Config.builder().interval(100)
                .timeout(50).numProbes(-1).policy(ProbingPolicy.BuiltIn.ANY_SUCCESS).delayInBetweenProbes(10).build(), e -> {
                    callCount.incrementAndGet();
                    throw new RuntimeException("Fails");
                });

        assertThrows(IllegalArgumentException.class, () -> new HealthCheckImpl(testEndpoint, strategy));
    }

    /**
     * <p>
     * - Verifies that the health check probes stop after the first probe when the scheduler thread is interrupted.
     * <p>
     * - The scheduler thread is the one that calls healthCheck(), which in turn calls doHealthCheck().
     * <p>
     * - This test interrupts the scheduler thread while it is waiting on the future from the first probe.
     * <p>
     * - The health check operation itself is not interrupted. This test does not validate interruption of the health check
     * operation itself, as that is not the responsibility of the HealthCheckImpl.
     */
    @Test
    void testInterruptionStopsProbes() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        CountDownLatch schedulerTaskStarted = new CountDownLatch(1);
        CountDownLatch statusChanged = new CountDownLatch(1);

        Thread[] schedulerThread = new Thread[1];

        final int OPERATION_TIMEOUT = 1000;
        final int LESS_THAN_OPERATION_TIMEOUT = 800;
        final int NUM_PROBES = 3;
        // Long interval so no second run, generous timeout so we can interrupt while waiting
        Function<RedisURI, HealthStatus> healthCheckOperation = e -> {
            callCount.incrementAndGet();
            try {
                Thread.sleep(LESS_THAN_OPERATION_TIMEOUT); // keep worker busy so scheduler waits on
                // future.get
            } catch (InterruptedException ie) {
            }
            return HealthStatus.UNHEALTHY;
        };

        // Override getPolicy() to capture the scheduler thread
        TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(
                HealthCheckStrategy.Config.builder().interval(5000).timeout(OPERATION_TIMEOUT).numProbes(NUM_PROBES)
                        .policy(ProbingPolicy.BuiltIn.ANY_SUCCESS).delayInBetweenProbes(10).build(),
                healthCheckOperation) {

            @Override
            public ProbingPolicy getPolicy() {
                schedulerThread[0] = Thread.currentThread();
                schedulerTaskStarted.countDown();
                return super.getPolicy();
            }

        };

        HealthStatusListener callback = evt -> statusChanged.countDown();

        HealthCheckImpl hc = new HealthCheckImpl(testEndpoint, strategy);
        hc.addListener(callback);
        hc.start();

        // Ensure first probe is in flight (scheduler is blocked on future.get)
        assertTrue(schedulerTaskStarted.await(1, TimeUnit.SECONDS), "Task should have started");

        // Interrupt the scheduler thread that runs HealthCheckImpl.healthCheck()
        schedulerThread[0].interrupt();

        // No status change should be published because healthCheck() returns early without safeUpdate
        assertFalse(statusChanged.await(hc.getMaxWaitFor(), TimeUnit.MILLISECONDS), "No status change expected");
        assertThat(hc.getStatus()).isEqualTo(HealthStatus.UNKNOWN);

        // Only the first probe should have been attempted
        int calls = callCount.get();
        assertTrue(calls <= 1, "Only one probe should have been attempted: " + calls);

        hc.stop();
    }

}
