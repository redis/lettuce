package io.lettuce.core.failover.health;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.lettuce.core.RedisURI;

/**
 * Unit tests for {@link HealthCheckImpl} {@link ProbingPolicy} functionality.
 *
 * @author Ali Takavci
 * @author Ivo Gaydazhiev
 */
class HealthCheckImplProbingPolicyUnitTests {

    private RedisURI testEndpoint;

    @BeforeEach
    void setUp() {
        testEndpoint = RedisURI.create("redis://localhost:6379");
    }

    @Test
    @Timeout(5)
    void testAllSuccessStopsOnFirstFailure() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        CountDownLatch unhealthyLatch = new CountDownLatch(1);

        TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(HealthCheckStrategy.Config.builder().interval(5)
                .timeout(200).numProbes(3).policy(ProbingPolicy.BuiltIn.ALL_SUCCESS).delayInBetweenProbes(5).build(), e -> {
                    int c = callCount.incrementAndGet();
                    return c == 1 ? HealthStatus.UNHEALTHY : HealthStatus.HEALTHY;
                });

        HealthCheckImpl hc = new HealthCheckImpl(testEndpoint, strategy);
        hc.addListener(evt -> {
            if (evt.getNewStatus() == HealthStatus.UNHEALTHY)
                unhealthyLatch.countDown();
        });
        hc.start();

        assertThat(unhealthyLatch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(hc.getStatus()).isEqualTo(HealthStatus.UNHEALTHY);
        // ALL_SUCCESS should stop after first failure
        assertThat(callCount.get()).isEqualTo(1);
        hc.stop();

    }

    @Test
    @Timeout(5)
    void testMajorityEarlySuccessStopsAtThree() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        CountDownLatch healthyLatch = new CountDownLatch(1);

        TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(HealthCheckStrategy.Config.builder().interval(5000)
                .timeout(200).numProbes(5).policy(ProbingPolicy.BuiltIn.MAJORITY_SUCCESS).delayInBetweenProbes(5).build(),
                e -> {
                    int c = callCount.incrementAndGet();
                    return c <= 3 ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
                });

        HealthCheckImpl hc = new HealthCheckImpl(testEndpoint, strategy);
        hc.addListener(evt -> {
            if (evt.getNewStatus() == HealthStatus.HEALTHY)
                healthyLatch.countDown();
        });
        hc.start();

        assertThat(healthyLatch.await(1, TimeUnit.SECONDS)).isTrue();
        // MAJORITY early success should stop after 3 successes
        assertThat(callCount.get()).isEqualTo(3);

        hc.stop();
    }

    @Test
    @Timeout(5)
    void testMajorityEarlyFailStopsAtTwo() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        CountDownLatch unhealthyLatch = new CountDownLatch(1);

        TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(HealthCheckStrategy.Config.builder().interval(5000)
                .timeout(200).numProbes(4).policy(ProbingPolicy.BuiltIn.MAJORITY_SUCCESS).delayInBetweenProbes(5).build(),
                e -> {
                    int c = callCount.incrementAndGet();
                    return c <= 2 ? HealthStatus.UNHEALTHY : HealthStatus.HEALTHY;
                });

        HealthCheckImpl hc = new HealthCheckImpl(testEndpoint, strategy);
        hc.addListener(evt -> {
            if (evt.getNewStatus() == HealthStatus.UNHEALTHY)
                unhealthyLatch.countDown();
        });
        hc.start();

        assertThat(unhealthyLatch.await(1, TimeUnit.SECONDS)).isTrue();
        // MAJORITY early fail should stop when majority impossible
        assertThat(callCount.get()).isEqualTo(2);

        hc.stop();
    }

}
