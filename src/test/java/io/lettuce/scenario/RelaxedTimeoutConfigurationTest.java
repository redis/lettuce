package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;
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
    private static final int TEST_COMMAND_COUNT = 10;

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

                log.info("ACTION: Stopping traffic after MOVING relaxed timeout testing");
                stopContinuousTraffic();

                // CRITICAL: Set maintenance to false after MOVING testing is complete
                maintenanceActive.set(false);
                log.info("ACTION: Setting maintenanceActive=false after MOVING testing");

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
                RedisFuture<KeyValue<String, String>> future = mainConnection.async().blpop(BLPOP_TIMEOUT.toMillis(),
                        "traffic-key-" + commandCount);
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

                // Check timeout behavior during maintenance
                if (isMaintenanceActive() && !"unknown".equals(timeoutDurationStr)) {
                    int timeoutDuration = Integer.parseInt(timeoutDurationStr);
                    if (timeoutDuration > NORMAL_COMMAND_TIMEOUT.toMillis()
                            && timeoutDuration <= EFFECTIVE_TIMEOUT_DURING_MAINTENANCE.toMillis()) {
                        log.info("Relaxed timeout detected during maintenance: {}ms", timeoutDuration);
                        recordRelaxedTimeout(); // Count this as a relaxed timeout
                    } else if (timeoutDuration <= NORMAL_COMMAND_TIMEOUT.toMillis()) {
                        log.warn(
                                "UNEXPECTED: Normal timeout detected during maintenance: {}ms - This indicates relaxed timeout mechanism is not working!",
                                timeoutDuration);
                        recordNormalTimeoutDuringMaintenance(); // Count this as an error
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

            int normalTimeoutCount = 0;
            int relaxedTimeoutCount = 0;
            int totalCommands = TEST_COMMAND_COUNT; // Smaller batch for intermediate testing

            for (int i = 0; i < totalCommands; i++) {
                if (!mainConnection.isOpen()) {
                    log.warn("Connection closed during MIGRATED timeout testing, stopping at command #{}", i);
                    break;
                }

                long startTime = System.currentTimeMillis();
                try {
                    RedisFuture<KeyValue<String, String>> future = mainConnection.async().blpop(BLPOP_TIMEOUT.getSeconds(),
                            "migrated-test-key-" + i);
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

            // STRICT VERIFICATION: After MIGRATED notification, ALL commands should fail with normal timeout
            // No mixing of timeout types should occur - this ensures the timeout un-relaxation mechanism works correctly after
            // MIGRATED
            assertThat(normalTimeoutCount).as("After MIGRATED notification, ALL commands should fail with normal timeout. "
                    + "Found {} normal timeouts out of {} total commands. "
                    + "Mixed timeout behavior indicates the timeout un-relaxation mechanism is not working properly after MIGRATED.",
                    normalTimeoutCount, totalCommands).isEqualTo(totalCommands);

            assertThat(relaxedTimeoutCount).as("After MIGRATED notification, NO commands should fail with relaxed timeout. "
                    + "Found {} relaxed timeouts out of {} total commands. "
                    + "Any relaxed timeouts indicate the timeout un-relaxation mechanism is not working properly after MIGRATED.",
                    relaxedTimeoutCount, totalCommands).isEqualTo(0);
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

        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture);

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

        // Send several BLPOP commands to test timeout behavior
        int normalTimeoutCount = 0;
        int relaxedTimeoutCount = 0;
        int totalCommands = TEST_COMMAND_COUNT;

        for (int i = 0; i < totalCommands; i++) {
            // Check connection state before each command
            if (!context.connection.isOpen()) {
                log.warn("Connection closed during normal timeout testing, stopping at command #{}", i);
                break;
            }

            long startTime = System.currentTimeMillis();
            try {
                // Use the normal timeout duration for BLPOP to test if timeouts are back to normal
                RedisFuture<KeyValue<String, String>> future = context.connection.async().blpop(BLPOP_TIMEOUT.getSeconds(),
                        "normal-test-key-" + i);
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

        // STRICT VERIFICATION: After maintenance completion, ALL commands should fail with normal timeout
        // No mixing of timeout types should occur - this ensures the timeout un-relaxation mechanism works correctly
        assertThat(normalTimeoutCount).as(
                "After maintenance completion, ALL commands should fail with normal timeout. "
                        + "Found {} normal timeouts out of {} total commands. "
                        + "Mixed timeout behavior indicates the timeout un-relaxation mechanism is not working properly.",
                normalTimeoutCount, totalCommands).isEqualTo(totalCommands);

        assertThat(relaxedTimeoutCount).as(
                "After maintenance completion, NO commands should fail with relaxed timeout. "
                        + "Found {} relaxed timeouts out of {} total commands. "
                        + "Any relaxed timeouts indicate the timeout un-relaxation mechanism is not working properly.",
                relaxedTimeoutCount, totalCommands).isEqualTo(0);
    }

    /**
     * Helper method to test that timeouts are back to normal after MOVING notification and reconnection
     */
    private void testNormalTimeoutsAfterMoving(TimeoutTestContext context) throws InterruptedException {
        log.info("Testing normal timeouts after MOVING notification and reconnection...");

        log.info("Connection status before timeout tests: {}", context.connection.isOpen());

        // Send several BLPOP commands to test timeout behavior after reconnection
        int normalTimeoutCount = 0;
        int relaxedTimeoutCount = 0;
        int totalCommands = TEST_COMMAND_COUNT;

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
                RedisFuture<KeyValue<String, String>> future = context.capture.getMainConnection().async()
                        .blpop(BLPOP_TIMEOUT.getSeconds(), "moving-normal-test-key-" + i);
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

        assertThat(normalTimeoutCount).as(
                "After MOVING notification and reconnection, ALL commands should fail with normal timeout. "
                        + "Found {} normal timeouts out of {} total commands. "
                        + "Mixed timeout behavior indicates the timeout un-relaxation mechanism is not working properly after MOVING.",
                normalTimeoutCount, totalCommands).isEqualTo(totalCommands);

        assertThat(relaxedTimeoutCount).as(
                "After MOVING notification and reconnection, NO commands should fail with relaxed timeout. "
                        + "Found {} relaxed timeouts out of {} total commands. "
                        + "Any relaxed timeouts indicate the timeout un-relaxation mechanism is not working properly after MOVING.",
                relaxedTimeoutCount, totalCommands).isEqualTo(0);
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
            log.info("Un-relaxed MOVING test: Stopping all traffic after MOVING operation completed");
            context.capture.stopContinuousTraffic();

            log.info("=== MOVING Un-relaxed Test: Testing normal timeouts after MOVING ===");

            // Test that timeouts are back to normal after MOVING (including reconnection)
            log.info("Testing that timeouts are back to normal after MOVING notification and reconnection...");
            testNormalTimeoutsAfterMoving(context);

            log.info("=== MOVING Un-relaxed Test Results ===");
            log.info("MOVING operation duration: {}ms", context.capture.getMovingDuration());
            log.info("Successful operations: {}", context.capture.getSuccessCount());
            log.info("Relaxed timeout operations during maintenance: {}", context.capture.getTimeoutCount());
            log.info("Normal timeout operations during maintenance (should be 0): {}",
                    context.capture.getNormalTimeoutsDuringMaintenance());
            log.info("Notifications received: {}", context.capture.getReceivedNotifications().size());

            // STRICT VERIFICATION: During maintenance, ALL timeouts should be relaxed, NONE should be normal
            assertThat(context.capture.getTimeoutCount())
                    .as("Should have detected at least one relaxed timeout during MOVING maintenance. "
                            + "No relaxed timeouts detected indicates the timeout relaxation mechanism is not working properly.")
                    .isGreaterThan(0);

            assertThat(context.capture.getNormalTimeoutsDuringMaintenance()).as(
                    "During MOVING maintenance, NO commands should fail with normal timeout. "
                            + "Found {} normal timeouts during maintenance. "
                            + "Any normal timeouts during maintenance indicate the timeout relaxation mechanism is not working properly.",
                    context.capture.getNormalTimeoutsDuringMaintenance()).isEqualTo(0);

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

            // Start the operation and wait for completion
            Boolean failoverResult = faultClient.triggerShardFailover(context.bdbId, nodeId, clusterConfig)
                    .block(Duration.ofMinutes(3));
            assertThat(failoverResult).isTrue();
            log.info("FAILED_OVER operation completed: {}", failoverResult);

            // Wait for failover notifications (2 expected: FAILING_OVER + FAILED_OVER)
            boolean failoverReceived = context.capture.waitForFailoverNotifications(Duration.ofMinutes(1));
            assertThat(failoverReceived).as("Should receive failover notifications").isTrue();

            // Verify notification was received and timeout testing completed
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("FAILED_OVER"))).isTrue();

            log.info("=== FAILED_OVER Un-relaxed Test: Testing normal timeouts after FAILED_OVER ===");

            // Test that timeouts are back to normal after FAILED_OVER
            log.info("Testing that timeouts are back to normal after FAILED_OVER notification...");
            testNormalTimeoutsAfterMaintenance(context);

            log.info("=== FAILED_OVER Un-relaxed Test Results ===");
            log.info("Successful operations: {}", context.capture.getSuccessCount());
            log.info("Relaxed timeout operations during maintenance: {}", context.capture.getTimeoutCount());
            log.info("Normal timeout operations during maintenance (should be 0): {}",
                    context.capture.getNormalTimeoutsDuringMaintenance());
            log.info("Notifications received: {}", context.capture.getReceivedNotifications().size());

            // STRICT VERIFICATION: During maintenance, ALL timeouts should be relaxed, NONE should be normal
            assertThat(context.capture.getTimeoutCount())
                    .as("Should have detected at least one relaxed timeout during FAILING_OVER maintenance. "
                            + "No relaxed timeouts detected indicates the timeout relaxation mechanism is not working properly.")
                    .isGreaterThan(0);

            assertThat(context.capture.getNormalTimeoutsDuringMaintenance()).as(
                    "During FAILING_OVER maintenance, NO commands should fail with normal timeout. "
                            + "Found {} normal timeouts during maintenance. "
                            + "Any normal timeouts during maintenance indicate the timeout relaxation mechanism is not working properly.",
                    context.capture.getNormalTimeoutsDuringMaintenance()).isEqualTo(0);

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
