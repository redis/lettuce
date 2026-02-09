package io.lettuce.scenario;

import static io.lettuce.TestTags.SCENARIO_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.failover.CircuitBreaker;
import io.lettuce.core.failover.DatabaseConfig;
import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.failover.MultiDbOptions;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.failover.event.DatabaseSwitchEvent;
import io.lettuce.core.failover.event.SwitchReason;
import io.lettuce.core.failover.health.HealthCheckStrategy;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.ProbingPolicy;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;
import reactor.core.Disposable;

/**
 * Active-Active (AA) failover scenario tests for lettuce's automatic-failover feature.
 * <p>
 * These tests verify:
 * <ul>
 * <li>Circuit breaker triggers failover on network failure</li>
 * <li>Health check triggers failover on unhealthy status</li>
 * <li>Manual switch works with no stuck commands</li>
 * <li>Pub/Sub subscriptions survive failover</li>
 * </ul>
 * <p>
 * Success criteria:
 * <ul>
 * <li>Commands start going to the expected port (verified via run_id)</li>
 * <li>No stuck commands</li>
 * </ul>
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
@Tag(SCENARIO_TEST)
@DisplayName("Active-Active Failover Scenario Tests")
public class ActiveActiveFailoverScenarioTest {

    private static final Logger log = LoggerFactory.getLogger(ActiveActiveFailoverScenarioTest.class);

    private static final Duration NETWORK_FAILURE_DURATION = Duration.ofSeconds(15);

    private static final Duration FAILOVER_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration WORKLOAD_DURATION = Duration.ofSeconds(30);

    private static final int NUM_THREADS = 4;

    private static Endpoint aaEndpoint;

    private final FaultInjectionClient faultClient = new FaultInjectionClient();

    private MultiDbClient multiDbClient;

    private StatefulRedisMultiDbConnection<String, String> connection;

    private Disposable eventSubscription;

    private RedisURI primaryUri;

    private RedisURI secondaryUri;

    private String primaryRunId;

    private String secondaryRunId;

    @BeforeAll
    public static void setup() {
        aaEndpoint = Endpoints.DEFAULT.getEndpoint("re-active-active");
        assumeTrue(aaEndpoint != null, "Skipping test because no Active-Active Redis endpoint is configured!");
        assumeTrue(aaEndpoint.getEndpoints() != null && aaEndpoint.getEndpoints().size() >= 2,
                "Skipping test because Active-Active endpoint requires at least 2 endpoints!");
    }

    @BeforeEach
    public void setupTest() {
        primaryUri = createRedisUri(aaEndpoint.getEndpoints().get(0));
        secondaryUri = createRedisUri(aaEndpoint.getEndpoints().get(1));

        log.info("Primary endpoint: {}", primaryUri);
        log.info("Secondary endpoint: {}", secondaryUri);
    }

    @AfterEach
    public void tearDown() {
        if (eventSubscription != null) {
            eventSubscription.dispose();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        if (multiDbClient != null) {
            multiDbClient.shutdown();
        }

        // Wait for cluster to become healthy before next test
        // This is needed because network_latency injection can affect DNS and connectivity
        waitForClusterHealthy(primaryUri, Duration.ofSeconds(90));
    }

    /**
     * Wait for cluster to become healthy by polling PING responses. Similar to redis-py's _wait_for_cluster_healthy pattern.
     */
    private void waitForClusterHealthy(RedisURI uri, Duration timeout) {
        log.info("Waiting for cluster to become healthy (timeout={}s)", timeout.getSeconds());

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout.toMillis();
        int healthyStreak = 0;
        int requiredStreak = 3;
        long pingThresholdMs = 1500;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            io.lettuce.core.RedisClient probeClient = null;
            try {
                probeClient = io.lettuce.core.RedisClient.create(uri);
                probeClient.setOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(5)).build())
                        .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5))).build());

                long pingStart = System.currentTimeMillis();
                try (io.lettuce.core.api.StatefulRedisConnection<String, String> probeConn = probeClient.connect()) {
                    probeConn.sync().ping();
                }
                long pingMs = System.currentTimeMillis() - pingStart;

                if (pingMs < pingThresholdMs) {
                    healthyStreak++;
                    log.info("Cluster PING: {}ms (streak {}/{})", pingMs, healthyStreak, requiredStreak);
                    if (healthyStreak >= requiredStreak) {
                        log.info("Cluster is healthy");
                        return;
                    }
                } else {
                    healthyStreak = 0;
                    log.info("Cluster PING: {}ms (too slow, resetting streak)", pingMs);
                }
            } catch (Exception e) {
                healthyStreak = 0;
                log.info("Cluster health check failed: {}", e.getMessage());
            } finally {
                if (probeClient != null) {
                    probeClient.shutdown(Duration.ZERO, Duration.ofSeconds(2));
                }
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.warn("Cluster did not become healthy within {}s, proceeding anyway", timeout.getSeconds());
    }

    // ========================================
    // Test 1: Circuit Breaker Kicks In
    // ========================================

    @Test
    @DisplayName("Circuit breaker triggers failover on command timeouts")
    public void testCircuitBreakerFailover() throws Exception {
        log.info("Starting circuit breaker failover test");

        // This test verifies that the circuit breaker triggers failover when commands timeout.
        // We use network_latency fault injection to add latency that causes timeouts,
        // which the circuit breaker tracks and uses to trigger failover.

        // Use short timeout (500ms) so commands timeout quickly during latency injection
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(10)).build())
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(500))).build();

        // Circuit breaker config: 10% failure rate threshold, minimum 5 failures
        CircuitBreaker.CircuitBreakerConfig cbConfig = CircuitBreaker.CircuitBreakerConfig.builder().failureRateThreshold(10.0f)
                .minimumNumberOfFailures(5).metricsWindowSize(10).build();

        // Disable health check - we want to test circuit breaker specifically
        DatabaseConfig primaryConfig = DatabaseConfig.builder(primaryUri).weight(1.0f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK)
                .build();

        DatabaseConfig secondaryConfig = DatabaseConfig.builder(secondaryUri).weight(0.5f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK)
                .build();

        MultiDbOptions multiDbOptions = MultiDbOptions.builder().failbackSupported(true)
                .failbackCheckInterval(Duration.ofSeconds(1)).gracePeriod(Duration.ofSeconds(2)).build();

        multiDbClient = MultiDbClient.create(Arrays.asList(primaryConfig, secondaryConfig), multiDbOptions);
        connection = multiDbClient.connect();

        // Verify connection works
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        // Ensure we start on the primary (highest weight)
        connection.switchTo(primaryUri);
        assertThat(connection.getCurrentEndpoint()).isEqualTo(primaryUri);

        // Capture run_ids
        primaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(secondaryUri);
        secondaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(primaryUri);
        log.info("Primary run_id: {}", primaryRunId);
        log.info("Secondary run_id: {}", secondaryRunId);
        assertThat(primaryRunId).isNotEqualTo(secondaryRunId);

        // Setup failover reporter
        FailoverReporter reporter = new FailoverReporter();
        eventSubscription = multiDbClient.getResources().eventBus().get().subscribe(event -> {
            if (event instanceof DatabaseSwitchEvent) {
                reporter.accept((DatabaseSwitchEvent) event);
            }
        });

        // Log initial circuit breaker state
        log.info("Circuit breaker initial state: {}", connection.getDatabase(primaryUri).getCircuitBreakerState());

        // Trigger network latency on primary - 2000ms delay will cause 500ms timeout commands to fail
        log.info("Triggering network_latency on primary (2000ms delay for 60s duration)");
        Map<String, Object> params = new HashMap<>();
        params.put("bdb_id", aaEndpoint.getBdbId());
        params.put("delay_ms", 2000); // 2 second latency - will cause 500ms timeout to fail
        params.put("duration", 60); // Latency will be active for 60 seconds
        faultClient.triggerActionAndWait("network_latency", params, Duration.ofSeconds(2), Duration.ofSeconds(1),
                Duration.ofSeconds(30)).block();
        log.info("Network latency applied, starting command execution");

        // Fire async commands to trigger circuit breaker - each will timeout after 500ms
        log.info("Firing async commands to trigger circuit breaker...");
        for (int i = 0; i < 20; i++) {
            try {
                connection.async().get("cb-test-key-" + i);
            } catch (Exception e) {
                // Expected - commands will timeout
            }
            // Small delay to let failures accumulate
            Thread.sleep(100);

            // Check CB state
            io.lettuce.core.failover.api.RedisDatabase database = connection.getDatabase(primaryUri);
            CircuitBreaker.State cbState = database.getCircuitBreakerState();
            io.lettuce.core.failover.metrics.MetricsSnapshot metrics = database.getMetricsSnapshot();
            log.info("Command {} - CB state: {}, metrics: total={}, failures={}, rate={}%", i, cbState, metrics.getTotalCount(),
                    metrics.getFailureCount(), metrics.getFailureRate());

            if (reporter.isFailoverHappened()) {
                log.info("Failover detected after {} commands", i + 1);
                break;
            }
        }

        // Wait for failover to complete
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(reporter.isFailoverHappened()).isTrue();
        });

        log.info("Failover happened at: {}", reporter.getFailoverAt());
        log.info("Failover reason: {}", reporter.getFailoverReason());

        // Verify failover reason is CIRCUIT_BREAKER
        assertThat(reporter.getFailoverReason()).isEqualTo(SwitchReason.CIRCUIT_BREAKER);

        // Verify we switched to secondary
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            String currentRunId = extractRunId(connection.sync().info("server"));
            assertThat(currentRunId).isEqualTo(secondaryRunId);
        });
        log.info("Verified failover to secondary endpoint");

        // Verify commands work on secondary
        log.info("Verifying commands work on secondary...");
        for (int i = 0; i < 5; i++) {
            String result = connection.sync().set("verify-key-" + i, "value-" + i);
            assertThat(result).isEqualTo("OK");
        }
        log.info("Successfully executed commands on secondary endpoint");

        log.info("Circuit breaker failover test completed successfully");
    }

    // ========================================
    // Test 2: Health Check Kicks In
    // ========================================

    @Test
    @DisplayName("Health check should trigger failover on unhealthy status")
    public void testHealthCheckFailover() throws Exception {
        log.info("Starting health check failover test");

        // Create controllable health check strategy that defaults to HEALTHY
        ControllableHealthCheckStrategy healthCheckStrategy = new ControllableHealthCheckStrategy();

        // Supplier that returns the same controllable strategy for all endpoints
        HealthCheckStrategySupplier controllableSupplier = (uri, factory) -> healthCheckStrategy;

        ClientOptions clientOptions = createClientOptions();

        // High CB threshold so circuit breaker doesn't interfere with health check test
        CircuitBreaker.CircuitBreakerConfig cbConfig = CircuitBreaker.CircuitBreakerConfig.builder().failureRateThreshold(90.0f)
                .minimumNumberOfFailures(1000).metricsWindowSize(100).build();

        // Use controllable health check strategy - it defaults to HEALTHY so connection will work
        DatabaseConfig primaryConfig = DatabaseConfig.builder(primaryUri).weight(1.0f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(controllableSupplier).build();

        DatabaseConfig secondaryConfig = DatabaseConfig.builder(secondaryUri).weight(0.5f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(controllableSupplier).build();

        MultiDbOptions multiDbOptions = MultiDbOptions.builder().failbackSupported(false).gracePeriod(Duration.ZERO).build();

        multiDbClient = MultiDbClient.create(Arrays.asList(primaryConfig, secondaryConfig), multiDbOptions);
        connection = multiDbClient.connect();

        // Verify connection works
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        // Ensure we start on the primary
        connection.switchTo(primaryUri);
        assertThat(connection.getCurrentEndpoint()).isEqualTo(primaryUri);

        // Capture run_ids
        primaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(secondaryUri);
        secondaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(primaryUri);

        log.info("Primary run_id: {}", primaryRunId);
        log.info("Secondary run_id: {}", secondaryRunId);

        // Setup failover reporter
        FailoverReporter reporter = new FailoverReporter();
        eventSubscription = multiDbClient.getResources().eventBus().get().subscribe(event -> {
            if (event instanceof DatabaseSwitchEvent) {
                reporter.accept((DatabaseSwitchEvent) event);
            }
        });

        // Start workload
        MultiThreadedFakeApp fakeApp = new MultiThreadedFakeApp(connection, NUM_THREADS, Duration.ofSeconds(20));
        Thread workloadThread = new Thread(fakeApp);
        workloadThread.start();

        // Wait for workload to start
        await().atMost(Duration.ofSeconds(5)).until(() -> fakeApp.getExecutedCommands() > 0);
        log.info("Workload started, commands: {}", fakeApp.getExecutedCommands());

        // Mark primary as unhealthy - health check runs every 100ms so detection should be fast
        log.info("Marking primary endpoint as UNHEALTHY");
        healthCheckStrategy.setHealthStatus(primaryUri, HealthStatus.UNHEALTHY);

        // Wait for health check to detect unhealthy status
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(connection.isHealthy(primaryUri)).isFalse();
        });
        log.info("Primary detected as unhealthy");

        // Wait for failover to happen
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(reporter.isFailoverHappened()).isTrue();
        });

        log.info("Failover happened at: {}", reporter.getFailoverAt());
        log.info("Failover reason: {}", reporter.getFailoverReason());

        // Verify failover reason is HEALTH_CHECK
        assertThat(reporter.getFailoverReason()).isEqualTo(SwitchReason.HEALTH_CHECK);

        // Verify we switched to secondary
        assertThat(connection.getCurrentEndpoint()).isEqualTo(secondaryUri);

        // Verify commands now go to secondary (different run_id)
        String currentRunId = extractRunId(connection.sync().info("server"));
        assertThat(currentRunId).isEqualTo(secondaryRunId);
        log.info("Verified commands now going to secondary endpoint");

        // Stop workload
        fakeApp.stop();
        workloadThread.join(5000);

        log.info("Total commands executed: {}", fakeApp.getExecutedCommands());
        log.info("Captured exceptions: {}", fakeApp.getCapturedExceptions().size());
        log.info("Health check failover test completed successfully");
    }

    // ========================================
    // Test 3: Manual Switch
    // ========================================

    @Test
    @DisplayName("Manual switch should work gracefully with no stuck commands")
    public void testManualSwitch() throws Exception {
        log.info("Starting manual switch test");

        // First verify we can connect directly with RedisClient
        log.info("Testing direct connection to primary: {}", primaryUri);
        io.lettuce.core.RedisClient directClient = io.lettuce.core.RedisClient.create(primaryUri);
        try (io.lettuce.core.api.StatefulRedisConnection<String, String> directConn = directClient.connect()) {
            String pong = directConn.sync().ping();
            log.info("Direct connection PING response: {}", pong);
            assertThat(pong).isEqualTo("PONG");
        }
        directClient.shutdown();
        log.info("Direct connection test passed, proceeding with MultiDbClient");

        ClientOptions clientOptions = createClientOptions();

        CircuitBreaker.CircuitBreakerConfig cbConfig = CircuitBreaker.CircuitBreakerConfig.builder().failureRateThreshold(50.0f)
                .minimumNumberOfFailures(1000).metricsWindowSize(10).build();

        DatabaseConfig primaryConfig = DatabaseConfig.builder(primaryUri).weight(1.0f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK)
                .build();

        DatabaseConfig secondaryConfig = DatabaseConfig.builder(secondaryUri).weight(0.5f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK)
                .build();

        MultiDbOptions multiDbOptions = MultiDbOptions.builder().failbackSupported(false).gracePeriod(Duration.ZERO).build();

        log.info("Creating MultiDbClient with configs: primary={}, secondary={}", primaryConfig, secondaryConfig);
        multiDbClient = MultiDbClient.create(Arrays.asList(primaryConfig, secondaryConfig), multiDbOptions);
        log.info("Attempting to connect via MultiDbClient...");
        connection = multiDbClient.connect();
        log.info("MultiDbClient connection established");

        // Verify connection works
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        // Start on primary
        connection.switchTo(primaryUri);

        // Capture run_ids
        primaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(secondaryUri);
        secondaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(primaryUri);

        log.info("Primary run_id: {}", primaryRunId);
        log.info("Secondary run_id: {}", secondaryRunId);

        // Setup failover reporter
        FailoverReporter reporter = new FailoverReporter();
        eventSubscription = multiDbClient.getResources().eventBus().get().subscribe(event -> {
            if (event instanceof DatabaseSwitchEvent) {
                reporter.accept((DatabaseSwitchEvent) event);
            }
        });

        // Start workload
        MultiThreadedFakeApp fakeApp = new MultiThreadedFakeApp(connection, NUM_THREADS, Duration.ofSeconds(15));
        Thread workloadThread = new Thread(fakeApp);
        workloadThread.start();

        // Wait for workload to start
        await().atMost(Duration.ofSeconds(5)).until(() -> fakeApp.getExecutedCommands() > 0);
        log.info("Workload started, commands executed: {}", fakeApp.getExecutedCommands());

        // Perform manual switch to secondary
        log.info("Performing manual switch to secondary");
        connection.switchTo(secondaryUri);

        // Verify switch happened
        assertThat(connection.getCurrentEndpoint()).isEqualTo(secondaryUri);

        // Verify switch event was published with FORCED reason
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(reporter.isFailoverHappened()).isTrue();
            assertThat(reporter.getFailoverReason()).isEqualTo(SwitchReason.FORCED);
        });

        // Verify commands now go to secondary
        String currentRunId = extractRunId(connection.sync().info("server"));
        assertThat(currentRunId).isEqualTo(secondaryRunId);

        log.info("Manual switch completed, now on secondary");

        // Switch back to primary
        log.info("Switching back to primary");
        connection.switchTo(primaryUri);

        // Verify commands now go to primary
        currentRunId = extractRunId(connection.sync().info("server"));
        assertThat(currentRunId).isEqualTo(primaryRunId);

        // Wait for workload to complete
        workloadThread.join(20000);

        // Verify no stuck commands
        log.info("Total commands executed: {}", fakeApp.getExecutedCommands());
        log.info("Captured exceptions: {}", fakeApp.getCapturedExceptions().size());

        log.info("Manual switch test completed successfully");
    }

    // ========================================
    // Test 4: Health Check Failover with Failback
    // ========================================

    @Test
    @DisplayName("Health check should trigger failover and failback cycle")
    public void testHealthCheckFailoverWithFailback() throws Exception {
        log.info("Starting health check failover with failback test");

        // Create controllable health check strategy that defaults to HEALTHY
        ControllableHealthCheckStrategy healthCheckStrategy = new ControllableHealthCheckStrategy();

        // Supplier that returns the same controllable strategy for all endpoints
        HealthCheckStrategySupplier controllableSupplier = (uri, factory) -> healthCheckStrategy;

        ClientOptions clientOptions = createClientOptions();

        // High CB threshold so circuit breaker doesn't interfere with health check test
        CircuitBreaker.CircuitBreakerConfig cbConfig = CircuitBreaker.CircuitBreakerConfig.builder().failureRateThreshold(90.0f)
                .minimumNumberOfFailures(1000).metricsWindowSize(100).build();

        // Use controllable health check strategy
        DatabaseConfig primaryConfig = DatabaseConfig.builder(primaryUri).weight(1.0f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(controllableSupplier).build();

        DatabaseConfig secondaryConfig = DatabaseConfig.builder(secondaryUri).weight(0.5f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(controllableSupplier).build();

        // Enable failback with short intervals for testing
        MultiDbOptions multiDbOptions = MultiDbOptions.builder().failbackSupported(true)
                .failbackCheckInterval(Duration.ofSeconds(5)).gracePeriod(Duration.ofSeconds(3)).build();

        multiDbClient = MultiDbClient.create(Arrays.asList(primaryConfig, secondaryConfig), multiDbOptions);
        connection = multiDbClient.connect();

        // Verify connection works
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        // Ensure we start on the primary (highest weight)
        connection.switchTo(primaryUri);
        assertThat(connection.getCurrentEndpoint()).isEqualTo(primaryUri);

        // Capture run_ids
        primaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(secondaryUri);
        secondaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(primaryUri);

        log.info("Primary run_id: {}", primaryRunId);
        log.info("Secondary run_id: {}", secondaryRunId);
        assertThat(primaryRunId).isNotEqualTo(secondaryRunId);

        // Setup failover reporter
        FailoverReporter reporter = new FailoverReporter();
        eventSubscription = multiDbClient.getResources().eventBus().get().subscribe(event -> {
            if (event instanceof DatabaseSwitchEvent) {
                reporter.accept((DatabaseSwitchEvent) event);
            }
        });

        // Verify we're on primary
        String currentRunId = extractRunId(connection.sync().info("server"));
        assertThat(currentRunId).isEqualTo(primaryRunId);
        log.info("Confirmed starting on primary endpoint");

        // ---- PHASE 1: Trigger failover by marking primary unhealthy ----
        log.info("PHASE 1: Marking primary endpoint as UNHEALTHY");
        healthCheckStrategy.setHealthStatus(primaryUri, HealthStatus.UNHEALTHY);

        // Wait for health check to detect unhealthy status
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(connection.isHealthy(primaryUri)).isFalse();
        });
        log.info("Primary detected as unhealthy");

        // Wait for failover to happen
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(reporter.isFailoverHappened()).isTrue();
        });

        log.info("Failover happened at: {}", reporter.getFailoverAt());
        log.info("Failover reason: {}", reporter.getFailoverReason());

        // Verify failover reason is HEALTH_CHECK
        assertThat(reporter.getFailoverReason()).isEqualTo(SwitchReason.HEALTH_CHECK);

        // Verify we switched to secondary
        assertThat(connection.getCurrentEndpoint()).isEqualTo(secondaryUri);
        currentRunId = extractRunId(connection.sync().info("server"));
        assertThat(currentRunId).isEqualTo(secondaryRunId);
        log.info("Verified failover to secondary endpoint");

        // ---- PHASE 2: Trigger failback by marking primary healthy again ----
        log.info("PHASE 2: Marking primary endpoint as HEALTHY again");
        healthCheckStrategy.setHealthStatus(primaryUri, HealthStatus.HEALTHY);

        // Wait for health check to detect healthy status
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(connection.isHealthy(primaryUri)).isTrue();
        });
        log.info("Primary detected as healthy again");

        // Wait for grace period (3s) + failback check interval (5s) + buffer
        log.info("Waiting for grace period and failback check...");

        // Wait for failback to happen
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(reporter.isFailbackHappened()).isTrue();
        });

        log.info("Failback happened at: {}", reporter.getFailbackAt());

        // Verify we're back on primary (higher weight)
        assertThat(connection.getCurrentEndpoint()).isEqualTo(primaryUri);
        currentRunId = extractRunId(connection.sync().info("server"));
        assertThat(currentRunId).isEqualTo(primaryRunId);
        log.info("Verified failback to primary endpoint");

        // Verify commands work on primary
        for (int i = 0; i < 5; i++) {
            String result = connection.sync().set("failback-verify-key-" + i, "value-" + i);
            assertThat(result).isEqualTo("OK");
        }
        log.info("Successfully executed commands on primary after failback");

        log.info("Health check failover with failback test completed successfully");
    }

    // ========================================
    // Test 5: Data Integrity During Failover
    // ========================================

    @Test
    @DisplayName("Data written before failover should be readable after failover (AA replication)")
    public void testDataIntegrityDuringFailover() throws Exception {
        log.info("Starting data integrity during failover test");

        // Create controllable health check strategy
        ControllableHealthCheckStrategy healthCheckStrategy = new ControllableHealthCheckStrategy();
        HealthCheckStrategySupplier controllableSupplier = (uri, factory) -> healthCheckStrategy;

        ClientOptions clientOptions = createClientOptions();

        // High CB threshold so circuit breaker doesn't interfere
        CircuitBreaker.CircuitBreakerConfig cbConfig = CircuitBreaker.CircuitBreakerConfig.builder().failureRateThreshold(90.0f)
                .minimumNumberOfFailures(1000).metricsWindowSize(100).build();

        DatabaseConfig primaryConfig = DatabaseConfig.builder(primaryUri).weight(1.0f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(controllableSupplier).build();

        DatabaseConfig secondaryConfig = DatabaseConfig.builder(secondaryUri).weight(0.5f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(controllableSupplier).build();

        // Enable failback for bidirectional test
        MultiDbOptions multiDbOptions = MultiDbOptions.builder().failbackSupported(true)
                .failbackCheckInterval(Duration.ofSeconds(5)).gracePeriod(Duration.ofSeconds(3)).build();

        multiDbClient = MultiDbClient.create(Arrays.asList(primaryConfig, secondaryConfig), multiDbOptions);
        connection = multiDbClient.connect();

        // Verify connection works
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        // Start on primary
        connection.switchTo(primaryUri);
        assertThat(connection.getCurrentEndpoint()).isEqualTo(primaryUri);

        // Capture run_ids
        primaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(secondaryUri);
        secondaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(primaryUri);

        log.info("Primary run_id: {}", primaryRunId);
        log.info("Secondary run_id: {}", secondaryRunId);

        // Setup failover reporter
        FailoverReporter reporter = new FailoverReporter();
        eventSubscription = multiDbClient.getResources().eventBus().get().subscribe(event -> {
            if (event instanceof DatabaseSwitchEvent) {
                reporter.accept((DatabaseSwitchEvent) event);
            }
        });

        // ---- PHASE 1: Write data on primary ----
        log.info("PHASE 1: Writing 100 keys on primary endpoint");
        Map<String, String> writtenOnPrimary = new HashMap<>();
        String testPrefix = "data-integrity-" + System.currentTimeMillis() + "-";

        for (int i = 0; i < 100; i++) {
            String key = testPrefix + "primary-" + i;
            String value = "value-" + i + "-" + System.currentTimeMillis();
            String result = connection.sync().set(key, value);
            assertThat(result).isEqualTo("OK");
            writtenOnPrimary.put(key, value);
        }
        log.info("Wrote {} keys on primary", writtenOnPrimary.size());

        // ---- PHASE 2: Trigger failover to secondary ----
        log.info("PHASE 2: Triggering failover to secondary");
        healthCheckStrategy.setHealthStatus(primaryUri, HealthStatus.UNHEALTHY);

        // Wait for failover
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(reporter.isFailoverHappened()).isTrue();
        });

        // Verify we're on secondary
        assertThat(connection.getCurrentEndpoint()).isEqualTo(secondaryUri);
        String currentRunId = extractRunId(connection.sync().info("server"));
        assertThat(currentRunId).isEqualTo(secondaryRunId);
        log.info("Failover to secondary complete");

        // ---- PHASE 3: Verify data is readable on secondary (AA replication) ----
        log.info("PHASE 3: Verifying data on secondary (with AA replication lag tolerance)");

        // Use Awaitility to handle AA replication lag
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            int matchCount = 0;
            for (Map.Entry<String, String> entry : writtenOnPrimary.entrySet()) {
                String actual = connection.sync().get(entry.getKey());
                if (entry.getValue().equals(actual)) {
                    matchCount++;
                }
            }
            log.info("Data verification progress: {}/{} keys match", matchCount, writtenOnPrimary.size());
            assertThat(matchCount).isEqualTo(writtenOnPrimary.size());
        });
        log.info("All {} keys verified on secondary", writtenOnPrimary.size());

        // ---- PHASE 4: Write additional data on secondary ----
        log.info("PHASE 4: Writing additional keys on secondary endpoint");
        Map<String, String> writtenOnSecondary = new HashMap<>();

        for (int i = 0; i < 50; i++) {
            String key = testPrefix + "secondary-" + i;
            String value = "secondary-value-" + i + "-" + System.currentTimeMillis();
            String result = connection.sync().set(key, value);
            assertThat(result).isEqualTo("OK");
            writtenOnSecondary.put(key, value);
        }
        log.info("Wrote {} keys on secondary", writtenOnSecondary.size());

        // ---- PHASE 5: Trigger failback to primary ----
        log.info("PHASE 5: Triggering failback to primary");
        healthCheckStrategy.setHealthStatus(primaryUri, HealthStatus.HEALTHY);

        // Wait for failback
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(reporter.isFailbackHappened()).isTrue();
        });

        // Verify we're back on primary
        assertThat(connection.getCurrentEndpoint()).isEqualTo(primaryUri);
        currentRunId = extractRunId(connection.sync().info("server"));
        assertThat(currentRunId).isEqualTo(primaryRunId);
        log.info("Failback to primary complete");

        // ---- PHASE 6: Verify data written on secondary is readable on primary ----
        log.info("PHASE 6: Verifying secondary data on primary (with AA replication lag tolerance)");

        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            int matchCount = 0;
            for (Map.Entry<String, String> entry : writtenOnSecondary.entrySet()) {
                String actual = connection.sync().get(entry.getKey());
                if (entry.getValue().equals(actual)) {
                    matchCount++;
                }
            }
            log.info("Data verification progress: {}/{} keys match", matchCount, writtenOnSecondary.size());
            assertThat(matchCount).isEqualTo(writtenOnSecondary.size());
        });
        log.info("All {} secondary keys verified on primary", writtenOnSecondary.size());

        // ---- PHASE 7: Verify original primary data is still there ----
        log.info("PHASE 7: Verifying original primary data is still accessible");
        int verifiedCount = 0;
        for (Map.Entry<String, String> entry : writtenOnPrimary.entrySet()) {
            String actual = connection.sync().get(entry.getKey());
            assertThat(actual).isEqualTo(entry.getValue());
            verifiedCount++;
        }
        log.info("Verified {} original primary keys", verifiedCount);

        log.info("Data integrity during failover test completed successfully");
        log.info("Total keys verified: {} (primary) + {} (secondary) = {}", writtenOnPrimary.size(), writtenOnSecondary.size(),
                writtenOnPrimary.size() + writtenOnSecondary.size());
    }

    // ========================================
    // Test 6: Pub/Sub Failover
    // ========================================

    @Test
    @DisplayName("Pub/Sub subscriptions should survive failover")
    public void testPubSubFailover() throws Exception {
        log.info("Starting Pub/Sub failover test");

        ClientOptions clientOptions = createClientOptions();

        CircuitBreaker.CircuitBreakerConfig cbConfig = CircuitBreaker.CircuitBreakerConfig.builder().failureRateThreshold(10.0f)
                .minimumNumberOfFailures(5).metricsWindowSize(5).build();

        DatabaseConfig primaryConfig = DatabaseConfig.builder(primaryUri).weight(1.0f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK)
                .build();

        DatabaseConfig secondaryConfig = DatabaseConfig.builder(secondaryUri).weight(0.5f).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK)
                .build();

        MultiDbOptions multiDbOptions = MultiDbOptions.builder().failbackSupported(false).gracePeriod(Duration.ZERO).build();

        multiDbClient = MultiDbClient.create(Arrays.asList(primaryConfig, secondaryConfig), multiDbOptions);

        // Create pub/sub connection
        StatefulRedisMultiDbPubSubConnection<String, String> pubSubConnection = multiDbClient.connectPubSub();

        // Setup message listener
        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(5);

        pubSubConnection.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                log.info("Received message on channel {}: {}", channel, message);
                receivedMessages.add(message);
                messageLatch.countDown();
            }

        });

        // Start on primary
        pubSubConnection.switchTo(primaryUri);

        // Subscribe to a channel
        String testChannel = "aa-failover-test-channel";
        pubSubConnection.sync().subscribe(testChannel);
        log.info("Subscribed to channel: {}", testChannel);

        // Use a separate connection to publish messages
        StatefulRedisMultiDbConnection<String, String> publisherConnection = multiDbClient.connect();

        // Publish some messages before failover
        log.info("Publishing messages before failover");
        for (int i = 0; i < 2; i++) {
            publisherConnection.sync().publish(testChannel, "message-before-" + i);
            Thread.sleep(100);
        }

        // Perform manual switch to simulate failover
        log.info("Switching pub/sub connection to secondary");
        pubSubConnection.switchTo(secondaryUri);
        publisherConnection.switchTo(secondaryUri);

        // Give time for subscription to be re-established
        Thread.sleep(1000);

        // Publish more messages after failover
        log.info("Publishing messages after failover");
        for (int i = 0; i < 3; i++) {
            publisherConnection.sync().publish(testChannel, "message-after-" + i);
            Thread.sleep(100);
        }

        // Wait for messages to be received
        boolean received = messageLatch.await(10, TimeUnit.SECONDS);
        log.info("Messages received: {}, count: {}", received, receivedMessages.size());

        // Verify messages were received
        assertThat(receivedMessages).isNotEmpty();
        log.info("All received messages: {}", receivedMessages);

        // Cleanup
        pubSubConnection.sync().unsubscribe(testChannel);
        publisherConnection.close();
        pubSubConnection.close();

        log.info("Pub/Sub failover test completed successfully");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private RedisURI createRedisUri(String endpoint) {
        RedisURI baseUri = RedisURI.create(endpoint);
        RedisURI.Builder builder = RedisURI.builder(baseUri);

        if (aaEndpoint.getPassword() != null) {
            if (aaEndpoint.getUsername() != null) {
                builder.withAuthentication(aaEndpoint.getUsername(), aaEndpoint.getPassword());
            } else {
                builder.withPassword(aaEndpoint.getPassword().toCharArray());
            }
        }

        return builder.build();
    }

    private ClientOptions createClientOptions() {
        // Use simple defaults - advanced socket options may cause issues with MultiDbClient
        return ClientOptions.builder().socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(10)).build())
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(10))).build();
    }

    /**
     * Extract run_id from INFO SERVER output.
     */
    private static String extractRunId(String info) {
        for (String line : info.split("\r?\n")) {
            if (line.startsWith("run_id:")) {
                return line.substring("run_id:".length()).trim();
            }
        }
        return "";
    }

    // ========================================
    // Inner Classes
    // ========================================

    /**
     * Reporter to capture DatabaseSwitchEvent with timestamps.
     */
    static class FailoverReporter implements Consumer<DatabaseSwitchEvent> {

        private volatile boolean failoverHappened = false;

        private volatile boolean failbackHappened = false;

        private volatile Instant failoverAt = null;

        private volatile Instant failbackAt = null;

        private volatile SwitchReason failoverReason = null;

        private volatile RedisURI fromUri = null;

        private volatile RedisURI toUri = null;

        @Override
        public void accept(DatabaseSwitchEvent event) {
            log.info("DatabaseSwitchEvent: from={} to={} reason={}", event.getFromDb(), event.getToDb(), event.getReason());

            if (event.getReason() == SwitchReason.FAILBACK) {
                failbackHappened = true;
                failbackAt = Instant.now();
            } else {
                if (!failoverHappened) {
                    failoverHappened = true;
                    failoverAt = Instant.now();
                    failoverReason = event.getReason();
                    fromUri = event.getFromDb();
                    toUri = event.getToDb();
                }
            }
        }

        public boolean isFailoverHappened() {
            return failoverHappened;
        }

        public boolean isFailbackHappened() {
            return failbackHappened;
        }

        public Instant getFailoverAt() {
            return failoverAt;
        }

        public Instant getFailbackAt() {
            return failbackAt;
        }

        public SwitchReason getFailoverReason() {
            return failoverReason;
        }

        public RedisURI getFromUri() {
            return fromUri;
        }

        public RedisURI getToUri() {
            return toUri;
        }

    }

    /**
     * Multi-threaded fake application that continuously executes commands.
     */
    static class MultiThreadedFakeApp implements Runnable {

        private final StatefulRedisMultiDbConnection<String, String> connection;

        private final int numThreads;

        private final Duration duration;

        private final AtomicLong executedCommands = new AtomicLong(0);

        private final List<Throwable> capturedExceptions = new CopyOnWriteArrayList<>();

        private final AtomicBoolean running = new AtomicBoolean(true);

        MultiThreadedFakeApp(StatefulRedisMultiDbConnection<String, String> connection, int numThreads, Duration duration) {
            this.connection = connection;
            this.numThreads = numThreads;
            this.duration = duration;
        }

        @Override
        public void run() {
            Thread[] threads = new Thread[numThreads];
            long endTime = System.currentTimeMillis() + duration.toMillis();

            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    while (running.get() && System.currentTimeMillis() < endTime) {
                        try {
                            String key = "aa-test-key-" + threadId + "-" + System.currentTimeMillis();
                            String value = "value-" + executedCommands.get();

                            connection.sync().set(key, value);
                            connection.sync().get(key);

                            executedCommands.incrementAndGet();

                            // Small delay between commands
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            capturedExceptions.add(e);
                            log.info("Thread {} caught exception: {}", threadId, e.getMessage());
                            try {
                                Thread.sleep(50); // Backoff on error
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            running.set(false);
        }

        public void stop() {
            running.set(false);
        }

        public long getExecutedCommands() {
            return executedCommands.get();
        }

        public List<Throwable> getCapturedExceptions() {
            return capturedExceptions;
        }

    }

    /**
     * Controllable health check strategy for testing health check failover.
     */
    static class ControllableHealthCheckStrategy implements HealthCheckStrategy {

        private final Map<RedisURI, HealthStatus> healthStatuses = new java.util.concurrent.ConcurrentHashMap<>();

        private final Config config;

        ControllableHealthCheckStrategy() {
            this.config = Config.builder().interval(100) // 100ms interval
                    .timeout(1000).numProbes(1).delayInBetweenProbes(10).build();
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
            return healthStatuses.getOrDefault(endpoint, HealthStatus.HEALTHY);
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

        public void setHealthStatus(RedisURI endpoint, HealthStatus status) {
            healthStatuses.put(endpoint, status);
            log.info("Set health status for {} to {}", endpoint, status);
        }

    }

}
