package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.ByteBuffer;
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
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.RedisFuture;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static io.lettuce.TestTags.SCENARIO_TEST;

/**
 * CAE-1130: Functional tests for relaxed timeout configuration during Redis Enterprise maintenance events. Validates that
 * command timeouts are properly relaxed during maintenance operations and return to normal afterward.
 */
@Tag(SCENARIO_TEST)
public class RelaxedTimeoutConfigurationTest {

    private static final Logger log = LoggerFactory.getLogger(RelaxedTimeoutConfigurationTest.class);

    // Timeout constants for testing
    private static final Duration NORMAL_COMMAND_TIMEOUT = Duration.ofMillis(30); // Small timeout to simulate latency vs
                                                                                  // timeout issues

    private static final Duration RELAXED_TIMEOUT_ADDITION = Duration.ofMillis(100); // Additional timeout during maintenance

    private static final Duration EFFECTIVE_TIMEOUT_DURING_MAINTENANCE = NORMAL_COMMAND_TIMEOUT.plus(RELAXED_TIMEOUT_ADDITION); // Total
                                                                                                                                // timeout
                                                                                                                                // during
                                                                                                                                // maintenance

    private static final Duration NOTIFICATION_WAIT_TIMEOUT = Duration.ofMinutes(3); // Wait for notifications

    private static final Duration LONG_OPERATION_TIMEOUT = Duration.ofMinutes(5); // For migrations/failovers

    private static final Duration PING_TIMEOUT = Duration.ofSeconds(10); // For ping operations

    private static final Duration MONITORING_TIMEOUT = Duration.ofMinutes(2); // For monitoring operations

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

        final RedisCommands<String, String> sync;

        final TimeoutCapture capture;

        final String bdbId;

        TimeoutTestContext(RedisClient client, StatefulRedisConnection<String, String> connection, TimeoutCapture capture,
                String bdbId) {
            this.client = client;
            this.connection = connection;
            this.sync = connection.sync();
            this.capture = capture;
            this.bdbId = bdbId;
        }

    }

    /**
     * Helper class to capture timeout events and measure command execution times
     */
    public static class TimeoutCapture {

        private final List<String> receivedNotifications = new CopyOnWriteArrayList<>();

        private final CountDownLatch notificationLatch = new CountDownLatch(1);

        private final AtomicReference<String> lastNotification = new AtomicReference<>();

        private final AtomicInteger timeoutCount = new AtomicInteger(0);

        private final AtomicInteger successCount = new AtomicInteger(0);

        private final AtomicBoolean maintenanceActive = new AtomicBoolean(false);

        private final boolean isMovingTest;

        private RedisCommands<String, String> mainSyncCommands; // Reference to main client sync commands

        private StatefulRedisConnection<String, String> mainConnection; // Reference to main connection for reflection

        // Traffic management for MOVING tests
        private final AtomicBoolean stopTraffic = new AtomicBoolean(false);

        private final List<CompletableFuture<Void>> trafficThreads = new CopyOnWriteArrayList<>();

        private final AtomicBoolean trafficStarted = new AtomicBoolean(false);

        // Timing for MOVING operation
        private final AtomicLong movingStartTime = new AtomicLong(0);

        private final AtomicLong movingEndTime = new AtomicLong(0);

        public TimeoutCapture(boolean isMovingTest) {
            this.isMovingTest = isMovingTest;
        }

        public void setMainSyncCommands(RedisCommands<String, String> mainSyncCommands) {
            this.mainSyncCommands = mainSyncCommands;
        }

        public void setMainConnection(StatefulRedisConnection<String, String> mainConnection) {
            this.mainConnection = mainConnection;
        }

        public void captureNotification(String notification) {
            receivedNotifications.add(notification);
            lastNotification.set(notification);
            log.info("Captured push notification: {}", notification);

            // Log what type of test this is
            String testType = isMovingTest ? "MOVING test" : "OTHER test";
            log.info("Test type: {} - Processing notification: {}", testType, notification);

            // For MOVING tests: Start traffic on MOVING, test during MOVING
            if (notification.contains("+MIGRATED") && isMovingTest) {
                log.info("*** MIGRATION COMPLETED - Waiting for MOVING notification to start traffic! ***");

                // Start traffic now that MOVING has been received
                startContinuousTraffic();

            } else if (notification.contains("+MOVING")) {
                maintenanceActive.set(true);
                recordMovingStart(); // Record when MOVING operation starts
                log.info("*** MOVING MAINTENANCE STARTED - Starting continuous traffic for testing! ***");

                // Brief delay to ensure MaintenanceAwareConnectionWatchdog processes MOVING
                try {
                    Thread.sleep(100); // Short delay for watchdog to process MOVING event
                    log.info(
                            "Processing delay completed - MaintenanceAwareConnectionWatchdog should have seen active commands");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Stop traffic after testing
                stopContinuousTraffic();

                notificationLatch.countDown(); // Count down for MOVING in MOVING tests

            } else if (notification.contains("+MIGRATING")) {
                if (isMovingTest) {
                    log.info("MOVING test received MIGRATING notification - waiting for MIGRATED then MOVING notification...");
                } else {
                    maintenanceActive.set(true);
                    log.info("*** MIGRATING MAINTENANCE STARTED - Starting continuous traffic for testing! ***");

                    // Start traffic now that MIGRATING has been received
                    startContinuousTraffic();

                    // Brief delay to ensure MaintenanceAwareConnectionWatchdog processes MIGRATING
                    try {
                        Thread.sleep(100); // Short delay for watchdog to process MIGRATING event
                        log.info(
                                "Processing delay completed - MaintenanceAwareConnectionWatchdog should have seen active commands");

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    // Stop traffic after testing
                    stopContinuousTraffic();

                    notificationLatch.countDown(); // Count down for MIGRATING in MIGRATING tests
                }

            } else if (notification.contains("+FAILING_OVER") && !isMovingTest) {
                maintenanceActive.set(true);
                log.info("*** FAILING_OVER MAINTENANCE STARTED - Starting continuous traffic for testing! ***");

                // Start traffic now that FAILING_OVER has been received
                startContinuousTraffic();

                // Brief delay to ensure MaintenanceAwareConnectionWatchdog processes FAILING_OVER
                try {
                    Thread.sleep(100); // Short delay for watchdog to process FAILING_OVER event
                    log.info(
                            "Processing delay completed - MaintenanceAwareConnectionWatchdog should have seen active commands");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Stop traffic after testing
                stopContinuousTraffic();

                notificationLatch.countDown(); // Count down for FAILING_OVER in FAILING_OVER tests

            } else if (notification.contains("+FAILED_OVER")) {
                maintenanceActive.set(false);
                log.info("Maintenance completed - timeouts should return to normal");

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

            log.info("Starting continuous traffic...");
            stopTraffic.set(false);

            CompletableFuture<Void> trafficFuture = CompletableFuture.runAsync(() -> {
                int commandCount = 0;
                boolean exceptionOccurred = false;
                log.info("Traffic thread started");

                do {
                    commandCount++;
                    exceptionOccurred = sendBlpopCommand(commandCount);
                } while (!stopTraffic.get() && exceptionOccurred);

                log.info("Traffic thread stopped after {} commands", commandCount);
            });

            trafficThreads.add(trafficFuture);
            log.info("Continuous traffic started");
        }

        private boolean sendBlpopCommand(int commandCount) {
            long startTime = System.currentTimeMillis();
            try {
                RedisFuture<KeyValue<String, String>> future = mainConnection.async().blpop(10, "traffic-key-" + commandCount);
                KeyValue<String, String> result = future.get();

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
                if (isMaintenanceActive() && timeoutDurationStr != "unknown") {
                    int timeoutDuration = Integer.parseInt(timeoutDurationStr);
                    if (timeoutDuration > NORMAL_COMMAND_TIMEOUT.toMillis()
                            && timeoutDuration <= EFFECTIVE_TIMEOUT_DURING_MAINTENANCE.toMillis()) {
                        log.info("*** RELAXED TIMEOUT DETECTED: {}ms (normal: {}ms, relaxed: {}ms) ***", timeoutDuration,
                                NORMAL_COMMAND_TIMEOUT.toMillis(), EFFECTIVE_TIMEOUT_DURING_MAINTENANCE.toMillis());
                        recordRelaxedTimeout(); // Count this as a relaxed timeout
                    }
                }

                return true; // Exception occurred
            }
        }

        private String extractTimeoutDuration(Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Command timed out after")) {
                String[] parts = e.getMessage().split("Command timed out after ");
                if (parts.length > 1) {
                    return parts[1].split(" ")[0];
                }
            }
            return "unknown";
        }

        /**
         * Stop continuous traffic
         */
        private void stopContinuousTraffic() {
            if (trafficStarted.get()) {
                log.info("Stopping continuous traffic...");
                stopTraffic.set(true);

                // Wait for all traffic threads to complete
                try {
                    CompletableFuture.allOf(trafficThreads.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
                    log.info("All traffic threads stopped");
                } catch (ExecutionException | TimeoutException | InterruptedException e) {
                    log.warn("Timeout waiting for traffic threads to stop: {}", e.getMessage());
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
            log.info("*** MOVING operation STARTED at {} ***", movingStartTime.get());
        }

        public void recordMovingEnd() {
            movingEndTime.set(System.currentTimeMillis());
            long duration = movingEndTime.get() - movingStartTime.get();
            log.info("*** MOVING operation COMPLETED at {} - Total duration: {}ms ***", movingEndTime.get(), duration);
        }

        public long getMovingDuration() {
            if (movingStartTime.get() > 0 && movingEndTime.get() > 0) {
                return movingEndTime.get() - movingStartTime.get();
            }
            return -1; // Not completed
        }

    }

    /**
     * Setup for MOVING timeout tests specifically
     */
    private TimeoutTestContext setupTimeoutTestForMoving() {
        return setupTimeoutTestWithType(true);
    }

    /**
     * Common setup for all timeout tests with maintenance events support enabled
     */
    private TimeoutTestContext setupTimeoutTest() {
        return setupTimeoutTestWithType(false);
    }

    /**
     * Common setup for all timeout tests with maintenance events support enabled
     */
    private TimeoutTestContext setupTimeoutTestWithType(boolean isMovingTest) {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).withTimeout(Duration.ofSeconds(5)) // Keep
                                                                                                                         // reasonable
                                                                                                                         // connection
                                                                                                                         // timeout
                .build();

        RedisClient client = RedisClient.create(uri);

        // Configure timeout options first (matching LettuceMaintenanceEventsDemo pattern)
        TimeoutOptions timeoutOptions = TimeoutOptions.builder().timeoutCommands() // Enable command timeouts
                .fixedTimeout(NORMAL_COMMAND_TIMEOUT) // Set normal timeout
                .timeoutsRelaxingDuringMaintenance(RELAXED_TIMEOUT_ADDITION) // Set relaxed timeout addition
                .build();

        // Configure client with maintenance events support and relaxed timeouts
        ClientOptions options = ClientOptions.builder().autoReconnect(true) // CRITICAL: Required for
                                                                            // MaintenanceAwareConnectionWatchdog
                .protocolVersion(ProtocolVersion.RESP3) // Required for push notifications
                .supportMaintenanceEvents(true) // Enable maintenance events support
                .timeoutOptions(timeoutOptions) // Apply timeout configuration
                .build();

        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        TimeoutCapture capture = new TimeoutCapture(isMovingTest);
        capture.setMainSyncCommands(connection.sync()); // Set the main client sync commands
        capture.setMainConnection(connection); // Set the main connection for reflection access

        log.info("*** TIMEOUT TEST SETUP: Test method detected as isMovingTest={} ***", isMovingTest);

        // DEBUG: Check if MaintenanceAwareExpiryWriter is being used
        log.info("=== DEBUGGING TIMEOUT CONFIGURATION ===");
        log.info("Client options support maintenance events: {}", options.supportsMaintenanceEvents());
        log.info("Timeout options: {}", options.getTimeoutOptions());

        // Print actual timeout values
        TimeoutOptions timeoutOpts = options.getTimeoutOptions();
        log.info("Timeout options details:");
        log.info("  - isTimeoutCommands(): {}", timeoutOpts.isTimeoutCommands());
        log.info("  - getSource().getTimeout(null): {} ns",
                timeoutOpts.getSource() != null ? timeoutOpts.getSource().getTimeout(null) : "null");
        log.info("  - getRelaxedTimeout(): {}", timeoutOpts.getRelaxedTimeout());

        log.info("Connection class: {}", connection.getClass().getSimpleName());

        // DEBUG: Check the writer chain to see if MaintenanceAwareExpiryWriter is being used
        RedisCommands<String, String> syncCommands = connection.sync();
        log.info("Sync commands class: {}", syncCommands.getClass().getSimpleName());

        // DEBUG: Check if we can access the underlying channel writer
        try {
            // Force reflection to check the writer type - correct field name is "channelWriter"
            java.lang.reflect.Field field = connection.getClass().getSuperclass().getDeclaredField("channelWriter");
            field.setAccessible(true);
            Object writer = field.get(connection);
            log.info("*** Connection channelWriter class: {} ***", writer.getClass().getSimpleName());

            // Check if it's MaintenanceAwareExpiryWriter
            if (writer.getClass().getSimpleName().contains("MaintenanceAware")) {
                log.info("✅ MaintenanceAwareExpiryWriter is being used!");
            } else {
                log.warn("❌ Regular CommandExpiryWriter is being used instead of MaintenanceAwareExpiryWriter!");
            }
        } catch (Exception e) {
            log.warn("Could not access channelWriter via reflection: {}", e.getMessage());
        }

        // DEBUG: Check if MaintenanceAwareConnectionWatchdog is in the pipeline
        try {
            // First get the channelWriter and unwrap to find the endpoint
            java.lang.reflect.Field writerField = connection.getClass().getSuperclass().getDeclaredField("channelWriter");
            writerField.setAccessible(true);
            Object writer = writerField.get(connection);

            // The writer might be wrapped (CommandExpiryWriter, etc), so we need to find the actual endpoint
            Object endpoint = null;
            if (writer.getClass().getSimpleName().contains("ExpiryWriter")) {
                // If it's a CommandExpiryWriter, get the delegate
                java.lang.reflect.Field delegateField = writer.getClass().getSuperclass().getDeclaredField("delegate");
                delegateField.setAccessible(true);
                endpoint = delegateField.get(writer);
            } else {
                endpoint = writer;
            }

            if (endpoint != null && endpoint.getClass().getSimpleName().contains("Endpoint")) {
                java.lang.reflect.Field channelField = endpoint.getClass().getDeclaredField("channel");
                channelField.setAccessible(true);
                Object channel = channelField.get(endpoint);

                if (channel != null) {
                    io.netty.channel.Channel nettyChannel = (io.netty.channel.Channel) channel;
                    io.netty.channel.ChannelPipeline pipeline = nettyChannel.pipeline();

                    Object watchdog = pipeline.get(io.lettuce.core.protocol.MaintenanceAwareConnectionWatchdog.class);
                    if (watchdog != null) {
                        log.info("✅ MaintenanceAwareConnectionWatchdog found in pipeline: {}",
                                watchdog.getClass().getSimpleName());

                        // Check if the watchdog is registered as a PushListener
                        try {
                            java.lang.reflect.Field endpointField = endpoint.getClass().getDeclaredField("pushListeners");
                            endpointField.setAccessible(true);
                            java.util.Collection<?> pushListeners = (java.util.Collection<?>) endpointField.get(endpoint);
                            boolean isRegistered = pushListeners.contains(watchdog);
                            log.info("*** WATCHDOG REGISTRATION CHECK: Registered as PushListener: {} ***", isRegistered);
                            log.info("*** WATCHDOG REGISTRATION CHECK: Total PushListeners: {} ***", pushListeners.size());
                            for (Object listener : pushListeners) {
                                log.info("*** WATCHDOG REGISTRATION CHECK: PushListener: {} ***",
                                        listener.getClass().getSimpleName());
                            }
                        } catch (Exception e) {
                            log.warn("Could not check PushListener registration: {}", e.getMessage());
                        }
                    } else {
                        log.warn("❌ MaintenanceAwareConnectionWatchdog NOT found in pipeline!");
                        // Check for regular ConnectionWatchdog
                        Object regularWatchdog = pipeline.get(io.lettuce.core.protocol.ConnectionWatchdog.class);
                        if (regularWatchdog != null) {
                            log.warn("Found regular ConnectionWatchdog instead: {}",
                                    regularWatchdog.getClass().getSimpleName());
                        }
                    }
                } else {
                    log.warn("Channel is null, cannot check pipeline");
                }
            } else {
                log.warn("Could not find endpoint, writer type: {}", writer.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.warn("Could not access pipeline via reflection: {}", e.getMessage());
        }

        // Try to trigger a simple command to force MaintenanceAwareExpiryWriter registration
        try {
            syncCommands.ping();
            log.info("Initial PING successful - MaintenanceAwareExpiryWriter should now be registered");
        } catch (Exception e) {
            log.warn("Initial PING failed: {}", e.getMessage());
        }

        log.info("=== END DEBUG ===");

        // Setup push notification monitoring
        setupPushNotificationMonitoring(connection, capture);

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

    @Test
    @DisplayName("CAE-1130.1 - Timeout relaxed on MOVING notification")
    public void timeoutRelaxedOnMovingTest() throws InterruptedException {
        TimeoutTestContext context = setupTimeoutTestForMoving();

        try {
            log.info("=== MOVING Timeout Test: Starting maintenance operation ===");

            String endpointId = clusterConfig.getFirstEndpointId();
            String policy = "single";
            String sourceNode = clusterConfig.getOptimalSourceNode();
            String targetNode = clusterConfig.getOptimalTargetNode();

            log.info("*** CLUSTER STATE DEBUG: Source={}, Target={}, EndpointId={} ***", sourceNode, targetNode, endpointId);

            // Start maintenance operation - notification handler will manage traffic automatically
            log.info("Starting maintenance operation (migrate + rebind)...");

            // Start the maintenance operation asynchronously
            faultClient.triggerMovingNotification(context.bdbId, endpointId, policy, sourceNode, targetNode).subscribe(
                    result -> log.info("MOVING operation completed: {}", result),
                    error -> log.error("MOVING operation failed: {}", error.getMessage()));

            // Wait for MOVING notification - the capture handler will:
            // 1. Start traffic when MIGRATED is received
            // 2. Test relaxed timeouts when MOVING is received
            // 3. Stop traffic after testing
            log.info("Waiting for MOVING notification (traffic will be managed automatically)...");
            boolean received = context.capture.waitForNotification(Duration.ofMinutes(3));
            assertThat(received).isTrue();

            // Verify we got the expected notifications
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+MIGRATED"))).isTrue();
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+MOVING"))).isTrue();

            // Record MOVING operation completion
            context.capture.recordMovingEnd();

            log.info("=== MOVING Timeout Test Results ===");
            log.info("MOVING operation duration: {}ms", context.capture.getMovingDuration());
            log.info("Successful operations: {}", context.capture.getSuccessCount());
            log.info("Timeout operations: {}", context.capture.getTimeoutCount());
            log.info("Notifications received: {}", context.capture.getReceivedNotifications().size());

            // CRITICAL: Verify that we detected at least one relaxed timeout during maintenance
            assertThat(context.capture.getTimeoutCount())
                    .as("Should have detected at least one relaxed timeout during MOVING maintenance. "
                            + "No relaxed timeouts detected indicates the timeout relaxation mechanism is not working properly.")
                    .isGreaterThan(0);

        } finally {
            cleanupTimeoutTest(context);
        }
    }

    @Test
    @DisplayName("CAE-1130.3 - Timeout relaxed on MIGRATING notification")
    public void timeoutRelaxedOnMigratingTest() throws InterruptedException {
        TimeoutTestContext context = setupTimeoutTest();

        try {
            log.info("=== MIGRATING Timeout Test: Starting maintenance operation ===");

            // Start MIGRATING notification in background
            String shardId = clusterConfig.getFirstMasterShardId();
            String sourceNode = clusterConfig.getOptimalSourceNode();
            String targetNode = clusterConfig.getOptimalTargetNode();
            String intermediateNode = clusterConfig.getOptimalIntermediateNode();

            log.info("Triggering shard migration for MIGRATING notification asynchronously...");

            // Start the operation but don't wait for completion
            if (clusterConfig.canMigrateDirectly()) {
                faultClient.triggerShardMigration(context.bdbId, shardId, sourceNode, targetNode).subscribe(
                        result -> log.info("MIGRATING operation completed: {}", result),
                        error -> log.error("MIGRATING operation failed: {}", error.getMessage()));
            } else {
                faultClient
                        .triggerShardMigrationWithEmptyTarget(context.bdbId, shardId, sourceNode, targetNode, intermediateNode)
                        .subscribe(result -> log.info("MIGRATING operation completed: {}", result),
                                error -> log.error("MIGRATING operation failed: {}", error.getMessage()));
            }

            // Wait for MIGRATING notification and automatic timeout testing
            log.info("Waiting for MIGRATING notification (timeout testing will happen automatically)...");
            boolean received = context.capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
            assertThat(received).isTrue();

            // Verify notification was received and timeout testing completed
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+MIGRATING"))).isTrue();

            log.info("=== MIGRATING Timeout Test Results ===");
            log.info("Successful operations: {}", context.capture.getSuccessCount());
            log.info("Timeout operations: {}", context.capture.getTimeoutCount());
            log.info("Notifications received: {}", context.capture.getReceivedNotifications().size());

            // CRITICAL: Verify that we detected at least one relaxed timeout during maintenance
            assertThat(context.capture.getTimeoutCount())
                    .as("Should have detected at least one relaxed timeout during MIGRATING maintenance. "
                            + "No relaxed timeouts detected indicates the timeout relaxation mechanism is not working properly.")
                    .isGreaterThan(0);

        } finally {
            cleanupTimeoutTest(context);
        }
    }

    @Test
    @DisplayName("CAE-1130.5 - Timeout relaxed on FAILING_OVER notification")
    public void timeoutRelaxedOnFailoverTest() throws InterruptedException {
        TimeoutTestContext context = setupTimeoutTest();

        try {
            log.info("=== FAILING_OVER Timeout Test: Starting maintenance operation ===");

            // Start FAILING_OVER notification in background
            String shardId = clusterConfig.getFirstMasterShardId();
            String nodeId = clusterConfig.getNodeWithMasterShards();

            log.info("Triggering shard failover for FAILING_OVER notification asynchronously...");

            // Start the operation but don't wait for completion
            faultClient.triggerShardFailover(context.bdbId, shardId, nodeId, clusterConfig).subscribe(
                    result -> log.info("FAILING_OVER operation completed: {}", result),
                    error -> log.error("FAILING_OVER operation failed: {}", error.getMessage()));

            // Wait for FAILING_OVER notification and automatic timeout testing
            log.info("Waiting for FAILING_OVER notification (timeout testing will happen automatically)...");
            boolean received = context.capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
            assertThat(received).isTrue();

            // Verify notification was received and timeout testing completed
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+FAILING_OVER"))).isTrue();

            log.info("=== FAILING_OVER Timeout Test Results ===");
            log.info("Successful operations: {}", context.capture.getSuccessCount());
            log.info("Timeout operations: {}", context.capture.getTimeoutCount());
            log.info("Notifications received: {}", context.capture.getReceivedNotifications().size());

            // CRITICAL: Verify that we detected at least one relaxed timeout during maintenance
            assertThat(context.capture.getTimeoutCount())
                    .as("Should have detected at least one relaxed timeout during FAILING_OVER maintenance. "
                            + "No relaxed timeouts detected indicates the timeout relaxation mechanism is not working properly.")
                    .isGreaterThan(0);

        } finally {
            cleanupTimeoutTest(context);
        }
    }

    /**
     * Setup push notification monitoring to capture maintenance events
     */
    private void setupPushNotificationMonitoring(StatefulRedisConnection<String, String> connection, TimeoutCapture capture) {
        log.info("Setting up push notification monitoring for maintenance events...");

        PushListener maintenanceListener = new PushListener() {

            @Override
            public void onPushMessage(PushMessage message) {
                String messageType = message.getType();
                log.info("*** TEST PUSH LISTENER: PUSH MESSAGE RECEIVED: type='{}' ***", messageType);

                List<Object> content = message.getContent();
                log.info("Push message content: {}", content);

                if ("MOVING".equals(messageType)) {
                    log.info("*** MOVING push message captured! ***");
                    handleMovingMessage(content, capture);
                } else if ("MIGRATING".equals(messageType)) {
                    log.info("*** MIGRATING push message captured! ***");
                    handleMigratingMessage(content, capture);
                } else if ("MIGRATED".equals(messageType)) {
                    log.info("*** MIGRATED push message captured! ***");
                    handleMigratedMessage(content, capture);
                } else if ("FAILING_OVER".equals(messageType)) {
                    log.info("*** FAILING_OVER push message captured! ***");
                    handleFailingOverMessage(content, capture);
                } else if ("FAILED_OVER".equals(messageType)) {
                    log.info("*** FAILED_OVER push message captured! ***");
                    handleFailedOverMessage(content, capture);
                } else {
                    log.info("Other push message: type={}, content={}", messageType, content);
                }
            }

            private void handleMovingMessage(List<Object> content, TimeoutCapture capture) {
                if (content.size() >= 3) {
                    String slotNumber = content.get(1).toString();
                    String newAddress = decodeByteBuffer(content.get(2));
                    log.info("MOVING: slot {} -> {}", slotNumber, newAddress);
                    String resp3Format = String.format(">3\r\n+MOVING\r\n:%s\r\n+%s\r\n", slotNumber, newAddress);
                    capture.captureNotification(resp3Format);
                }
            }

            private void handleMigratingMessage(List<Object> content, TimeoutCapture capture) {
                if (content.size() >= 3) {
                    String slotNumber = content.get(1).toString();
                    String timestamp = content.get(2).toString();
                    log.info("MIGRATING: slot {} at timestamp {}", slotNumber, timestamp);
                    String resp3Format = String.format(">3\r\n+MIGRATING\r\n:%s\r\n:%s\r\n", timestamp, slotNumber);
                    capture.captureNotification(resp3Format);
                }
            }

            private void handleMigratedMessage(List<Object> content, TimeoutCapture capture) {
                if (content.size() >= 2) {
                    String slotNumber = content.get(1).toString();
                    log.info("MIGRATED: slot {}", slotNumber);
                    String resp3Format = String.format(">2\r\n+MIGRATED\r\n:%s\r\n", slotNumber);
                    capture.captureNotification(resp3Format);
                }
            }

            private void handleFailingOverMessage(List<Object> content, TimeoutCapture capture) {
                if (content.size() >= 3) {
                    String timestamp = content.get(1).toString();
                    String shardId = content.get(2).toString();
                    log.info("FAILING_OVER: shard {} at timestamp {}", shardId, timestamp);
                    String resp3Format = String.format(">3\r\n+FAILING_OVER\r\n:%s\r\n:%s\r\n", timestamp, shardId);
                    capture.captureNotification(resp3Format);
                }
            }

            private void handleFailedOverMessage(List<Object> content, TimeoutCapture capture) {
                if (content.size() >= 2) {
                    String shardId = content.get(1).toString();
                    log.info("FAILED_OVER: shard {}", shardId);
                    String resp3Format = String.format(">2\r\n+FAILED_OVER\r\n:%s\r\n", shardId);
                    capture.captureNotification(resp3Format);
                }
            }

            private String decodeByteBuffer(Object obj) {
                if (obj instanceof ByteBuffer) {
                    ByteBuffer buffer = (ByteBuffer) obj;
                    return io.lettuce.core.codec.StringCodec.UTF8.decodeKey(buffer);
                } else {
                    return obj.toString();
                }
            }

        };

        // Add the listener to the connection's endpoint
        connection.addListener(maintenanceListener);
        log.info("PushListener registered for maintenance event timeout testing");

        // Also trigger some activity to encourage push messages
        RedisReactiveCommands<String, String> reactive = connection.reactive();

        // Send periodic pings to trigger any pending push notifications
        Disposable monitoring = Flux.interval(Duration.ofMillis(5000)).take(MONITORING_TIMEOUT)
                .doOnNext(i -> log.info("=== Ping #{} - Activity to trigger push messages ===", i))
                .flatMap(i -> reactive.ping().timeout(PING_TIMEOUT)
                        .doOnNext(response -> log.info("Ping #{} response: '{}'", i, response)).onErrorResume(e -> {
                            log.debug("Ping #{} failed, continuing: {}", i, e.getMessage());
                            return Mono.empty();
                        }))
                .doOnComplete(() -> log.info("Push notification monitoring completed")).subscribe();

        log.info("Push notification monitoring active with timeout testing capability");
    }

}
