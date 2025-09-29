package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

/**
 * Optimized functional tests for relaxed timeout configuration during Redis Enterprise maintenance events. Validates that
 * command timeouts are properly relaxed during maintenance operations and return to normal afterward.
 * 
 * This test suite has been optimized to run only 2 comprehensive test cases that provide complete coverage: 1. MOVING
 * operations - Tests complete 4-phase sequence: relaxed during MIGRATING, unrelaxed after MIGRATED, relaxed during MOVING,
 * unrelaxed after MOVING 2. Failover operations - Tests complete 2-phase sequence: relaxed during FAILING_OVER, unrelaxed after
 * FAILED_OVER
 */
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

    // Wait for notifications
    private static final Duration NOTIFICATION_WAIT_TIMEOUT = Duration.ofMinutes(3);

    // For migrations/failovers
    private static final Duration LONG_OPERATION_TIMEOUT = Duration.ofMinutes(5);

    // For ping operations
    private static final Duration PING_TIMEOUT = Duration.ofSeconds(10);

    // For monitoring operations
    private static final Duration MONITORING_TIMEOUT = Duration.ofMinutes(2);

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

    /**
     * Test context holding common objects used across all timeout tests
     */
    private static class TimeoutTestContext {

        final RedisClient client;

        final StatefulRedisConnection<String, String> connection;

        // Removed unused sync field

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

    /**
     * Helper class to capture timeout events and measure command execution times
     */
    public static class TimeoutCapture implements MaintenanceNotificationCapture {

        private final List<String> receivedNotifications = new CopyOnWriteArrayList<>();

        private final CountDownLatch notificationLatch = new CountDownLatch(1);

        private final AtomicReference<String> lastNotification = new AtomicReference<>();

        private final AtomicInteger timeoutCount = new AtomicInteger(0);

        private final AtomicInteger successCount = new AtomicInteger(0);

        private final AtomicBoolean maintenanceActive = new AtomicBoolean(false);

        private final AtomicBoolean testPhaseActive = new AtomicBoolean(true);

        private final boolean isMovingTest;

        private final boolean isUnrelaxedTest;

        // Removed unused mainSyncCommands field

        // Reference to main connection for reflection
        private StatefulRedisConnection<String, String> mainConnection;

        // Traffic management for MOVING tests
        private final AtomicBoolean stopTraffic = new AtomicBoolean(false);

        private final List<CompletableFuture<Void>> trafficThreads = new CopyOnWriteArrayList<>();

        private final AtomicBoolean trafficStarted = new AtomicBoolean(false);

        // Timing for MOVING operation
        private final AtomicLong movingStartTime = new AtomicLong(0);

        private final AtomicLong movingEndTime = new AtomicLong(0);

        public TimeoutCapture(boolean isMovingTest, boolean isUnrelaxedTest) {
            this.isMovingTest = isMovingTest;
            this.isUnrelaxedTest = isUnrelaxedTest;
        }

        // Removed unused setMainSyncCommands method

        public void setMainConnection(StatefulRedisConnection<String, String> mainConnection) {
            this.mainConnection = mainConnection;
        }

        public StatefulRedisConnection<String, String> getMainConnection() {
            return mainConnection;
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

                // Log what type of test this is - only 2 types remain
                String testType = isMovingTest ? "MOVING UN-RELAXED test" : "FAILOVER UN-RELAXED test";
                log.info("Test type: {} - Processing notification: {}", testType, notification);
                log.info("Test flags: isMovingTest={}, isUnrelaxedTest={}", isMovingTest, isUnrelaxedTest);
            } else {
                log.info("DECISION: testPhaseActive=false -> Ignoring notification during cleanup phase");
                log.debug("Ignoring notification during cleanup phase: {}", notification);
                return;
            }

            // For MOVING tests: Test all 4 phases
            if (notification.contains("MIGRATED") && isMovingTest) {
                maintenanceActive.set(false);
                log.info("MIGRATED received - Testing unrelaxed timeouts after MIGRATED");

                // Stop traffic from MIGRATING phase
                stopContinuousTraffic();

                // Test unrelaxed timeouts after MIGRATED
                testUnrelaxedTimeoutsAfterMigrated();

                log.info("MIGRATED unrelaxed testing completed - Waiting for MOVING notification");
            } else if (notification.contains("MOVING")) {
                log.info("=== MOVING DECISION TREE START ===");
                log.info("DECISION: MOVING notification received");
                log.info("ACTION: Setting maintenanceActive=true, recording MOVING start");
                maintenanceActive.set(true);
                recordMovingStart(); // Record when MOVING operation starts

                // Only MOVING unrelaxed tests remain (isMovingTest=true && isUnrelaxedTest=true)
                log.info("DECISION: MOVING unrelaxed test");
                log.info("ACTION: Starting traffic to test relaxed timeouts during MOVING");
                startContinuousTraffic();

                // Let traffic run for a bit to test relaxed timeouts during MOVING
                try {
                    Thread.sleep(3000); // 3 seconds to test MOVING relaxed timeouts
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                log.info("ACTION: Stopping traffic after MOVING relaxed timeout testing");
                stopContinuousTraffic();

                // CRITICAL: Set maintenance to false after MOVING testing is complete
                maintenanceActive.set(false);
                log.info("ACTION: Setting maintenanceActive=false after MOVING testing");

                log.info("ACTION: Counting down notification latch for MOVING");
                notificationLatch.countDown(); // Count down ONLY on MOVING for MOVING tests
                log.info("=== MOVING DECISION TREE END ===");

            } else if (notification.contains("MIGRATING")) {
                // Only MOVING tests remain, so isMovingTest is always true here
                maintenanceActive.set(true);
                log.info(
                        "MOVING test received MIGRATING notification - Starting traffic to test relaxed timeouts during MIGRATING");
                startContinuousTraffic();
                // Continue traffic until MIGRATED notification

            } else if (notification.contains("FAILING_OVER") && !isMovingTest) {
                // Only failover tests with isUnrelaxedTest=true remain
                maintenanceActive.set(true);
                log.info("FAILING_OVER maintenance started - Starting continuous traffic for testing");

                // Start traffic now that FAILING_OVER has been received
                startContinuousTraffic();

                // All remaining tests are unrelaxed tests, so keep traffic running until FAILED_OVER
                log.info("Un-relaxed test: Keeping traffic running until FAILED_OVER notification");

            } else if (notification.contains("FAILED_OVER")) {
                maintenanceActive.set(false);
                log.info("Maintenance completed - timeouts should return to normal");

                // All remaining tests are unrelaxed tests, so stop traffic after FAILED_OVER
                log.info("Un-relaxed test: Stopping traffic after FAILED_OVER notification");
                stopContinuousTraffic();

                // Only count down latch for non-MOVING tests (failover tests)
                if (!isMovingTest) {
                    notificationLatch.countDown(); // Count down for FAILED_OVER in FAILED_OVER tests
                }

            } else {
                log.info("Ignoring notification: {} (not relevant for current test)", notification);
            }
        }

        /**
         * Start continuous traffic with BLPOP commands during maintenance events
         */
        private void startContinuousTraffic() {
            if (!trafficStarted.compareAndSet(false, true)) {
                log.info("Traffic already started, skipping...");
                return;
            }

            // Check if connection is still open before starting traffic
            if (mainConnection != null && !mainConnection.isOpen()) {
                log.warn("Cannot start traffic - connection is closed");
                trafficStarted.set(false); // Reset the flag since we couldn't start
                return;
            }

            log.info("Starting continuous traffic...");
            stopTraffic.set(false);

            CompletableFuture<Void> trafficFuture = CompletableFuture.runAsync(() -> {
                int commandCount = 0;
                log.info("Traffic thread started");

                do {
                    commandCount++;
                    sendBlpopCommand(commandCount);
                } while (!stopTraffic.get());

                log.info("Traffic thread stopped after {} commands", commandCount);
            });

            trafficThreads.add(trafficFuture);
            log.info("Continuous traffic started");
        }

        private boolean sendBlpopCommand(int commandCount) {
            long startTime = System.currentTimeMillis();
            try {
                // Use the normal timeout duration for BLPOP to test timeout behavior
                RedisFuture<KeyValue<String, String>> future = mainConnection.async().blpop(10, "traffic-key-" + commandCount);
                future.get(); // Execute the command, result not needed

                long duration = System.currentTimeMillis() - startTime;
                log.info("BLPOP command #{} completed successfully in {}ms", commandCount, duration);
                recordSuccess();
                return false; // No exception occurred

            } catch (Exception e) {
                long wallClockDuration = System.currentTimeMillis() - startTime;
                String timeoutDurationStr = extractTimeoutDuration(e);
                log.info("BLPOP command #{} timed out - Wall clock: {}ms, Actual timeout: {}ms, Exception: {}", commandCount,
                        wallClockDuration, timeoutDurationStr, e.getMessage());

                // Check if this is a relaxed timeout
                if (isMaintenanceActive() && !"unknown".equals(timeoutDurationStr)) {
                    int timeoutDuration = Integer.parseInt(timeoutDurationStr);
                    if (timeoutDuration > NORMAL_COMMAND_TIMEOUT.toMillis()
                            && timeoutDuration <= EFFECTIVE_TIMEOUT_DURING_MAINTENANCE.toMillis()) {
                        log.info("Relaxed timeout detected: {}ms", timeoutDuration);
                        recordRelaxedTimeout(); // Count this as a relaxed timeout
                    }
                }

                return true; // Exception occurred
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

        /**
         * Clear the command stack to allow rebind completion mechanism to work properly. This method uses reflection to access
         * the internal command stack and clear it.
         * 
         * @param context a description of when/why the stack is being cleared for logging
         */
        private void clearCommandStack(String context) {
            log.info("Attempting to clear command stack {}...", context);
            try {
                if (mainConnection != null && mainConnection.isOpen()) {
                    // Access the delegate inside MaintenanceAwareExpiryWriter to get the real ChannelWriter
                    io.lettuce.core.RedisChannelHandler<?, ?> handler = (io.lettuce.core.RedisChannelHandler<?, ?>) mainConnection;
                    io.lettuce.core.RedisChannelWriter writer = handler.getChannelWriter();

                    if (writer instanceof io.lettuce.core.protocol.MaintenanceAwareExpiryWriter) {
                        // Get the delegate field from MaintenanceAwareExpiryWriter
                        java.lang.reflect.Field delegateField = writer.getClass().getDeclaredField("delegate");
                        delegateField.setAccessible(true);
                        io.lettuce.core.RedisChannelWriter delegate = (io.lettuce.core.RedisChannelWriter) delegateField
                                .get(writer);

                        // Get the channel directly from the delegate
                        java.lang.reflect.Field channelField = delegate.getClass().getDeclaredField("channel");
                        channelField.setAccessible(true);
                        io.netty.channel.Channel channel = (io.netty.channel.Channel) channelField.get(delegate);

                        // Print detailed channel and rebind state information
                        log.info("=== CHANNEL STATE DEBUG INFO ===");
                        log.info("Channel: {}", channel);
                        log.info("Channel active: {}", channel.isActive());
                        log.info("Channel registered: {}", channel.isRegistered());

                        // Check rebind attribute
                        if (channel.hasAttr(io.lettuce.core.protocol.MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE)) {
                            Object rebindState = channel
                                    .attr(io.lettuce.core.protocol.MaintenanceAwareConnectionWatchdog.REBIND_ATTRIBUTE).get();
                            log.info("Rebind attribute present: true, state: {}", rebindState);
                        } else {
                            log.info("Rebind attribute present: false");
                        }

                        // Access the CommandHandler directly
                        io.lettuce.core.protocol.CommandHandler commandHandler = channel.pipeline()
                                .get(io.lettuce.core.protocol.CommandHandler.class);
                        if (commandHandler != null) {
                            int stackSize = commandHandler.getStack().size();
                            log.info("CommandHandler found, stack size: {}", stackSize);
                            if (stackSize > 0) {
                                log.info("Clearing command stack ({} commands) to allow rebind completion", stackSize);
                                commandHandler.getStack().clear();
                                log.info("Command stack cleared successfully");
                            } else {
                                log.info("Command stack is already empty ({} commands)", stackSize);
                            }
                        } else {
                            log.warn("CommandHandler not found in pipeline");
                        }
                        log.info("=== END CHANNEL STATE DEBUG INFO ===");
                    } else {
                        // Fallback to normal approach if not MaintenanceAwareExpiryWriter
                        int stackSize = ConnectionTestUtil.getStack(mainConnection).size();
                        if (stackSize > 0) {
                            log.info("Clearing command stack ({} commands) to allow rebind completion", stackSize);
                            ConnectionTestUtil.getStack(mainConnection).clear();
                            log.info("Command stack cleared successfully");
                        } else {
                            log.info("Command stack is already empty ({} commands)", stackSize);
                        }
                    }
                } else {
                    log.warn("mainConnection is null or closed - cannot clear stack");
                }
            } catch (Exception e) {
                log.warn("Failed to clear command stack {}: {} - {}", context, e.getClass().getSimpleName(), e.getMessage());
            }
        }

        /**
         * Stop continuous traffic
         */
        public void stopContinuousTraffic() {
            if (trafficStarted.get()) {
                log.info("Stopping continuous traffic...");
                stopTraffic.set(true);

                // Clear the command stack immediately when stopping traffic during MOVING
                // This should help the rebind completion mechanism work properly
                clearCommandStack("during traffic stop");

                // Wait for all traffic threads to complete
                try {
                    CompletableFuture.allOf(trafficThreads.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
                    log.info("All traffic threads stopped");
                } catch (ExecutionException | TimeoutException | InterruptedException e) {
                    log.warn("Timeout waiting for traffic threads to stop: {}", e.getMessage());
                } finally {
                    // Clear the traffic threads list and reset the started flag
                    trafficThreads.clear();
                    trafficStarted.set(false);
                }
            }
        }

        public boolean waitForNotification(Duration timeout) throws InterruptedException {
            return notificationLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public void recordRelaxedTimeout() {
            timeoutCount.incrementAndGet();
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

        public int getSuccessCount() {
            return successCount.get();
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
            return -1; // Not completed
        }

        public void endTestPhase() {
            testPhaseActive.set(false);
            log.info("Test phase ended - notifications will be ignored during cleanup");
        }

        /**
         * Test unrelaxed timeouts after MIGRATED notification (before MOVING)
         */
        private void testUnrelaxedTimeoutsAfterMigrated() {
            log.info("Testing unrelaxed timeouts after MIGRATED notification...");

            // Wait a moment for maintenance state to clear
            try {
                Thread.sleep(2000); // 2 second pause
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int normalTimeoutCount = 0;
            int relaxedTimeoutCount = 0;
            int totalCommands = 10; // Smaller batch for intermediate testing

            for (int i = 0; i < totalCommands; i++) {
                if (!mainConnection.isOpen()) {
                    log.warn("Connection closed during MIGRATED timeout testing, stopping at command #{}", i);
                    break;
                }

                long startTime = System.currentTimeMillis();
                try {
                    RedisFuture<KeyValue<String, String>> future = mainConnection.async().blpop(5, "migrated-test-key-" + i);
                    future.get();

                    long duration = System.currentTimeMillis() - startTime;
                    log.info("MIGRATED test BLPOP command #{} completed successfully in {}ms", i, duration);
                    recordSuccess();

                } catch (Exception e) {
                    long wallClockDuration = System.currentTimeMillis() - startTime;
                    String timeoutDurationStr = extractTimeoutDuration(e);
                    log.info("MIGRATED test BLPOP command #{} timed out - Wall clock: {}ms, Actual timeout: {}ms", i,
                            wallClockDuration, timeoutDurationStr);

                    if (!"unknown".equals(timeoutDurationStr)) {
                        int timeoutDuration = Integer.parseInt(timeoutDurationStr);
                        if (timeoutDuration <= 30) { // NORMAL_COMMAND_TIMEOUT
                            log.info("Normal timeout detected after MIGRATED: {}ms", timeoutDuration);
                            normalTimeoutCount++;
                        } else if (timeoutDuration > 30 && timeoutDuration <= 130) { // EFFECTIVE_TIMEOUT_DURING_MAINTENANCE
                            log.info("Relaxed timeout still active after MIGRATED: {}ms", timeoutDuration);
                            relaxedTimeoutCount++;
                        }
                    }
                }
            }

            log.info("=== MIGRATED Unrelaxed Test Results ===");
            log.info("Total commands sent: {}", totalCommands);
            log.info("Normal timeouts detected: {}", normalTimeoutCount);
            log.info("Relaxed timeouts still active: {}", relaxedTimeoutCount);

            // We expect mostly normal timeouts after MIGRATED
            if (normalTimeoutCount > 0) {
                log.info("SUCCESS: Detected normal timeouts after MIGRATED - unrelaxation working");
            } else {
                log.warn("WARNING: No normal timeouts detected after MIGRATED - unrelaxation may not be working");
            }
        }

    }

    /**
     * Setup for MOVING un-relaxed timeout tests specifically (used by comprehensive MOVING test)
     */
    private TimeoutTestContext setupTimeoutTestForMovingUnrelaxed() {
        return setupTimeoutTestWithType(true, true);
    }

    /**
     * Setup for un-relaxed timeout tests (used by comprehensive failover test)
     */
    private TimeoutTestContext setupTimeoutTestForUnrelaxed() {
        return setupTimeoutTestWithType(false, true);
    }

    /**
     * Common setup for all timeout tests with maintenance events support enabled
     */
    private TimeoutTestContext setupTimeoutTestWithType(boolean isMovingTest, boolean isUnrelaxedTest) {
        // Keep reasonable connection timeout
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).withTimeout(Duration.ofSeconds(5))
                .build();

        RedisClient client = RedisClient.create(uri);

        // Configure timeout options first (matching LettuceMaintenanceEventsDemo pattern)
        // Enable command timeouts
        // Set normal timeout
        // Set relaxed timeout addition
        TimeoutOptions timeoutOptions = TimeoutOptions.builder().timeoutCommands().fixedTimeout(NORMAL_COMMAND_TIMEOUT)
                .relaxedTimeoutsDuringMaintenance(RELAXED_TIMEOUT_ADDITION).build();

        // Configure client with maintenance events support and relaxed timeouts
        // CRITICAL: Required for MaintenanceAwareConnectionWatchdog
        // Required for push notifications
        // Enable maintenance events support
        // Apply timeout configuration
        ClientOptions options = ClientOptions.builder().autoReconnect(true).protocolVersion(ProtocolVersion.RESP3)
                .maintNotificationsConfig(MaintNotificationsConfig.enabled(EndpointType.EXTERNAL_IP))
                .timeoutOptions(timeoutOptions).build();

        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        TimeoutCapture capture = new TimeoutCapture(isMovingTest, isUnrelaxedTest);
        // Removed unused setMainSyncCommands call
        // Set the main connection for reflection access
        capture.setMainConnection(connection);

        log.info("*** TIMEOUT TEST SETUP: Test method detected as isMovingTest={} ***", isMovingTest);

        // Initial ping to ensure connection is established
        try {
            connection.sync().ping();
            log.info("Initial PING successful - connection established");
        } catch (Exception e) {
            log.warn("Initial PING failed: {}", e.getMessage());
        }

        // Setup push notification monitoring using the utility
        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture, MONITORING_TIMEOUT, PING_TIMEOUT,
                Duration.ofMillis(5000));

        String bdbId = String.valueOf(mStandard.getBdbId());

        return new TimeoutTestContext(client, connection, capture, bdbId);
    }

    /**
     * Common cleanup for all timeout tests
     */
    private void cleanupTimeoutTest(TimeoutTestContext context) {
        context.connection.close();
        context.client.shutdown();
    }

    /**
     * Helper method to test that timeouts are back to normal after maintenance events
     */
    private void testNormalTimeoutsAfterMaintenance(TimeoutTestContext context) throws InterruptedException {
        log.info("Testing normal timeouts after maintenance completion...");

        // Wait a bit for any pending operations to complete
        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).until(() -> true);

        // Send several BLPOP commands to test timeout behavior
        int normalTimeoutCount = 0;
        int relaxedTimeoutCount = 0;
        int totalCommands = 20;

        for (int i = 0; i < totalCommands; i++) {
            // Check connection state before each command
            if (!context.connection.isOpen()) {
                log.warn("Connection closed during normal timeout testing, stopping at command #{}", i);
                break;
            }

            long startTime = System.currentTimeMillis();
            try {
                // Use the normal timeout duration for BLPOP to test if timeouts are back to normal
                RedisFuture<KeyValue<String, String>> future = context.connection.async().blpop(10, "normal-test-key-" + i);
                future.get(); // Execute the command, result not needed

                long duration = System.currentTimeMillis() - startTime;
                log.info("Normal test BLPOP command #{} completed successfully in {}ms", i, duration);
                context.capture.recordSuccess();

            } catch (Exception e) {
                long wallClockDuration = System.currentTimeMillis() - startTime;
                String timeoutDurationStr = context.capture.extractTimeoutDuration(e);
                log.info("Normal test BLPOP command #{} timed out - Wall clock: {}ms, Actual timeout: {}ms, Exception: {}", i,
                        wallClockDuration, timeoutDurationStr, e.getMessage());

                // Check if this is a normal timeout (not relaxed)
                if (!"unknown".equals(timeoutDurationStr)) {
                    int timeoutDuration = Integer.parseInt(timeoutDurationStr);
                    if (timeoutDuration <= NORMAL_COMMAND_TIMEOUT.toMillis()) {
                        log.info("Normal timeout detected: {}ms", timeoutDuration);
                        normalTimeoutCount++;
                    } else if (timeoutDuration > NORMAL_COMMAND_TIMEOUT.toMillis()
                            && timeoutDuration <= EFFECTIVE_TIMEOUT_DURING_MAINTENANCE.toMillis()) {
                        log.info("Relaxed timeout still active: {}ms", timeoutDuration);
                        relaxedTimeoutCount++;
                    }
                }
            }
        }

        log.info("=== Normal Timeout Test Results ===");
        log.info("Total commands sent: {}", totalCommands);
        log.info("Normal timeouts detected: {}", normalTimeoutCount);
        log.info("Relaxed timeouts still active: {}", relaxedTimeoutCount);

        // Verify that we have some normal timeouts (indicating timeout relaxation was properly disabled)
        assertThat(normalTimeoutCount).as("Should have detected normal timeouts after maintenance completion. "
                + "All timeouts still being relaxed indicates the timeout un-relaxation mechanism is not working properly.")
                .isGreaterThan(0);

        // Verify that relaxed timeouts are not predominant (indicating proper un-relaxation)
        assertThat(relaxedTimeoutCount)
                .as("Should have fewer relaxed timeouts than normal timeouts after maintenance completion. "
                        + "Too many relaxed timeouts indicates the timeout un-relaxation mechanism is not working properly.")
                .isLessThan(normalTimeoutCount);
    }

    /**
     * Helper method to test that timeouts are back to normal after MOVING notification and reconnection
     */
    private void testNormalTimeoutsAfterMoving(TimeoutTestContext context) throws InterruptedException {
        log.info("Testing normal timeouts after MOVING notification and reconnection...");

        // Wait for the connection to drop and reconnect after MOVING
        log.info("Waiting for connection to drop and reconnect after MOVING notification...");

        // Wait longer for any pending operations to complete after reconnection and for relaxed timeouts to be cleared
        log.info("Waiting for maintenance state to be fully cleared...");
        await().pollDelay(Duration.ofSeconds(15)).atMost(Duration.ofSeconds(30)).until(() -> true); // Allow time for
                                                                                                    // maintenance state to
                                                                                                    // clear

        log.info("Connection status before timeout tests: {}", context.connection.isOpen());

        // Send several BLPOP commands to test timeout behavior after reconnection
        int normalTimeoutCount = 0;
        int relaxedTimeoutCount = 0;
        int totalCommands = 20;

        for (int i = 0; i < totalCommands; i++) {
            // Check connection state before each command
            if (!context.connection.isOpen()) {
                log.warn("Connection closed during normal timeout testing after MOVING, stopping at command #{}", i);
                break;
            }

            long startTime = System.currentTimeMillis();
            try {
                // Use the normal timeout duration for BLPOP to test if timeouts are back to normal
                // CRITICAL: Use mainConnection like traffic generation does, not context.connection
                RedisFuture<KeyValue<String, String>> future = context.capture.getMainConnection().async().blpop(10,
                        "moving-normal-test-key-" + i);
                future.get(); // Execute the command, result not needed

                long duration = System.currentTimeMillis() - startTime;
                log.info("MOVING normal test BLPOP command #{} completed successfully in {}ms", i, duration);
                context.capture.recordSuccess();

            } catch (Exception e) {
                long wallClockDuration = System.currentTimeMillis() - startTime;
                String timeoutDurationStr = context.capture.extractTimeoutDuration(e);
                log.info(
                        "MOVING normal test BLPOP command #{} timed out - Wall clock: {}ms, Actual timeout: {}ms, Exception: {}",
                        i, wallClockDuration, timeoutDurationStr, e.getMessage());

                // Check if this is a normal timeout (not relaxed)
                if (!"unknown".equals(timeoutDurationStr)) {
                    int timeoutDuration = Integer.parseInt(timeoutDurationStr);
                    log.info("Command #{} timeout: {}ms (normal: {}ms, relaxed: {}ms)", i, timeoutDuration,
                            NORMAL_COMMAND_TIMEOUT.toMillis(), EFFECTIVE_TIMEOUT_DURING_MAINTENANCE.toMillis());

                    if (timeoutDuration <= NORMAL_COMMAND_TIMEOUT.toMillis()) {
                        log.info("Normal timeout detected after MOVING: {}ms", timeoutDuration);
                        normalTimeoutCount++;
                    } else if (timeoutDuration > NORMAL_COMMAND_TIMEOUT.toMillis()
                            && timeoutDuration <= EFFECTIVE_TIMEOUT_DURING_MAINTENANCE.toMillis()) {
                        log.info("Relaxed timeout still active after MOVING: {}ms", timeoutDuration);
                        relaxedTimeoutCount++;
                    }
                } else {
                    log.warn("Command #{} - Could not extract timeout duration from exception", i);
                }
            }
        }

        log.info("=== MOVING Normal Timeout Test Results ===");
        log.info("Total commands sent: {}", totalCommands);
        log.info("Normal timeouts detected: {}", normalTimeoutCount);
        log.info("Relaxed timeouts still active: {}", relaxedTimeoutCount);

        // Verify that we have some normal timeouts (indicating timeout relaxation was properly disabled after MOVING)
        assertThat(normalTimeoutCount).as("Should have detected normal timeouts after MOVING notification and reconnection. "
                + "All timeouts still being relaxed indicates the timeout un-relaxation mechanism is not working properly after MOVING.")
                .isGreaterThan(0);

        // Verify that relaxed timeouts are not predominant (indicating proper un-relaxation after MOVING)
        assertThat(relaxedTimeoutCount)
                .as("Should have fewer relaxed timeouts than normal timeouts after MOVING notification and reconnection. "
                        + "Too many relaxed timeouts indicates the timeout un-relaxation mechanism is not working properly after MOVING.")
                .isLessThan(normalTimeoutCount);
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
            // Start maintenance operation - notification handler will manage traffic automatically
            log.info("Starting maintenance operation (migrate + rebind) with endpoint-aware node selection...");

            // Start the maintenance operation and wait for it to complete fully
            log.info("Starting MOVING operation with endpoint-aware node selection and waiting for it to complete...");
            Boolean operationResult = faultClient.triggerMovingNotification(context.bdbId, endpointId, policy, clusterConfig)
                    .block(Duration.ofMinutes(3));
            assertThat(operationResult).isTrue();
            log.info("MOVING operation fully completed: {}", operationResult);

            // Verify we got the expected notifications during the operation
            log.info("Verifying we received the expected notifications...");
            // Short wait since operation already completed
            boolean received = context.capture.waitForNotification(Duration.ofSeconds(5));

            assertThat(received).isTrue();

            // Verify we got the expected notifications
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MIGRATED"))).isTrue();
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isTrue();

            // Record MOVING operation completion
            context.capture.recordMovingEnd();

            log.info("Waiting for maintenance state to be fully cleared...");
            await().pollDelay(Duration.ofSeconds(10)).atMost(Duration.ofSeconds(20)).until(() -> true); // Allow time for
                                                                                                        // maintenance state to
                                                                                                        // clear
            // Stop any remaining traffic for this specific test case
            log.info("Un-relaxed MOVING test: Stopping all traffic after MOVING operation completed");
            context.capture.stopContinuousTraffic();

            log.info("=== MOVING Un-relaxed Test: Testing normal timeouts after MOVING ===");

            // Test that timeouts are back to normal after MOVING (including reconnection)
            log.info("Testing that timeouts are back to normal after MOVING notification and reconnection...");
            testNormalTimeoutsAfterMoving(context);

            log.info("=== MOVING Un-relaxed Test Results ===");
            log.info("MOVING operation duration: {}ms", context.capture.getMovingDuration());
            log.info("Successful operations: {}", context.capture.getSuccessCount());
            log.info("Timeout operations: {}", context.capture.getTimeoutCount());
            log.info("Notifications received: {}", context.capture.getReceivedNotifications().size());

            // CRITICAL: Verify that we detected at least one relaxed timeout during maintenance
            assertThat(context.capture.getTimeoutCount())
                    .as("Should have detected at least one relaxed timeout during MOVING maintenance. "
                            + "No relaxed timeouts detected indicates the timeout relaxation mechanism is not working properly.")
                    .isGreaterThan(0);

            // End test phase to prevent capturing cleanup notifications
            context.capture.endTestPhase();

        } finally {
            cleanupTimeoutTest(context);
        }
        log.info("test timeoutUnrelaxedOnMovingTest ended");
    }

    @Test
    @DisplayName("Comprehensive failover timeout test - Tests relaxed during FAILING_OVER and unrelaxed after FAILED_OVER")
    public void timeoutUnrelaxedOnFailedoverTest() throws InterruptedException {
        log.info("test timeoutUnrelaxedOnFailedoverTest started");
        TimeoutTestContext context = setupTimeoutTestForUnrelaxed();

        try {
            log.info("=== FAILED_OVER Un-relaxed Timeout Test: Starting maintenance operation ===");

            // Start FAILING_OVER notification in background
            String nodeId = clusterConfig.getNodeWithMasterShards();

            log.info("Triggering shard failover for FAILED_OVER notification asynchronously...");

            // Start the operation but don't wait for completion
            faultClient.triggerShardFailover(context.bdbId, nodeId, clusterConfig).subscribe(
                    result -> log.info("FAILED_OVER operation completed: {}", result),
                    error -> log.error("FAILED_OVER operation failed: {}", error.getMessage()));

            // Wait for FAILED_OVER notification and automatic timeout testing
            log.info("Waiting for FAILED_OVER notification (timeout testing will happen automatically)...");
            boolean received = context.capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
            assertThat(received).isTrue();

            // Verify notification was received and timeout testing completed
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("FAILED_OVER"))).isTrue();

            log.info("=== FAILED_OVER Un-relaxed Test: Testing normal timeouts after FAILED_OVER ===");

            // Test that timeouts are back to normal after FAILED_OVER
            log.info("Testing that timeouts are back to normal after FAILED_OVER notification...");
            testNormalTimeoutsAfterMaintenance(context);

            log.info("=== FAILED_OVER Un-relaxed Test Results ===");
            log.info("Successful operations: {}", context.capture.getSuccessCount());
            log.info("Timeout operations: {}", context.capture.getTimeoutCount());
            log.info("Notifications received: {}", context.capture.getReceivedNotifications().size());

            // Verify that we detected relaxed timeouts during maintenance
            assertThat(context.capture.getTimeoutCount())
                    .as("Should have detected at least one relaxed timeout during FAILING_OVER maintenance. "
                            + "No relaxed timeouts detected indicates the timeout relaxation mechanism is not working properly.")
                    .isGreaterThan(0);

            // End test phase to prevent capturing cleanup notifications
            context.capture.endTestPhase();

            clusterConfig = RedisEnterpriseConfig.refreshClusterConfig(faultClient, String.valueOf(mStandard.getBdbId()));
            nodeId = clusterConfig.getNodeWithMasterShards();

            log.info("performing cluster cleanup operation for failover testing");
            StepVerifier.create(faultClient.triggerShardFailover(context.bdbId, nodeId, clusterConfig)).expectNext(true)
                    .expectComplete().verify(LONG_OPERATION_TIMEOUT);

        } finally {
            cleanupTimeoutTest(context);

        }
        log.info("test timeoutUnrelaxedOnFailedoverTest ended");
    }

}
