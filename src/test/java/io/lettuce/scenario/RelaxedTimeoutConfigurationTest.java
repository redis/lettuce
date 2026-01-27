package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.lettuce.core.MaintNotificationsConfig;
import io.lettuce.core.MaintNotificationsConfig.EndpointType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;

import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.KeyValue;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.RedisFuture;
import io.lettuce.test.ConnectionTestUtil;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;

import reactor.test.StepVerifier;

import static io.lettuce.TestTags.SCENARIO_TEST;

@Tag(SCENARIO_TEST)
public class RelaxedTimeoutConfigurationTest {

    private static final Logger log = LoggerFactory.getLogger(RelaxedTimeoutConfigurationTest.class);

    // Timeout constants for testing
    // Small timeout to simulate latency vs timeout issues
    private static final Duration NORMAL_COMMAND_TIMEOUT = Duration.ofMillis(30);

    // Additional timeout during maintenance
    private static final Duration RELAXED_TIMEOUT_ADDITION = Duration.ofMillis(100);

    // Total timeout during maintenance
    private static final Duration EFFECTIVE_TIMEOUT_DURING_MAINTENANCE = NORMAL_COMMAND_TIMEOUT.plus(RELAXED_TIMEOUT_ADDITION);

    // For migrations/failovers
    private static final Duration LONG_OPERATION_TIMEOUT = Duration.ofMinutes(5);

    // For blpop operations
    private static final Duration BLPOP_TIMEOUT = Duration.ofSeconds(10);

    // Number of commands to send in timeout verification tests
    private static final int TEST_COMMAND_COUNT = 1;

    private static Endpoint mStandard;

    private RedisEnterpriseConfig clusterConfig;

    private final FaultInjectionClient faultClient = new FaultInjectionClient();

    @BeforeAll
    public static void setup() {
        mStandard = Endpoints.DEFAULT.getEndpoint("m-standard");
        assumeTrue(mStandard != null, "Skipping test because no M-Standard Redis endpoint is configured!");
    }

    @BeforeEach
    public void refreshClusterConfig() {
        clusterConfig = RedisEnterpriseConfig.refreshClusterConfig(faultClient, String.valueOf(mStandard.getBdbId()));
    }

    private static class TimeoutTestContext {

        final RedisClient client;

        final StatefulRedisConnection<String, String> connection;

        final TimeoutCapture capture;

        final String bdbId;

        TimeoutTestContext(RedisClient client, StatefulRedisConnection<String, String> connection, TimeoutCapture capture,
                String bdbId) {
            this.client = client;
            this.connection = connection;
            this.capture = capture;
            this.bdbId = bdbId;
        }

    }

    public static class TimeoutCapture implements MaintenanceNotificationCapture {

        private final List<String> receivedNotifications = new CopyOnWriteArrayList<>();

        // Separate latches for different operation types
        private final CountDownLatch migrationLatch = new CountDownLatch(3); // MIGRATING + MIGRATED + MOVING

        private final CountDownLatch failoverLatch = new CountDownLatch(2); // FAILING_OVER + FAILED_OVER

        // Track which notification types we've already seen (to avoid duplicate countdown)
        private final AtomicBoolean migratingReceived = new AtomicBoolean(false);

        private final AtomicBoolean migratedReceived = new AtomicBoolean(false);

        private final AtomicBoolean movingReceived = new AtomicBoolean(false);

        private final AtomicBoolean failingOverReceived = new AtomicBoolean(false);

        private final AtomicBoolean failedOverReceived = new AtomicBoolean(false);

        private final AtomicReference<String> lastNotification = new AtomicReference<>();

        private final AtomicInteger timeoutCount = new AtomicInteger(0);

        private final AtomicInteger successCount = new AtomicInteger(0);

        // Track normal timeouts during maintenance (should be 0)
        private final AtomicInteger normalTimeoutsDuringMaintenance = new AtomicInteger(0);

        private final AtomicBoolean maintenanceActive = new AtomicBoolean(false);

        private final AtomicBoolean testPhaseActive = new AtomicBoolean(true);

        private final boolean isMovingTest;

        private final boolean isUnrelaxedTest;

        // Removed unused mainSyncCommands field

        // Reference to main connection for reflection
        private StatefulRedisConnection<String, String> mainConnection;

        // Reference to test context for consolidated traffic functions
        private TimeoutTestContext testContext;

        // Timing for MOVING operation
        private final AtomicLong movingStartTime = new AtomicLong(0);

        private final AtomicLong movingEndTime = new AtomicLong(0);

        // Capture assertion failures from background threads
        private final AtomicReference<AssertionError> assertionFailure = new AtomicReference<>();

        public TimeoutCapture(boolean isMovingTest, boolean isUnrelaxedTest) {
            this.isMovingTest = isMovingTest;
            this.isUnrelaxedTest = isUnrelaxedTest;
        }

        public void setMainConnection(StatefulRedisConnection<String, String> mainConnection) {
            this.mainConnection = mainConnection;
        }

        public StatefulRedisConnection<String, String> getMainConnection() {
            return mainConnection;
        }

        public void setTestContext(TimeoutTestContext testContext) {
            this.testContext = testContext;
        }

        public TimeoutTestContext getTestContext() {
            return testContext;
        }

        public void captureNotification(String notification) {
            log.info("=== NOTIFICATION CAPTURE START ===");
            log.info("Raw notification received: {}", notification);

            // Only capture notifications during the test phase, not during cleanup
            if (testPhaseActive.get()) {
                log.info("DECISION: testPhaseActive=true -> Processing notification");
                receivedNotifications.add(notification);
                lastNotification.set(notification);
                log.info("Captured push notification: {}", notification);

                String testType = isMovingTest ? "MOVING UN-RELAXED test" : "FAILOVER UN-RELAXED test";
                log.info("Test type: {} - Processing notification: {}", testType, notification);
                log.info("Test flags: isMovingTest={}, isUnrelaxedTest={}", isMovingTest, isUnrelaxedTest);
            } else {
                log.info("DECISION: testPhaseActive=false -> Ignoring notification during cleanup phase");
                log.debug("Ignoring notification during cleanup phase: {}", notification);
                return;
            }

            // Count down appropriate latches based on notification type (only once per type)
            if (notification.contains("MIGRATING")) {
                if (migratingReceived.compareAndSet(false, true)) {
                    migrationLatch.countDown();
                    log.info("First MIGRATING notification - migration latch count down");
                }
            } else if (notification.contains("MIGRATED")) {
                if (migratedReceived.compareAndSet(false, true)) {
                    migrationLatch.countDown();
                    log.info("First MIGRATED notification - migration latch count down");
                }
            } else if (notification.contains("MOVING")) {
                if (movingReceived.compareAndSet(false, true)) {
                    migrationLatch.countDown();
                    log.info("First MOVING notification - migration latch count down");
                }
            } else if (notification.contains("FAILING_OVER")) {
                if (failingOverReceived.compareAndSet(false, true)) {
                    failoverLatch.countDown();
                    log.info("First FAILING_OVER notification - failover latch count down");
                }
            } else if (notification.contains("FAILED_OVER")) {
                if (failedOverReceived.compareAndSet(false, true)) {
                    failoverLatch.countDown();
                    log.info("First FAILED_OVER notification - failover latch count down");
                }
            }

            // For MOVING tests: Test all 4 phases
            if (notification.contains("MIGRATED") && isMovingTest) {
                maintenanceActive.set(false);
                log.info("MIGRATED received - Testing unrelaxed timeouts after MIGRATED");

                try {
                    testUnrelaxedTimeoutsAfterMigrated(testContext);
                } catch (AssertionError e) {
                    log.error("Assertion failed in MIGRATED timeout test: {}", e.getMessage(), e);
                    assertionFailure.compareAndSet(null, e);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while testing unrelaxed timeouts after MIGRATED", e);
                    Thread.currentThread().interrupt();
                }

                log.info("MIGRATED unrelaxed testing completed - Waiting for MOVING notification");
            } else if (notification.contains("MOVING")) {
                log.info("=== MOVING DECISION TREE START ===");
                log.info("DECISION: MOVING notification received");
                log.info("ACTION: Setting maintenanceActive=true, recording MOVING start");
                maintenanceActive.set(true);
                recordMovingStart();

                log.info("DECISION: MOVING unrelaxed test");
                log.info("ACTION: Sending BLPOP traffic to test relaxed timeouts during MOVING");
                BlpopTrafficResult movingResult = sendBlpopTraffic(testContext, "moving-traffic-key-", TEST_COMMAND_COUNT);
                try {
                    checkTimeouts(movingResult, TimeoutExpectation.RELAXED, "MOVING");
                } catch (AssertionError e) {
                    log.error("Assertion failed in MOVING timeout test: {}", e.getMessage(), e);
                    assertionFailure.compareAndSet(null, e);
                }

                verifyConnectionAndStack("after MOVING traffic");

                maintenanceActive.set(false);
                log.info("ACTION: Setting maintenanceActive=false after MOVING testing");

                log.info("=== MOVING DECISION TREE END ===");

            } else if (notification.contains("MIGRATING")) {
                maintenanceActive.set(true);
                log.info(
                        "MOVING test received MIGRATING notification - Sending traffic to test relaxed timeouts during MIGRATING");
                BlpopTrafficResult migratingResult = sendBlpopTraffic(testContext, "migrating-traffic-key-",
                        TEST_COMMAND_COUNT);
                try {
                    checkTimeouts(migratingResult, TimeoutExpectation.RELAXED, "MIGRATING");
                } catch (AssertionError e) {
                    log.error("Assertion failed in MIGRATING timeout test: {}", e.getMessage(), e);
                    assertionFailure.compareAndSet(null, e);
                }

            } else if (notification.contains("FAILING_OVER") && !isMovingTest) {
                maintenanceActive.set(true);
                log.info("FAILING_OVER maintenance started - Sending traffic for testing");

                BlpopTrafficResult failingOverResult = sendBlpopTraffic(testContext, "failingover-traffic-key-",
                        TEST_COMMAND_COUNT);
                try {
                    checkTimeouts(failingOverResult, TimeoutExpectation.RELAXED, "FAILING_OVER");
                } catch (AssertionError e) {
                    log.error("Assertion failed in FAILING_OVER timeout test: {}", e.getMessage(), e);
                    assertionFailure.compareAndSet(null, e);
                }

            } else if (notification.contains("FAILED_OVER")) {
                maintenanceActive.set(false);
                log.info("Maintenance completed - timeouts should return to normal");

            } else {
                log.info("Ignoring notification: {} (not relevant for current test)", notification);
            }
        }

        public String extractTimeoutDuration(Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Command timed out after")) {
                String[] parts = e.getMessage().split("Command timed out after ");
                if (parts.length > 1) {
                    return parts[1].split(" ")[0];
                }
            }
            return "unknown";
        }

        public enum TimeoutExpectation {
            RELAXED, UNRELAXED
        }

        public static class BlpopTrafficResult {

            private final int normalTimeoutCount;

            private final int relaxedTimeoutCount;

            private final int successCount;

            private final int totalCommands;

            public BlpopTrafficResult(int normalTimeoutCount, int relaxedTimeoutCount, int successCount, int totalCommands) {
                this.normalTimeoutCount = normalTimeoutCount;
                this.relaxedTimeoutCount = relaxedTimeoutCount;
                this.successCount = successCount;
                this.totalCommands = totalCommands;
            }

            public int getNormalTimeoutCount() {
                return normalTimeoutCount;
            }

            public int getRelaxedTimeoutCount() {
                return relaxedTimeoutCount;
            }

            public int getSuccessCount() {
                return successCount;
            }

            public int getTotalCommands() {
                return totalCommands;
            }

        }

        private BlpopTrafficResult sendBlpopTraffic(TimeoutTestContext context, String keyPrefix, int commandCount) {
            log.info("Sending {} BLPOP commands with key prefix: {}", commandCount, keyPrefix);

            int normalTimeoutCount = 0;
            int relaxedTimeoutCount = 0;
            int successCount = 0;

            for (int i = 0; i < commandCount; i++) {
                StatefulRedisConnection<String, String> connection = context.capture.getMainConnection();

                if (connection == null || !connection.isOpen()) {
                    log.warn("Connection closed during BLPOP traffic, stopping at command #{}", i);
                    break;
                }

                long startTime = System.currentTimeMillis();
                try {
                    RedisFuture<KeyValue<String, String>> future = connection.async().blpop(BLPOP_TIMEOUT.getSeconds(),
                            keyPrefix + i);
                    future.get();

                    long duration = System.currentTimeMillis() - startTime;
                    log.info("BLPOP command #{} completed successfully in {}ms", i, duration);
                    successCount++;

                } catch (Exception e) {
                    long wallClockDuration = System.currentTimeMillis() - startTime;
                    String timeoutDurationStr = extractTimeoutDuration(e);
                    log.info("BLPOP command #{} timed out - Wall clock: {}ms, Actual timeout: {}ms", i, wallClockDuration,
                            timeoutDurationStr);

                    if (!"unknown".equals(timeoutDurationStr)) {
                        int timeoutDuration = Integer.parseInt(timeoutDurationStr);
                        if (timeoutDuration <= NORMAL_COMMAND_TIMEOUT.toMillis()) {
                            log.info("Normal timeout detected: {}ms", timeoutDuration);
                            normalTimeoutCount++;
                        } else if (timeoutDuration > NORMAL_COMMAND_TIMEOUT.toMillis()
                                && timeoutDuration <= EFFECTIVE_TIMEOUT_DURING_MAINTENANCE.toMillis()) {
                            log.info("Relaxed timeout detected: {}ms", timeoutDuration);
                            relaxedTimeoutCount++;
                        }
                    }
                }
            }

            return new BlpopTrafficResult(normalTimeoutCount, relaxedTimeoutCount, successCount, commandCount);
        }

        private void checkTimeouts(BlpopTrafficResult result, TimeoutExpectation expectation, String phase) {
            log.info("=== {} Timeout Check Results ===", phase);
            log.info("Total commands sent: {}", result.getTotalCommands());
            log.info("Successful operations: {}", result.getSuccessCount());
            log.info("Normal timeouts detected: {}", result.getNormalTimeoutCount());
            log.info("Relaxed timeouts detected: {}", result.getRelaxedTimeoutCount());

            if (expectation == TimeoutExpectation.RELAXED) {
                assertThat(result.getRelaxedTimeoutCount()).as(
                        "During {} phase, should have detected at least one relaxed timeout. "
                                + "No relaxed timeouts detected indicates the timeout relaxation mechanism is not working properly.",
                        phase).isEqualTo(result.getTotalCommands());

                assertThat(result.getNormalTimeoutCount()).as(
                        "During {} phase, NO commands should fail with normal timeout. " + "Found {} normal timeouts. "
                                + "Any normal timeouts indicate the timeout relaxation mechanism is not working properly.",
                        phase, result.getNormalTimeoutCount()).isEqualTo(0);
            } else {
                assertThat(result.getNormalTimeoutCount()).as("After {} phase, ALL commands should fail with normal timeout. "
                        + "Found {} normal timeouts out of {} total commands. "
                        + "Mixed timeout behavior indicates the timeout un-relaxation mechanism is not working properly.",
                        phase, result.getNormalTimeoutCount(), result.getTotalCommands()).isEqualTo(result.getTotalCommands());

                assertThat(result.getRelaxedTimeoutCount()).as(
                        "After {} phase, NO commands should fail with relaxed timeout. "
                                + "Found {} relaxed timeouts out of {} total commands. "
                                + "Any relaxed timeouts indicate the timeout un-relaxation mechanism is not working properly.",
                        phase, result.getRelaxedTimeoutCount(), result.getTotalCommands()).isEqualTo(0);
            }
        }

        private void verifyConnectionAndStack(String context) {
            log.info("Verifying connection and stack state: {}...", context);
            try {
                if (mainConnection != null && mainConnection.isOpen()) {
                    ConnectionTestUtil.verifyConnectionAndStackState(mainConnection, context);
                } else {
                    log.warn("mainConnection is null or closed - cannot verify stack");
                }
            } catch (Exception e) {
                log.warn("Failed to verify connection and stack {}: {} - {}", context, e.getClass().getSimpleName(),
                        e.getMessage());
            }
        }

        public void recordRelaxedTimeout() {
            timeoutCount.incrementAndGet();
        }

        public void recordNormalTimeoutDuringMaintenance() {
            normalTimeoutsDuringMaintenance.incrementAndGet();
        }

        public void recordSuccess() {
            successCount.incrementAndGet();
        }

        public boolean isMaintenanceActive() {
            return maintenanceActive.get();
        }

        public int getTimeoutCount() {
            return timeoutCount.get();
        }

        public int getNormalTimeoutsDuringMaintenance() {
            return normalTimeoutsDuringMaintenance.get();
        }

        public int getSuccessCount() {
            return successCount.get();
        }

        public boolean waitForMigrationNotifications(Duration timeout) throws InterruptedException {
            return migrationLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public boolean waitForFailoverNotifications(Duration timeout) throws InterruptedException {
            return failoverLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public List<String> getReceivedNotifications() {
            return receivedNotifications;
        }

        public String getLastNotification() {
            return lastNotification.get();
        }

        public void recordMovingStart() {
            movingStartTime.set(System.currentTimeMillis());
            log.info("MOVING operation started at {}", movingStartTime.get());
        }

        public void recordMovingEnd() {
            movingEndTime.set(System.currentTimeMillis());
            long duration = movingEndTime.get() - movingStartTime.get();
            log.info("MOVING operation completed at {} - Total duration: {}ms", movingEndTime.get(), duration);
        }

        public long getMovingDuration() {
            if (movingStartTime.get() > 0 && movingEndTime.get() > 0) {
                return movingEndTime.get() - movingStartTime.get();
            }
            return -1;
        }

        public void endTestPhase() {
            testPhaseActive.set(false);
            log.info("Test phase ended - notifications will be ignored during cleanup");
        }

        private void testUnrelaxedTimeoutsAfterMigrated(TimeoutTestContext context) throws InterruptedException {
            log.info("Testing unrelaxed timeouts after MIGRATED notification...");
            log.info("Waiting for grace period ({}ms) to allow timeout un-relaxation to take effect...",
                    2 * RELAXED_TIMEOUT_ADDITION.toMillis());
            Thread.sleep(2 * RELAXED_TIMEOUT_ADDITION.toMillis()); // Wait for grace period + 100ms buffer
            log.info("Grace period expired, starting traffic to verify unrelaxed timeouts...");
            BlpopTrafficResult result = sendBlpopTraffic(context, "migrated-test-key-", TEST_COMMAND_COUNT);
            checkTimeouts(result, TimeoutExpectation.UNRELAXED, "MIGRATED");
        }

        public void throwIfAssertionFailed() {
            AssertionError error = assertionFailure.get();
            if (error != null) {
                throw error;
            }
        }

    }

    private TimeoutTestContext setupTimeoutTestForMovingUnrelaxed() {
        return setupTimeoutTestWithType(true, true);
    }

    private TimeoutTestContext setupTimeoutTestForUnrelaxed() {
        return setupTimeoutTestWithType(false, true);
    }

    private TimeoutTestContext setupTimeoutTestWithType(boolean isMovingTest, boolean isUnrelaxedTest) {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).withTimeout(Duration.ofSeconds(5))
                .build();

        RedisClient client = RedisClient.create(uri);

        TimeoutOptions timeoutOptions = TimeoutOptions.builder().timeoutCommands().fixedTimeout(NORMAL_COMMAND_TIMEOUT)
                .relaxedTimeoutsDuringMaintenance(RELAXED_TIMEOUT_ADDITION).build();

        ClientOptions options = ClientOptions.builder().autoReconnect(true).protocolVersion(ProtocolVersion.RESP3)
                .maintNotificationsConfig(MaintNotificationsConfig.enabled(EndpointType.EXTERNAL_IP))
                .timeoutOptions(timeoutOptions).build();

        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        TimeoutCapture capture = new TimeoutCapture(isMovingTest, isUnrelaxedTest);

        capture.setMainConnection(connection);

        log.info("*** TIMEOUT TEST SETUP: Test method detected as isMovingTest={} ***", isMovingTest);

        try {
            connection.sync().ping();
            log.info("Initial PING successful - connection established");
        } catch (Exception e) {
            log.warn("Initial PING failed: {}", e.getMessage());
        }

        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture);

        String bdbId = String.valueOf(mStandard.getBdbId());

        TimeoutTestContext context = new TimeoutTestContext(client, connection, capture, bdbId);
        capture.setTestContext(context);

        return context;
    }

    private void cleanupTimeoutTest(TimeoutTestContext context) {
        context.connection.close();
        context.client.shutdown();
    }

    @Test
    @DisplayName("Comprehensive MOVING timeout test - Tests complete 4-phase sequence: relaxed during MIGRATING, unrelaxed after MIGRATED, relaxed during MOVING, unrelaxed after MOVING")
    public void timeoutUnrelaxedOnMovingTest() throws InterruptedException {
        log.info("test timeoutUnrelaxedOnMovingTest started");
        TimeoutTestContext context = setupTimeoutTestForMovingUnrelaxed();

        try {
            log.info("=== MOVING Un-relaxed Timeout Test: Starting maintenance operation ===");

            String endpointId = clusterConfig.getFirstEndpointId();
            String policy = "single";
            log.info("Starting maintenance operation (migrate + rebind) with endpoint-aware node selection...");

            log.info("Starting MOVING operation with endpoint-aware node selection and waiting for it to complete...");
            Boolean operationResult = faultClient.triggerMovingNotification(context.bdbId, endpointId, policy, clusterConfig)
                    .block(Duration.ofMinutes(3));
            assertThat(operationResult).isTrue();
            log.info("MOVING operation fully completed: {}", operationResult);

            // Wait for migration notifications (3 expected: MIGRATING + MIGRATED + MOVING)
            boolean migrationReceived = context.capture.waitForMigrationNotifications(Duration.ofMinutes(1));
            assertThat(migrationReceived).as("Should receive migration notifications").isTrue();

            log.info("Verifying we received the expected notifications...");
            log.info("Received notifications: {}", context.capture.getReceivedNotifications());
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MIGRATED"))).isTrue();
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isTrue();

            context.capture.recordMovingEnd();

            log.info("=== MOVING Un-relaxed Test: Testing normal timeouts after MOVING ===");

            log.info("Testing normal timeouts after MOVING notification and reconnection...");
            log.info("Connection status before timeout tests: {}", context.connection.isOpen());
            TimeoutCapture.BlpopTrafficResult result = context.capture.sendBlpopTraffic(context, "moving-normal-test-key-",
                    TEST_COMMAND_COUNT);
            context.capture.checkTimeouts(result, TimeoutCapture.TimeoutExpectation.UNRELAXED, "MOVING");

            log.info("=== MOVING Un-relaxed Test Results ===");
            log.info("MOVING operation duration: {}ms", context.capture.getMovingDuration());
            log.info("Notifications received: {}", context.capture.getReceivedNotifications().size());

            // Check if any assertions failed in background threads
            context.capture.throwIfAssertionFailed();

        } finally {
            context.capture.endTestPhase();
            cleanupTimeoutTest(context);
        }
        log.info("test timeoutUnrelaxedOnMovingTest ended");
    }

    @Test
    @DisplayName("Comprehensive failover timeout test - Tests relaxed during FAILING_OVER and unrelaxed after FAILED_OVER")
    public void timeoutUnrelaxedOnFailedoverTest() throws InterruptedException {
        log.info("test timeoutUnrelaxedOnFailedoverTest started");
        TimeoutTestContext context = setupTimeoutTestForUnrelaxed();

        String nodeId = clusterConfig.getNodeWithMasterShards();
        try {
            log.info("=== FAILED_OVER Un-relaxed Timeout Test: Starting maintenance operation ===");

            log.info("Triggering shard failover for FAILED_OVER notification asynchronously...");

            Boolean failoverResult = faultClient.triggerShardFailover(context.bdbId, nodeId, clusterConfig)
                    .block(Duration.ofMinutes(3));
            assertThat(failoverResult).isTrue();
            log.info("FAILED_OVER operation completed: {}", failoverResult);

            // Wait for failover notifications (2 expected: FAILING_OVER + FAILED_OVER)
            boolean failoverReceived = context.capture.waitForFailoverNotifications(Duration.ofMinutes(1));
            assertThat(failoverReceived).as("Should receive failover notifications").isTrue();

            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("FAILED_OVER"))).isTrue();

            log.info("=== FAILED_OVER Un-relaxed Test: Testing normal timeouts after FAILED_OVER ===");

            log.info("Testing that timeouts are back to normal after FAILED_OVER notification...");
            log.info("Waiting for grace period ({}ms) to allow timeout un-relaxation to take effect...",
                    2 * RELAXED_TIMEOUT_ADDITION.toMillis());
            Thread.sleep(2 * RELAXED_TIMEOUT_ADDITION.toMillis()); // Wait for grace period + 100ms buffer
            log.info("Grace period expired, starting traffic to verify unrelaxed timeouts...");
            TimeoutCapture.BlpopTrafficResult result = context.capture.sendBlpopTraffic(context, "normal-test-key-",
                    TEST_COMMAND_COUNT);
            context.capture.checkTimeouts(result, TimeoutCapture.TimeoutExpectation.UNRELAXED, "failedover completion");

            log.info("=== FAILED_OVER Un-relaxed Test Results ===");
            log.info("Notifications received: {}", context.capture.getReceivedNotifications().size());

            // Check if any assertions failed in background threads
            context.capture.throwIfAssertionFailed();

        } finally {
            context.capture.endTestPhase();

            clusterConfig = RedisEnterpriseConfig.refreshClusterConfig(faultClient, String.valueOf(mStandard.getBdbId()));
            nodeId = clusterConfig.getNodeWithMasterShards();

            log.info("performing cluster cleanup operation for failover testing");
            StepVerifier.create(faultClient.triggerShardFailover(context.bdbId, nodeId, clusterConfig)).expectNext(true)
                    .expectComplete().verify(LONG_OPERATION_TIMEOUT);
            cleanupTimeoutTest(context);
        }
        log.info("test timeoutUnrelaxedOnFailedoverTest ended");
    }

}
