package io.lettuce.core.failover;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.health.HealthCheckStrategy;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.ProbingPolicy;
import io.lettuce.test.LettuceExtension;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;

/**
 * Integration tests for health check functionality in MultiDbClient.
 *
 * @author Ivo Gaydazhiev
 * @since 7.1
 */
@ExtendWith(LettuceExtension.class)
@Tag("integration")
@DisplayName("HealthCheck Integration Tests")
public class HealthCheckIntegrationTest extends MultiDbTestSupport {

    /** Default timeout for await() calls in this test - 500ms for fast test execution */
    private static final Duration AWAIT_TIMEOUT = Duration.ofMillis(500);

    /** Poll interval for await() calls - 2ms for responsive polling */
    private static final Duration POLL_INTERVAL = Duration.ofMillis(2);

    /** Expected run_id for uri1 Redis instance - used to verify we are connected to the correct endpoint */
    private String expectedRunIdUri1;

    /** Expected run_id for uri2 Redis instance - used to verify we are connected to the correct endpoint */
    private String expectedRunIdUri2;

    /** Expected run_id for uri3 Redis instance - used to verify we are connected to the correct endpoint */
    private String expectedRunIdUri3;

    @Inject
    HealthCheckIntegrationTest(MultiDbClient client) {
        super(client);
    }

    @BeforeEach
    void extractRunIds() {
        try (StatefulRedisConnection<String, String> conn1 = directClient1.connect()) {
            expectedRunIdUri1 = extractRunId(conn1.sync().info("server"));
        }
        try (StatefulRedisConnection<String, String> conn2 = directClient2.connect()) {
            expectedRunIdUri2 = extractRunId(conn2.sync().info("server"));
        }
        try (StatefulRedisConnection<String, String> conn3 = directClient3.connect()) {
            expectedRunIdUri3 = extractRunId(conn3.sync().info("server"));
        }
        assertThat(expectedRunIdUri1).isNotEmpty();
        assertThat(expectedRunIdUri2).isNotEmpty().isNotEqualTo(expectedRunIdUri1);
        assertThat(expectedRunIdUri3).isNotEmpty().isNotEqualTo(expectedRunIdUri1).isNotEqualTo(expectedRunIdUri2);
    }

    @Nested
    @DisplayName("Health Check Configuration")
    class HealthCheckConfigurationTests {

        @Test
        @DisplayName("Should create MultiDbClient without health checks when supplier is null")
        void shouldCreateClientWithoutHealthChecks() {
            // Given: DatabaseConfigs without HealthCheckStrategySupplier (null)
            DatabaseConfig config1 = new DatabaseConfig(uri1, 1.0f, null, null, null);
            DatabaseConfig config2 = new DatabaseConfig(uri2, 0.5f, null, null, null);

            // When: Create MultiDbClient and connect
            MultiDbClient testClient = MultiDbClient.create(java.util.Arrays.asList(config1, config2));
            StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

            try {
                // Then: Connection should work normally
                assertThat(connection).isNotNull();
                assertThat(connection.sync().ping()).isEqualTo("PONG");

                // And: Verify we can execute commands on both endpoints
                connection.switchToDatabase(uri1);
                connection.sync().set("test-key", "test-value");
                assertThat(connection.sync().get("test-key")).isEqualTo("test-value");

                connection.switchToDatabase(uri2);
                connection.sync().set("test-key2", "test-value2");
                assertThat(connection.sync().get("test-key2")).isEqualTo("test-value2");

                // And: Verify health status returns HEALTHY when health checks are not configured
                // (When no health check supplier is provided, the database is assumed healthy)
                assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.HEALTHY);
                assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.HEALTHY);

            } finally {
                connection.close();
                testClient.shutdown();
            }
        }

        @Test
        @DisplayName("Should create MultiDbClient with custom health check strategy supplier")
        void shouldCreateClientWithCustomHealthCheckSupplier() {
            // Given: Custom HealthCheckStrategySupplier with minimal delays for fast testing
            HealthCheckStrategy.Config config = HealthCheckStrategy.Config.builder().interval(1) // 1ms for fast testing
                    .timeout(10).numProbes(1).delayInBetweenProbes(1).build();

            TestHealthCheckStrategy testHealthCheckStrategy = new TestHealthCheckStrategy(config);
            HealthCheckStrategySupplier supplier = (uri, options) -> testHealthCheckStrategy;

            DatabaseConfig config1 = new DatabaseConfig(uri1, 1.0f, null, null, supplier);
            DatabaseConfig config2 = new DatabaseConfig(uri2, 0.5f, null, null, supplier);

            // When: Create MultiDbClient and connect
            MultiDbClient testClient = MultiDbClient.create(java.util.Arrays.asList(config1, config2));
            StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

            try {
                // Then: Connection should work
                assertThat(connection).isNotNull();
                assertThat(connection.sync().ping()).isEqualTo("PONG");

                // And: Health checks should be created and running
                // Wait for health status to transition from UNKNOWN to HEALTHY
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.HEALTHY);
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.HEALTHY);
                });

                // When: Change health status to UNHEALTHY
                testHealthCheckStrategy.setHealthStatus(uri1, HealthStatus.UNHEALTHY);

                // Then: Verify health status reflects the change
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.UNHEALTHY);
                });

                // Then: connection should failover to uri2
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getCurrentEndpoint()).isEqualTo(uri2);
                });

                // When: Change health status to UNHEALTHY
                testHealthCheckStrategy.setHealthStatus(uri2, HealthStatus.UNHEALTHY);

                // Then: Verify health status reflects the change
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.UNHEALTHY);
                });

                // And: when all endpoints are unhealthy
                // Then: connection should stay on the current endpoint
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getCurrentEndpoint()).isEqualTo(uri2);
                });

            } finally {
                connection.close();
                testClient.shutdown();
            }
        }

        @Test
        @DisplayName("Should use different health check strategies for different endpoints")
        void shouldUseDifferentStrategiesPerEndpoint() {
            // Given: Two independent health check strategies
            HealthCheckStrategy.Config config = HealthCheckStrategy.Config.builder().interval(1) // 1ms for fast testing
                    .timeout(10).numProbes(1).delayInBetweenProbes(1).build();

            TestHealthCheckStrategy strategy1 = new TestHealthCheckStrategy(config);
            TestHealthCheckStrategy strategy2 = new TestHealthCheckStrategy(config);

            // Different suppliers for each endpoint
            HealthCheckStrategySupplier supplier1 = (uri, options) -> strategy1;
            HealthCheckStrategySupplier supplier2 = (uri, options) -> strategy2;

            DatabaseConfig config1 = new DatabaseConfig(uri1, 1.0f, null, null, supplier1);
            DatabaseConfig config2 = new DatabaseConfig(uri2, 0.5f, null, null, supplier2);

            // When: Create MultiDbClient with different strategies per endpoint
            MultiDbClient testClient = MultiDbClient.create(java.util.Arrays.asList(config1, config2));
            StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

            try {
                // Then: Connection should work
                assertThat(connection).isNotNull();
                assertThat(connection.sync().ping()).isEqualTo("PONG");

                // And: Both endpoints should become HEALTHY
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.HEALTHY);
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.HEALTHY);
                });

                // When: Set different health statuses for each endpoint
                strategy1.setHealthStatus(uri1, HealthStatus.UNHEALTHY);
                strategy2.setHealthStatus(uri2, HealthStatus.HEALTHY);

                // Then: Each endpoint should reflect its own strategy's status
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.UNHEALTHY);
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.HEALTHY);
                });

                // When: Change strategy2's status to UNHEALTHY
                strategy2.setHealthStatus(uri2, HealthStatus.UNHEALTHY);

                // Then: uri2 should become UNHEALTHY while uri1 remains UNHEALTHY
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.UNHEALTHY);
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.UNHEALTHY);
                });

                // When: Restore uri1 to HEALTHY
                strategy1.setHealthStatus(uri1, HealthStatus.HEALTHY);

                // Then: uri1 should become HEALTHY while uri2 remains UNHEALTHY
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.HEALTHY);
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.UNHEALTHY);
                });

            } finally {
                connection.close();
                testClient.shutdown();
            }
        }

        @Test
        @DisplayName("Should configure health check interval and timeout")
        void shouldConfigureHealthCheckIntervalAndTimeout() {
            // Given: Custom interval and timeout values (minimum for fast testing)
            int customInterval = 1;
            int customTimeout = 10;

            HealthCheckStrategy.Config config = HealthCheckStrategy.Config.builder().interval(customInterval)
                    .timeout(customTimeout).numProbes(1).delayInBetweenProbes(1).build();

            TestHealthCheckStrategy testStrategy = new TestHealthCheckStrategy(config);
            HealthCheckStrategySupplier supplier = (uri, options) -> testStrategy;

            DatabaseConfig databaseConfig = new DatabaseConfig(uri1, 1.0f, null, null, supplier);

            // When: Create MultiDbClient and connect
            MultiDbClient testClient = MultiDbClient.create(Collections.singletonList(databaseConfig));
            StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

            try {
                // Then: Health checks should be running and called multiple times
                // With 1ms interval, we expect at least 5 calls within 20ms
                int expectedMinimumCalls = 5;
                with().pollInterval(5, MILLISECONDS).timeout(Duration.ofMillis(20)).await()
                        .until(() -> testStrategy.getHealthCheckCallCount(uri1) >= expectedMinimumCalls);

                // And: Verify health status is HEALTHY
                assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.HEALTHY);

                // And: Simulate timeout by introducing delay longer than timeout
                CountDownLatch timeoutLatch = new java.util.concurrent.CountDownLatch(1);
                testStrategy.setHealthCheckDelay(uri1, TestHealthCheckStrategy.Delay.Await(timeoutLatch)); // 15ms delay > 10ms
                                                                                                           // timeout

                // When health check times out, it should be recorded as failed and status becomes UNHEALTHY
                with().pollInterval(5, MILLISECONDS).timeout(Duration.ofMillis(customTimeout + 50)).await()
                        .untilAsserted(() -> {
                            assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.UNHEALTHY);
                        });

            } finally {
                connection.close();
                testClient.shutdown();
            }
        }

        @Test
        @DisplayName("Should configure health check probing policy")
        @Disabled("Not implemented yet")
        void shouldConfigureHealthCheckProbingPolicy() {
            // TODO: Implement test
            // - Create HealthCheckStrategy.Config with custom ProbingPolicy
            // - Create HealthCheckStrategySupplier using the config
            // - Create MultiDbClient and connect
            // - Verify health checks use the configured probing policy
        }

        @Test
        @DisplayName("Should configure number of probes and delay between probes")
        @Disabled("Not implemented yet")
        void shouldConfigureProbesAndDelay() {
            // TODO: Implement test
            // - Create HealthCheckStrategy.Config with custom numProbes and delayInBetweenProbes
            // - Create HealthCheckStrategySupplier using the config
            // - Create MultiDbClient and connect
            // - Verify health checks use the configured probe settings
        }

    }

    @Nested
    @DisplayName("Health Check Lifecycle")
    class HealthCheckLifecycleTests {

        @Test
        @DisplayName("Should start health checks automatically when connection is created")
        @Disabled("Not implemented yet")
        void shouldStartHealthChecksOnConnect() {
            // TODO: Implement test
            // - Create MultiDbClient with health check supplier
            // - Connect
            // - Verify health checks are started for all endpoints
            // - Verify initial status is UNKNOWN
        }

        @Test
        @DisplayName("Should stop health checks when connection is closed")
        @Disabled("Not implemented yet")
        void shouldStopHealthChecksOnClose() {
            // TODO: Implement test
            // - Create MultiDbClient with health check supplier
            // - Connect
            // - Close connection
            // - Verify health checks are stopped
        }

        @Test
        @DisplayName("Should restart health check when database is re-added")
        @Disabled("Not implemented yet")
        void shouldRestartHealthCheckOnDatabaseReAdd() {
            // TODO: Implement test
            // - Create MultiDbClient with health check supplier
            // - Connect
            // - Remove a database
            // - Re-add the same database
            // - Verify old health check is stopped and new one is started
        }

        @Test
        @DisplayName("Should transition from UNKNOWN to HEALTHY")
        void shouldTransitionFromUnknownToHealthy() {
            // Given: Health check strategy with fast interval for testing
            HealthCheckStrategy.Config config = HealthCheckStrategy.Config.builder().interval(1) // 1ms interval
                    .timeout(10).numProbes(1).delayInBetweenProbes(1).build();

            TestHealthCheckStrategy testStrategy = new TestHealthCheckStrategy(config);
            HealthCheckStrategySupplier supplier = (uri, options) -> testStrategy;

            // Block health check for uri2 initially to ensure we can verify UNKNOWN state
            // uri1 will become healthy immediately, allowing the connection to be created
            // At leasst one healhty endpoint is required to establish connection,
            // block health check for uri2 only
            CountDownLatch healthCheckUri2Latch = new CountDownLatch(1);
            testStrategy.setHealthCheckDelay(uri2, TestHealthCheckStrategy.Delay.Await(healthCheckUri2Latch));

            DatabaseConfig config1 = new DatabaseConfig(uri1, 1.0f, null, null, supplier);
            DatabaseConfig config2 = new DatabaseConfig(uri2, 0.5f, null, null, supplier);

            // When: Create MultiDbClient and connect
            MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2));
            StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

            try {
                // Then: uri1 should become HEALTHY (allowing connection to be created)
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.HEALTHY);
                });

                // And: uri2 should be UNKNOWN (health check is blocked)
                assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.UNKNOWN);

                // And: Verify health check was called for uri2 but is blocked
                awaitAtMost().untilAsserted(() -> {
                    assertThat(testStrategy.getHealthCheckCallCount(uri2)).isGreaterThan(0);
                });

                // And: Status should still be UNKNOWN while blocked
                assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.UNKNOWN);

                // When: Unblock health check to allow it to complete
                healthCheckUri2Latch.countDown();

                // Then: Wait for uri2 status to transition to HEALTHY
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.HEALTHY);
                });

            } finally {
                healthCheckUri2Latch.countDown(); // Ensure cleanup
                connection.close();
                testClient.shutdown();
            }
        }

    }

    @Nested
    @DisplayName("Health Status Listeners")
    class HealthStatusListenerTests {

        @Test
        @DisplayName("Should notify listeners on status change")
        @Disabled("Not implemented yet")
        void shouldNotifyListenersOnStatusChange() {
            // TODO: Implement test
            // - Create MultiDbClient with health check supplier
            // - Connect
            // - Register HealthStatusListener
            // - Trigger status change (e.g., stop Redis)
            // - Verify listener is notified with correct event
        }

        @Test
        @DisplayName("Should notify multiple listeners on status change")
        @Disabled("Not implemented yet")
        void shouldNotifyMultipleListeners() {
            // TODO: Implement test
            // - Create MultiDbClient with health check supplier
            // - Connect
            // - Register multiple HealthStatusListeners
            // - Trigger status change
            // - Verify all listeners are notified
        }

        @Test
        @DisplayName("Should not notify removed listeners")
        @Disabled("Not implemented yet")
        void shouldNotNotifyRemovedListeners() {
            // TODO: Implement test
            // - Create MultiDbClient with health check supplier
            // - Connect
            // - Register HealthStatusListener
            // - Remove the listener
            // - Trigger status change
            // - Verify listener is not notified
        }

        @Test
        @DisplayName("Should handle listener exceptions gracefully")
        @Disabled("Not implemented yet")
        void shouldHandleListenerExceptions() {
            // TODO: Implement test
            // - Create MultiDbClient with health check supplier
            // - Connect
            // - Register listener that throws exception
            // - Register another normal listener
            // - Trigger status change
            // - Verify exception doesn't prevent other listeners from being notified
            // - Verify health check continues to work
        }

    }

    @Nested
    @DisplayName("Failover Integration")
    class FailoverIntegrationTests {

        @Test
        @DisplayName("Should trigger failover when health check detects unhealthy endpoint")
        void shouldTriggerFailoverOnUnhealthyStatus() {
            // Given: MultiDbClient with health check supplier and multiple endpoints
            HealthCheckStrategy.Config config = HealthCheckStrategy.Config.builder().interval(1) // 1ms for fast testing
                    .timeout(10).numProbes(1).delayInBetweenProbes(1).build();

            TestHealthCheckStrategy testStrategy = new TestHealthCheckStrategy(config);
            HealthCheckStrategySupplier supplier = (uri, options) -> testStrategy;

            // uri1 has higher weight (1.0) than uri2 (0.5), so uri1 should be selected initially
            DatabaseConfig config1 = new DatabaseConfig(uri1, 1.0f, null, null, supplier);
            DatabaseConfig config2 = new DatabaseConfig(uri2, 0.5f, null, null, supplier);

            // When: Create MultiDbClient and connect
            MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2));
            StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

            try {
                // Then: Wait for all endpoints to be HEALTHY
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.HEALTHY);
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.HEALTHY);
                });

                // And: Verify current endpoint is uri1 (higher weight)
                assertThat(connection.getCurrentEndpoint()).isEqualTo(uri1);

                // And: Verify we're connected to uri1 by comparing run_id
                String actualRunIdBeforeFailover = extractRunId(connection.sync().info("server"));
                assertThat(actualRunIdBeforeFailover).isEqualTo(expectedRunIdUri1);

                // When: Simulate failure of current active endpoint (uri1)
                testStrategy.setHealthStatus(uri1, HealthStatus.UNHEALTHY);

                // Then: Wait for health check to detect failure
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.UNHEALTHY);
                });

                // And: Verify failover to another healthy endpoint (uri2)
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getCurrentEndpoint()).isEqualTo(uri2);
                });

                // And: Verify we're actually connected to uri2 by comparing run_id (proves TCP connection switched)
                awaitAtMost().untilAsserted(() -> {
                    String actualRunIdAfterFailover = extractRunId(connection.sync().info("server"));
                    assertThat(actualRunIdAfterFailover).isEqualTo(expectedRunIdUri2);
                });

                // And: Verify connection still works on uri2
                assertThat(connection.sync().ping()).isEqualTo("PONG");
                connection.sync().set("failover-test", "success");
                assertThat(connection.sync().get("failover-test")).isEqualTo("success");

            } finally {
                connection.close();
                testClient.shutdown();
            }
        }

        @Test
        @DisplayName("Should not failover to unhealthy endpoints")
        void shouldNotFailoverToUnhealthyEndpoints() {
            // Given: MultiDbClient with health check supplier and multiple endpoints
            HealthCheckStrategy.Config config = HealthCheckStrategy.Config.builder().interval(1) // 1ms for fast testing
                    .timeout(10).numProbes(1).delayInBetweenProbes(1).build();

            TestHealthCheckStrategy testStrategy = new TestHealthCheckStrategy(config);
            HealthCheckStrategySupplier supplier = (uri, options) -> testStrategy;

            // Create 3 endpoints: uri1 (weight 1.0), uri2 (weight 0.5), uri3 (weight 0.25)
            DatabaseConfig config1 = new DatabaseConfig(uri1, 1.0f, null, null, supplier);
            DatabaseConfig config2 = new DatabaseConfig(uri2, 0.5f, null, null, supplier);
            DatabaseConfig config3 = new DatabaseConfig(uri3, 0.25f, null, null, supplier);

            // When: Create MultiDbClient and connect
            MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2, config3));
            StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

            try {

                // Then: Verify current endpoint is uri1 (highest weight)
                assertThat(connection.getCurrentEndpoint()).isEqualTo(uri1);

                // And: Verify health status for all endpoints to be HEALTHY
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.HEALTHY);
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.HEALTHY);
                    assertThat(connection.getHealthStatus(uri3)).isEqualTo(HealthStatus.HEALTHY);
                });

                // When: Mark uri2 as UNHEALTHY (second highest weight)
                testStrategy.setHealthStatus(uri2, HealthStatus.UNHEALTHY);

                // Then: Wait for health status to update
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.UNHEALTHY);
                });

                // When: Trigger failover from uri1 by marking it UNHEALTHY
                testStrategy.setHealthStatus(uri1, HealthStatus.UNHEALTHY);

                // Then: Wait for health check to detect failure
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.UNHEALTHY);
                });

                // And: Verify failover skips UNHEALTHY uri2 and goes to HEALTHY uri3
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getCurrentEndpoint()).isEqualTo(uri3);
                });

                // And: Verify we're actually connected to uri3 by comparing run_id
                awaitAtMost().untilAsserted(() -> {
                    String actualRunId = extractRunId(connection.sync().info("server"));
                    assertThat(actualRunId).isEqualTo(expectedRunIdUri3);
                });

                // And: Verify connection still works on uri3
                assertThat(connection.sync().ping()).isEqualTo("PONG");

            } finally {
                connection.close();
                testClient.shutdown();
            }
        }

        @Test
        @DisplayName("Should trigger failover via circuit breaker even when health check returns HEALTHY")
        void shouldCoordinateHealthCheckAndCircuitBreaker() {
            // Given: MultiDbClient with both health check and circuit breaker configured
            // Configure health check with fast intervals
            HealthCheckStrategy.Config healthCheckConfig = HealthCheckStrategy.Config.builder().interval(1) // 1ms
                    .timeout(10).numProbes(1).delayInBetweenProbes(1).build();

            TestHealthCheckStrategy testStrategy = new TestHealthCheckStrategy(healthCheckConfig);
            HealthCheckStrategySupplier supplier = (uri, options) -> testStrategy;

            // Configure circuit breaker with low thresholds for fast testing
            CircuitBreaker.CircuitBreakerConfig cbConfig = new CircuitBreaker.CircuitBreakerConfig(50.0f, // 50% failure rate
                                                                                                          // threshold
                    2, // minimum 2 failures
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());

            DatabaseConfig config1 = new DatabaseConfig(uri1, 1.0f, null, cbConfig, supplier);
            DatabaseConfig config2 = new DatabaseConfig(uri2, 0.5f, null, cbConfig, supplier);

            // When: Create MultiDbClient and connect
            MultiDbClient testClient = MultiDbClient.create(Arrays.asList(config1, config2));
            StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

            try {
                // Then: Wait for all endpoints to be HEALTHY
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.HEALTHY);
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.HEALTHY);
                });

                // And: Verify current endpoint is uri1 (higher weight)
                assertThat(connection.getCurrentEndpoint()).isEqualTo(uri1);

                // And: Verify we're connected to uri1 by comparing run_id
                String actualRunIdBeforeFailover = extractRunId(connection.sync().info("server"));
                assertThat(actualRunIdBeforeFailover).isEqualTo(expectedRunIdUri1);

                // And: Verify circuit breaker is CLOSED initially
                CircuitBreaker cb1 = connection.getCircuitBreaker(uri1);
                assertThat(cb1.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);

                // When: Record failures to trigger circuit breaker (need 2 failures with 50% rate)
                // Record 2 failures and 1 success = 66% failure rate, which exceeds 50% threshold
                cb1.recordFailure();
                cb1.recordFailure();
                cb1.recordSuccess();

                // Then: Circuit breaker should transition to OPEN
                awaitAtMost().untilAsserted(() -> {
                    assertThat(cb1.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
                });

                // And: Health check still returns HEALTHY (this is the key test point)
                assertThat(connection.getHealthStatus(uri1)).isEqualTo(HealthStatus.HEALTHY);

                // And: Failover should still be triggered by circuit breaker (not health check)
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getCurrentEndpoint()).isEqualTo(uri2);
                });

                // And: Verify we're actually connected to uri2 by comparing run_id
                awaitAtMost().untilAsserted(() -> {
                    String actualRunIdAfterFailover = extractRunId(connection.sync().info("server"));
                    assertThat(actualRunIdAfterFailover).isEqualTo(expectedRunIdUri2);
                });

                // And: Verify connection still works on uri2
                assertThat(connection.sync().ping()).isEqualTo("PONG");

            } finally {
                connection.close();
                testClient.shutdown();
            }
        }

    }

    /**
     * Extract run_id from INFO SERVER output. The run_id is a unique identifier for each Redis server instance.
     *
     * @param info the INFO SERVER output
     * @return the run_id value, or empty string if not found
     */
    private static String extractRunId(String info) {
        for (String line : info.split("\r?\n")) {
            if (line.startsWith("run_id:")) {
                return line.substring("run_id:".length()).trim();
            }
        }
        return "";
    }

    @Nested
    @DisplayName("Dynamic Database Management")
    class DynamicDatabaseManagementTests {

        @Test
        @DisplayName("Should create health check when adding new database")
        void shouldCreateHealthCheckOnAddDatabase() {
            // Given: MultiDbClient with health check supplier for initial database
            HealthCheckStrategy.Config config = HealthCheckStrategy.Config.builder().interval(1) // 1ms interval
                    .timeout(10).numProbes(1).delayInBetweenProbes(1).build();

            TestHealthCheckStrategy testStrategy = new TestHealthCheckStrategy(config);
            HealthCheckStrategySupplier supplier = (uri, options) -> testStrategy;

            DatabaseConfig config1 = new DatabaseConfig(uri1, 1.0f, null, null, supplier);

            MultiDbClient testClient = MultiDbClient.create(Collections.singletonList(config1));
            StatefulRedisMultiDbConnection<String, String> connection = testClient.connect();

            try {
                // When: Add a new database dynamically with health check supplier
                DatabaseConfig config2 = new DatabaseConfig(uri2, 0.5f, null, null, supplier);
                connection.addDatabase(config2);

                // Then: Health check should be created and started for the new database
                // Wait for health check to run and status to become HEALTHY
                awaitAtMost().untilAsserted(() -> {
                    assertThat(connection.getHealthStatus(uri2)).isEqualTo(HealthStatus.HEALTHY);
                });

                // And: Verify health check was actually called for the new database
                awaitAtMost().untilAsserted(() -> {
                    assertThat(testStrategy.getHealthCheckCallCount(uri2)).isGreaterThan(0);
                });

            } finally {
                connection.close();
                testClient.shutdown();
            }
        }

        @Test
        @DisplayName("Should stop health check when removing database")
        @Disabled("Not implemented yet")
        void shouldStopHealthCheckOnRemoveDatabase() {
            // TODO: Implement test
            // - Create MultiDbClient with health check supplier
            // - Connect
            // - Remove a database
            // - Verify health check is stopped and cleaned up
        }

    }

    /**
     * Helper method to create an Awaitility condition builder with consistent timeout and poll interval. Use this instead of
     * {@code waitAtMost(AWAIT_TIMEOUT)} to ensure all async assertions use the same poll interval.
     *
     * @return Awaitility condition builder configured with AWAIT_TIMEOUT and POLL_INTERVAL
     */
    private static ConditionFactory awaitAtMost() {
        return await().atMost(AWAIT_TIMEOUT).pollInterval(POLL_INTERVAL);
    }

    /**
     * Test implementation of HealthCheckStrategy with controllable health status. Used for testing health check configuration,
     * lifecycle, and status transitions.
     * <p>
     * By default, returns HEALTHY for all endpoints. Use {@link #setHealthStatus(RedisURI, HealthStatus)} to control the health
     * status returned for specific endpoints.
     * </p>
     * <p>
     * Supports:
     * <ul>
     * <li>simulating timeouts by introducing delays longer than the configured timeout.</li>
     * <li>health check call count and last call time per endpoint for verification.</li>
     * </ul>
     * </p>
     *
     */
    static class TestHealthCheckStrategy implements HealthCheckStrategy {

        private final Config config;

        private final Map<RedisURI, AtomicReference<HealthStatus>> endpointHealth = new HashMap<>();

        private final Map<RedisURI, AtomicInteger> healthCheckCallCount = new HashMap<>();

        private final Map<RedisURI, AtomicLong> lastHealthCheckTime = new HashMap<>();

        private final Map<RedisURI, AtomicReference<Delay>> healthCheckDelayMs = new HashMap<>();

        interface Delay {

            void delay();

            static Delay None() {
                return () -> {
                };
            };

            static Delay Fixed(int delayMs) {
                return () -> {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Health check interrupted", e);
                    }
                };
            }

            static Delay Await(CountDownLatch latch) {
                return () -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Health check interrupted", e);
                    }
                };
            }

        }

        TestHealthCheckStrategy(Config config) {
            this.config = config;
        }

        @Override
        public int getInterval() {
            return config.getInterval();
        }

        @Override
        public int getTimeout() {
            return config.getTimeout();
        }

        @Override
        public HealthStatus doHealthCheck(RedisURI endpoint) {
            // Record the health check call
            healthCheckCallCount.computeIfAbsent(endpoint, k -> new AtomicInteger(0)).incrementAndGet();
            lastHealthCheckTime.computeIfAbsent(endpoint, k -> new AtomicLong(0)).set(System.currentTimeMillis());

            // Simulate delay if configured (for timeout testing)
            Delay delay = healthCheckDelayMs.getOrDefault(endpoint, new AtomicReference<>(Delay.None())).get();
            delay.delay();

            // Return the current health status for the endpoint (default: HEALTHY)
            return endpointHealth.computeIfAbsent(endpoint, k -> new AtomicReference<>(HealthStatus.HEALTHY)).get();
        }

        @Override
        public int getNumProbes() {
            return config.getNumProbes();
        }

        @Override
        public ProbingPolicy getPolicy() {
            return config.getPolicy();
        }

        @Override
        public int getDelayInBetweenProbes() {
            return config.getDelayInBetweenProbes();
        }

        /**
         * Set the health status for a specific endpoint. This allows controlled testing of health status transitions.
         *
         * @param endpoint the endpoint URI
         * @param status the health status to return for this endpoint
         */
        public void setHealthStatus(RedisURI endpoint, HealthStatus status) {
            endpointHealth.computeIfAbsent(endpoint, k -> new AtomicReference<>(HealthStatus.HEALTHY)).set(status);
        }

        /**
         * Get the number of times doHealthCheck was called for a specific endpoint.
         *
         * @param endpoint the endpoint URI
         * @return the number of health check calls
         */
        public int getHealthCheckCallCount(RedisURI endpoint) {
            java.util.concurrent.atomic.AtomicInteger count = healthCheckCallCount.get(endpoint);
            return count != null ? count.get() : 0;
        }

        /**
         * Get the timestamp of the last health check for a specific endpoint.
         *
         * @param endpoint the endpoint URI
         * @return the timestamp in milliseconds, or 0 if never called
         */
        public long getLastHealthCheckTime(RedisURI endpoint) {
            java.util.concurrent.atomic.AtomicLong time = lastHealthCheckTime.get(endpoint);
            return time != null ? time.get() : 0;
        }

        /**
         * Set a delay to simulate slow health checks (for timeout testing).
         *
         * @param endpoint the endpoint URI
         * @param delay the delay to apply
         */
        public void setHealthCheckDelay(RedisURI endpoint, Delay delay) {
            healthCheckDelayMs.compute(endpoint, (k, v) -> {
                if (v == null) {
                    v = new AtomicReference<>();
                }
                v.set(delay);
                return v;
            });
        }

    }

}
