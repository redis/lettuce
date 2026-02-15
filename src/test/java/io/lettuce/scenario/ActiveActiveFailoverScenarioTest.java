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
import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.failover.api.CircuitBreakerConfig;
import io.lettuce.core.failover.api.DatabaseConfig;
import io.lettuce.core.failover.api.MultiDbOptions;
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

@Tag(SCENARIO_TEST)
@DisplayName("Active-Active Failover Scenario Tests")
public class ActiveActiveFailoverScenarioTest {

    private static final Logger log = LoggerFactory.getLogger(ActiveActiveFailoverScenarioTest.class);

    private static final int NUM_THREADS = 4;

    private static final int PRIMARY_KEYS_COUNT = 100;

    private static final int SECONDARY_KEYS_COUNT = 50;

    private static final int CB_TRIGGER_COMMANDS = 20;

    private static final int PUBSUB_EXPECTED_MESSAGES = 5;

    private static final float PRIMARY_WEIGHT = 1.0f;

    private static final float SECONDARY_WEIGHT = 0.5f;

    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration CB_COMMAND_TIMEOUT = Duration.ofMillis(500);

    private static final Duration CLUSTER_HEALTHY_TIMEOUT = Duration.ofSeconds(90);

    private static final Duration FAILOVER_AWAIT_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration FAILBACK_AWAIT_TIMEOUT = Duration.ofSeconds(15);

    private static final Duration HEALTH_DETECT_TIMEOUT = Duration.ofSeconds(5);

    private static final Duration REPLICATION_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration WORKLOAD_DURATION_SHORT = Duration.ofSeconds(15);

    private static final Duration WORKLOAD_DURATION_LONG = Duration.ofSeconds(20);

    private static final Duration THREAD_JOIN_TIMEOUT = Duration.ofSeconds(20);

    private static final Duration GRACE_PERIOD = Duration.ofSeconds(3);

    private static final Duration FAILBACK_CHECK_INTERVAL = Duration.ofSeconds(5);

    private static final int NETWORK_LATENCY_MS = 2000;

    private static final int NETWORK_LATENCY_DURATION_SEC = 60;

    private static final Duration FAULT_ACTION_TIMEOUT = Duration.ofSeconds(30);

    private static final int HEALTHY_PING_STREAK_REQUIRED = 3;

    private static final long HEALTHY_PING_THRESHOLD_MS = 1500;

    private static final Duration HEALTHY_POLL_INTERVAL = Duration.ofSeconds(2);

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
        waitForClusterHealthy(primaryUri, CLUSTER_HEALTHY_TIMEOUT);
    }

    @Test
    @DisplayName("Circuit breaker triggers failover on command timeouts")
    public void testCircuitBreakerFailover() throws Exception {
        log.info("Starting circuit breaker failover test");

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder().connectTimeout(CONNECTION_TIMEOUT).build())
                .timeoutOptions(TimeoutOptions.enabled(CB_COMMAND_TIMEOUT)).build();

        CircuitBreakerConfig cbConfig = createLowCBConfig();

        createMultiDbClient(clientOptions, cbConfig, HealthCheckStrategySupplier.NO_HEALTH_CHECK, true, Duration.ofSeconds(1),
                Duration.ofSeconds(2));

        assertThat(connection.sync().ping()).isEqualTo("PONG");
        connection.switchTo(primaryUri);
        assertThat(connection.getCurrentEndpoint()).isEqualTo(primaryUri);

        captureRunIds();
        FailoverReporter reporter = setupFailoverReporter();

        log.info("Circuit breaker initial state: {}", connection.getDatabase(primaryUri).getCircuitBreakerState());

        log.info("Triggering network_latency on primary ({}ms delay for {}s duration)", NETWORK_LATENCY_MS,
                NETWORK_LATENCY_DURATION_SEC);
        Map<String, Object> params = new HashMap<>();
        params.put("bdb_id", aaEndpoint.getBdbId());
        params.put("delay_ms", NETWORK_LATENCY_MS);
        params.put("duration", NETWORK_LATENCY_DURATION_SEC);
        faultClient.triggerActionAndWait("network_latency", params, Duration.ofSeconds(2), Duration.ofSeconds(1),
                FAULT_ACTION_TIMEOUT).block();
        log.info("Network latency applied, starting command execution");

        log.info("Firing async commands to trigger circuit breaker");
        fireCommandsUntilFailover(reporter, CB_TRIGGER_COMMANDS);

        await().atMost(FAILBACK_AWAIT_TIMEOUT).untilAsserted(() -> assertThat(reporter.isFailoverHappened()).isTrue());

        log.info("Failover happened at: {}, reason: {}", reporter.getFailoverAt(), reporter.getFailoverReason());
        assertThat(reporter.getFailoverReason()).isEqualTo(SwitchReason.CIRCUIT_BREAKER);

        await().atMost(FAILOVER_AWAIT_TIMEOUT).untilAsserted(() -> {
            String currentRunId = extractRunId(connection.sync().info("server"));
            assertThat(currentRunId).isEqualTo(secondaryRunId);
        });
        log.info("Verified failover to secondary endpoint");

        verifyCommandsWork();
        log.info("Circuit breaker failover test completed successfully");
    }

    @Test
    @DisplayName("Health check should trigger failover on unhealthy status")
    public void testHealthCheckFailover() throws Exception {
        log.info("Starting health check failover test");

        ControllableHealthCheckStrategy healthCheckStrategy = new ControllableHealthCheckStrategy();
        HealthCheckStrategySupplier controllableSupplier = (uri, factory) -> healthCheckStrategy;

        createMultiDbClient(createClientOptions(), createHighCBConfig(), controllableSupplier, false, null, Duration.ZERO);

        assertThat(connection.sync().ping()).isEqualTo("PONG");
        connection.switchTo(primaryUri);
        assertThat(connection.getCurrentEndpoint()).isEqualTo(primaryUri);

        captureRunIds();
        FailoverReporter reporter = setupFailoverReporter();

        MultiThreadedFakeApp fakeApp = startWorkload(WORKLOAD_DURATION_LONG);

        log.info("Marking primary endpoint as UNHEALTHY");
        healthCheckStrategy.setHealthStatus(primaryUri, HealthStatus.UNHEALTHY);

        awaitUnhealthy(primaryUri);
        awaitFailover(reporter, SwitchReason.HEALTH_CHECK);
        verifyOnSecondary();

        stopWorkload(fakeApp);
    }

    @Test
    @DisplayName("Manual switch should work gracefully with no stuck commands")
    public void testManualSwitch() throws Exception {
        log.info("Starting manual switch test");

        verifyDirectConnection();

        createMultiDbClient(createClientOptions(), createHighCBConfig(), HealthCheckStrategySupplier.NO_HEALTH_CHECK, false,
                null, Duration.ZERO);

        assertThat(connection.sync().ping()).isEqualTo("PONG");
        connection.switchTo(primaryUri);

        captureRunIds();
        FailoverReporter reporter = setupFailoverReporter();

        MultiThreadedFakeApp fakeApp = startWorkload(WORKLOAD_DURATION_SHORT);

        log.info("Performing manual switch to secondary");
        connection.switchTo(secondaryUri);

        assertThat(connection.getCurrentEndpoint()).isEqualTo(secondaryUri);
        awaitFailover(reporter, SwitchReason.FORCED);
        verifyOnSecondary();

        log.info("Switching back to primary");
        connection.switchTo(primaryUri);
        verifyOnPrimary();

        stopWorkload(fakeApp);
    }

    @Test
    @DisplayName("Health check should trigger failover and failback cycle")
    public void testHealthCheckFailoverWithFailback() throws Exception {
        log.info("Starting health check failover with failback test");

        ControllableHealthCheckStrategy healthCheckStrategy = new ControllableHealthCheckStrategy();
        HealthCheckStrategySupplier controllableSupplier = (uri, factory) -> healthCheckStrategy;

        createMultiDbClient(createClientOptions(), createHighCBConfig(), controllableSupplier, true, FAILBACK_CHECK_INTERVAL,
                GRACE_PERIOD);

        assertThat(connection.sync().ping()).isEqualTo("PONG");
        connection.switchTo(primaryUri);
        assertThat(connection.getCurrentEndpoint()).isEqualTo(primaryUri);

        captureRunIds();
        FailoverReporter reporter = setupFailoverReporter();
        verifyOnPrimary();

        log.info("PHASE 1: Marking primary endpoint as UNHEALTHY");
        healthCheckStrategy.setHealthStatus(primaryUri, HealthStatus.UNHEALTHY);

        awaitUnhealthy(primaryUri);
        awaitFailover(reporter, SwitchReason.HEALTH_CHECK);
        verifyOnSecondary();

        log.info("PHASE 2: Marking primary endpoint as HEALTHY again");
        healthCheckStrategy.setHealthStatus(primaryUri, HealthStatus.HEALTHY);

        awaitHealthy(primaryUri);
        awaitFailback(reporter);
        verifyOnPrimary();

        verifyCommandsWork();
        log.info("Health check failover with failback test completed successfully");
    }

    @Test
    @DisplayName("Data written before failover should be readable after failover (AA replication)")
    public void testDataIntegrityDuringFailover() throws Exception {
        log.info("Starting data integrity during failover test");

        ControllableHealthCheckStrategy healthCheckStrategy = new ControllableHealthCheckStrategy();
        HealthCheckStrategySupplier controllableSupplier = (uri, factory) -> healthCheckStrategy;

        createMultiDbClient(createClientOptions(), createHighCBConfig(), controllableSupplier, true, FAILBACK_CHECK_INTERVAL,
                GRACE_PERIOD);

        assertThat(connection.sync().ping()).isEqualTo("PONG");
        connection.switchTo(primaryUri);
        assertThat(connection.getCurrentEndpoint()).isEqualTo(primaryUri);

        captureRunIds();
        FailoverReporter reporter = setupFailoverReporter();

        log.info("PHASE 1: Writing {} keys on primary endpoint", PRIMARY_KEYS_COUNT);
        String testPrefix = "data-integrity-" + System.currentTimeMillis() + "-";
        Map<String, String> writtenOnPrimary = writeTestKeys(testPrefix + "primary-", PRIMARY_KEYS_COUNT);

        log.info("PHASE 2: Triggering failover to secondary");
        healthCheckStrategy.setHealthStatus(primaryUri, HealthStatus.UNHEALTHY);

        await().atMost(FAILOVER_AWAIT_TIMEOUT).untilAsserted(() -> assertThat(reporter.isFailoverHappened()).isTrue());
        verifyOnSecondary();

        log.info("PHASE 3: Verifying data on secondary (with AA replication lag tolerance)");
        verifyDataReplication(writtenOnPrimary);
        log.info("All {} keys verified on secondary", writtenOnPrimary.size());

        log.info("PHASE 4: Writing {} additional keys on secondary endpoint", SECONDARY_KEYS_COUNT);
        Map<String, String> writtenOnSecondary = writeTestKeys(testPrefix + "secondary-", SECONDARY_KEYS_COUNT);

        log.info("PHASE 5: Triggering failback to primary");
        healthCheckStrategy.setHealthStatus(primaryUri, HealthStatus.HEALTHY);

        awaitFailback(reporter);
        verifyOnPrimary();

        log.info("PHASE 6: Verifying secondary data on primary");
        verifyDataReplication(writtenOnSecondary);
        log.info("All {} secondary keys verified on primary", writtenOnSecondary.size());

        log.info("PHASE 7: Verifying original primary data is still accessible");
        verifyAllKeys(writtenOnPrimary);
        log.info("Verified {} original primary keys", writtenOnPrimary.size());

        log.info("Data integrity test completed. Total keys: {}", writtenOnPrimary.size() + writtenOnSecondary.size());
    }

    @Test
    @DisplayName("Pub/Sub subscriptions should survive failover")
    public void testPubSubFailover() throws Exception {
        log.info("Starting Pub/Sub failover test");

        createMultiDbClient(createClientOptions(), createLowCBConfig(), HealthCheckStrategySupplier.NO_HEALTH_CHECK, false,
                null, Duration.ZERO);

        StatefulRedisMultiDbPubSubConnection<String, String> pubSubConnection = multiDbClient.connectPubSub();

        List<String> receivedMessages = new CopyOnWriteArrayList<>();
        CountDownLatch messageLatch = new CountDownLatch(PUBSUB_EXPECTED_MESSAGES);

        pubSubConnection.addListener(new RedisPubSubAdapter<String, String>() {

            @Override
            public void message(String channel, String message) {
                log.info("Received message on channel {}: {}", channel, message);
                receivedMessages.add(message);
                messageLatch.countDown();
            }

        });

        pubSubConnection.switchTo(primaryUri);

        String testChannel = "aa-failover-test-channel";
        pubSubConnection.sync().subscribe(testChannel);
        log.info("Subscribed to channel: {}", testChannel);

        StatefulRedisMultiDbConnection<String, String> publisherConnection = multiDbClient.connect();

        log.info("Publishing messages before failover");
        publishMessages(publisherConnection, testChannel, "message-before-", 2);

        log.info("Switching pub/sub connection to secondary");
        pubSubConnection.switchTo(secondaryUri);
        publisherConnection.switchTo(secondaryUri);

        await().atMost(HEALTH_DETECT_TIMEOUT).until(() -> true);

        log.info("Publishing messages after failover");
        publishMessages(publisherConnection, testChannel, "message-after-", 3);

        boolean received = messageLatch.await(FAILOVER_AWAIT_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        log.info("Messages received: {}, count: {}", received, receivedMessages.size());

        assertThat(receivedMessages).isNotEmpty();
        log.info("All received messages: {}", receivedMessages);

        pubSubConnection.sync().unsubscribe(testChannel);
        publisherConnection.close();
        pubSubConnection.close();

        log.info("Pub/Sub failover test completed successfully");
    }

    private void createMultiDbClient(ClientOptions clientOptions, CircuitBreakerConfig cbConfig,
            HealthCheckStrategySupplier healthCheckSupplier, boolean failbackSupported, Duration failbackCheckInterval,
            Duration gracePeriod) {

        DatabaseConfig primaryConfig = DatabaseConfig.builder(primaryUri).weight(PRIMARY_WEIGHT).clientOptions(clientOptions)
                .circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(healthCheckSupplier).build();

        DatabaseConfig secondaryConfig = DatabaseConfig.builder(secondaryUri).weight(SECONDARY_WEIGHT)
                .clientOptions(clientOptions).circuitBreakerConfig(cbConfig).healthCheckStrategySupplier(healthCheckSupplier)
                .build();

        MultiDbOptions.Builder optionsBuilder = MultiDbOptions.builder().failbackSupported(failbackSupported)
                .gracePeriod(gracePeriod);
        if (failbackCheckInterval != null) {
            optionsBuilder.failbackCheckInterval(failbackCheckInterval);
        }

        multiDbClient = MultiDbClient.create(Arrays.asList(primaryConfig, secondaryConfig), optionsBuilder.build());
        connection = multiDbClient.connect();
    }

    private void captureRunIds() {
        primaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(secondaryUri);
        secondaryRunId = extractRunId(connection.sync().info("server"));
        connection.switchTo(primaryUri);
        log.info("Primary run_id: {}, Secondary run_id: {}", primaryRunId, secondaryRunId);
        assertThat(primaryRunId).isNotEqualTo(secondaryRunId);
    }

    private FailoverReporter setupFailoverReporter() {
        FailoverReporter reporter = new FailoverReporter();
        eventSubscription = multiDbClient.getResources().eventBus().get().subscribe(event -> {
            if (event instanceof DatabaseSwitchEvent) {
                reporter.accept((DatabaseSwitchEvent) event);
            }
        });
        return reporter;
    }

    private void verifyDirectConnection() {
        log.info("Testing direct connection to primary: {}", primaryUri);
        io.lettuce.core.RedisClient directClient = io.lettuce.core.RedisClient.create(primaryUri);
        try (io.lettuce.core.api.StatefulRedisConnection<String, String> directConn = directClient.connect()) {
            assertThat(directConn.sync().ping()).isEqualTo("PONG");
        }
        directClient.shutdown();
        log.info("Direct connection test passed");
    }

    private void verifyCommandsWork() {
        log.info("Verifying commands work");
        for (int i = 0; i < 5; i++) {
            assertThat(connection.sync().set("verify-key-" + i, "value-" + i)).isEqualTo("OK");
        }
        log.info("Successfully executed commands");
    }

    private void verifyOnPrimary() {
        String currentRunId = extractRunId(connection.sync().info("server"));
        assertThat(currentRunId).isEqualTo(primaryRunId);
        log.info("Verified on primary endpoint");
    }

    private void verifyOnSecondary() {
        String currentRunId = extractRunId(connection.sync().info("server"));
        assertThat(currentRunId).isEqualTo(secondaryRunId);
        log.info("Verified on secondary endpoint");
    }

    private void awaitUnhealthy(RedisURI uri) {
        await().atMost(HEALTH_DETECT_TIMEOUT).untilAsserted(() -> assertThat(connection.isHealthy(uri)).isFalse());
        log.info("Endpoint detected as unhealthy: {}", uri);
    }

    private void awaitHealthy(RedisURI uri) {
        await().atMost(HEALTH_DETECT_TIMEOUT).untilAsserted(() -> assertThat(connection.isHealthy(uri)).isTrue());
        log.info("Endpoint detected as healthy: {}", uri);
    }

    private void awaitFailover(FailoverReporter reporter, SwitchReason expectedReason) {
        await().atMost(FAILOVER_AWAIT_TIMEOUT).untilAsserted(() -> {
            assertThat(reporter.isFailoverHappened()).isTrue();
            assertThat(reporter.getFailoverReason()).isEqualTo(expectedReason);
        });
        log.info("Failover happened at: {}, reason: {}", reporter.getFailoverAt(), reporter.getFailoverReason());
    }

    private void awaitFailback(FailoverReporter reporter) {
        log.info("Waiting for grace period and failback check");
        await().atMost(FAILBACK_AWAIT_TIMEOUT).untilAsserted(() -> assertThat(reporter.isFailbackHappened()).isTrue());
        log.info("Failback happened at: {}", reporter.getFailbackAt());
    }

    private MultiThreadedFakeApp startWorkload(Duration duration) {
        MultiThreadedFakeApp fakeApp = new MultiThreadedFakeApp(connection, NUM_THREADS, duration);
        Thread workloadThread = new Thread(fakeApp);
        workloadThread.start();
        fakeApp.setThread(workloadThread);
        await().atMost(HEALTH_DETECT_TIMEOUT).until(() -> fakeApp.getExecutedCommands() > 0);
        log.info("Workload started, commands: {}", fakeApp.getExecutedCommands());
        return fakeApp;
    }

    private void stopWorkload(MultiThreadedFakeApp fakeApp) throws InterruptedException {
        fakeApp.stop();
        fakeApp.getThread().join(THREAD_JOIN_TIMEOUT.toMillis());
        log.info("Total commands: {}, exceptions: {}", fakeApp.getExecutedCommands(), fakeApp.getCapturedExceptions().size());
    }

    private void fireCommandsUntilFailover(FailoverReporter reporter, int maxCommands) {
        for (int i = 0; i < maxCommands; i++) {
            try {
                connection.async().get("cb-test-key-" + i);
            } catch (Exception e) {
            }

            await().pollDelay(Duration.ofMillis(100)).atMost(Duration.ofMillis(150)).until(() -> true);

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
    }

    private Map<String, String> writeTestKeys(String prefix, int count) {
        Map<String, String> written = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String key = prefix + i;
            String value = "value-" + i + "-" + System.currentTimeMillis();
            assertThat(connection.sync().set(key, value)).isEqualTo("OK");
            written.put(key, value);
        }
        log.info("Wrote {} keys with prefix {}", count, prefix);
        return written;
    }

    private void verifyAllKeys(Map<String, String> expectedData) {
        for (Map.Entry<String, String> entry : expectedData.entrySet()) {
            assertThat(connection.sync().get(entry.getKey())).isEqualTo(entry.getValue());
        }
    }

    private void verifyDataReplication(Map<String, String> expectedData) {
        await().atMost(REPLICATION_TIMEOUT).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            int matchCount = 0;
            for (Map.Entry<String, String> entry : expectedData.entrySet()) {
                String actual = connection.sync().get(entry.getKey());
                if (entry.getValue().equals(actual)) {
                    matchCount++;
                }
            }
            log.info("Data verification progress: {}/{} keys match", matchCount, expectedData.size());
            assertThat(matchCount).isEqualTo(expectedData.size());
        });
    }

    private void publishMessages(StatefulRedisMultiDbConnection<String, String> publisher, String channel, String prefix,
            int count) {
        for (int i = 0; i < count; i++) {
            publisher.sync().publish(channel, prefix + i);
        }
    }

    private void waitForClusterHealthy(RedisURI uri, Duration timeout) {
        log.info("Waiting for cluster to become healthy (timeout={}s)", timeout.getSeconds());

        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout.toMillis();
        int healthyStreak = 0;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            io.lettuce.core.RedisClient probeClient = null;
            try {
                probeClient = io.lettuce.core.RedisClient.create(uri);
                probeClient.setOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder().connectTimeout(HEALTH_DETECT_TIMEOUT).build())
                        .timeoutOptions(TimeoutOptions.enabled(HEALTH_DETECT_TIMEOUT)).build());

                long pingStart = System.currentTimeMillis();
                try (io.lettuce.core.api.StatefulRedisConnection<String, String> probeConn = probeClient.connect()) {
                    probeConn.sync().ping();
                }
                long pingMs = System.currentTimeMillis() - pingStart;

                if (pingMs < HEALTHY_PING_THRESHOLD_MS) {
                    healthyStreak++;
                    log.info("Cluster PING: {}ms (streak {}/{})", pingMs, healthyStreak, HEALTHY_PING_STREAK_REQUIRED);
                    if (healthyStreak >= HEALTHY_PING_STREAK_REQUIRED) {
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

            await().pollDelay(HEALTHY_POLL_INTERVAL).atMost(HEALTHY_POLL_INTERVAL.plusSeconds(1)).until(() -> true);
        }

        log.warn("Cluster did not become healthy within {}s, proceeding anyway", timeout.getSeconds());
    }

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
        return ClientOptions.builder().socketOptions(SocketOptions.builder().connectTimeout(CONNECTION_TIMEOUT).build())
                .timeoutOptions(TimeoutOptions.enabled(COMMAND_TIMEOUT)).build();
    }

    private CircuitBreakerConfig createHighCBConfig() {
        return CircuitBreakerConfig.builder().failureRateThreshold(90.0f).minimumNumberOfFailures(1000).metricsWindowSize(100)
                .build();
    }

    private CircuitBreakerConfig createLowCBConfig() {
        return CircuitBreakerConfig.builder().failureRateThreshold(10.0f).minimumNumberOfFailures(5).metricsWindowSize(10)
                .build();
    }

    private static String extractRunId(String info) {
        for (String line : info.split("\r?\n")) {
            if (line.startsWith("run_id:")) {
                return line.substring("run_id:".length()).trim();
            }
        }
        return "";
    }

    static class FailoverReporter implements Consumer<DatabaseSwitchEvent> {

        private volatile boolean failoverHappened = false;

        private volatile boolean failbackHappened = false;

        private volatile Instant failoverAt = null;

        private volatile Instant failbackAt = null;

        private volatile SwitchReason failoverReason = null;

        @Override
        public void accept(DatabaseSwitchEvent event) {
            log.info("DatabaseSwitchEvent: from={} to={} reason={}", event.getFromDb(), event.getToDb(), event.getReason());

            if (event.getReason() == SwitchReason.FAILBACK) {
                failbackHappened = true;
                failbackAt = Instant.now();
            } else if (!failoverHappened) {
                failoverHappened = true;
                failoverAt = Instant.now();
                failoverReason = event.getReason();
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

    }

    static class MultiThreadedFakeApp implements Runnable {

        private static final long COMMAND_INTERVAL_MS = 10;

        private static final long ERROR_BACKOFF_MS = 50;

        private final StatefulRedisMultiDbConnection<String, String> connection;

        private final int numThreads;

        private final Duration duration;

        private final AtomicLong executedCommands = new AtomicLong(0);

        private final List<Throwable> capturedExceptions = new CopyOnWriteArrayList<>();

        private final AtomicBoolean running = new AtomicBoolean(true);

        private Thread parentThread;

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
                            connection.sync().set(key, "value-" + executedCommands.get());
                            connection.sync().get(key);
                            executedCommands.incrementAndGet();
                            Thread.sleep(COMMAND_INTERVAL_MS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            capturedExceptions.add(e);
                            log.info("Thread {} caught exception: {}", threadId, e.getMessage());
                            try {
                                Thread.sleep(ERROR_BACKOFF_MS);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            running.set(false);
        }

        public void setThread(Thread thread) {
            this.parentThread = thread;
        }

        public Thread getThread() {
            return parentThread;
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

    static class ControllableHealthCheckStrategy implements HealthCheckStrategy {

        private static final int HEALTH_CHECK_INTERVAL_MS = 100;

        private static final int HEALTH_CHECK_TIMEOUT_MS = 1000;

        private static final int NUM_PROBES = 1;

        private static final int DELAY_BETWEEN_PROBES_MS = 10;

        private final Map<RedisURI, HealthStatus> healthStatuses = new java.util.concurrent.ConcurrentHashMap<>();

        private final Config config;

        ControllableHealthCheckStrategy() {
            this.config = Config.builder().interval(HEALTH_CHECK_INTERVAL_MS).timeout(HEALTH_CHECK_TIMEOUT_MS)
                    .numProbes(NUM_PROBES).delayInBetweenProbes(DELAY_BETWEEN_PROBES_MS).build();
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

        @Override
        public HealthStatus doHealthCheck(RedisURI endpoint) {
            return healthStatuses.getOrDefault(endpoint, HealthStatus.HEALTHY);
        }

        public void setHealthStatus(RedisURI endpoint, HealthStatus status) {
            healthStatuses.put(endpoint, status);
            log.info("Set health status for {} to {}", endpoint, status);
        }

    }

}
