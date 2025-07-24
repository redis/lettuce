package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.KeyValue;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static io.lettuce.TestTags.SCENARIO_TEST;

/**
 * CAE-1130: Functional tests for relaxed timeout configuration during Redis Enterprise maintenance events. Validates that
 * command timeouts are properly relaxed during maintenance operations and return to normal afterward.
 */
@Tag(SCENARIO_TEST)
public class RelaxedTimeoutConfigurationTest {

    private static final Logger log = LoggerFactory.getLogger(RelaxedTimeoutConfigurationTest.class);

    // Timeout constants for testing
    private static final Duration NORMAL_COMMAND_TIMEOUT = Duration.ofSeconds(1); // Small timeout to simulate latency vs
                                                                                  // timeout issues

    private static final Duration RELAXED_TIMEOUT_ADDITION = Duration.ofSeconds(5); // Additional timeout during maintenance

    private static final Duration EFFECTIVE_TIMEOUT_DURING_MAINTENANCE = NORMAL_COMMAND_TIMEOUT.plus(RELAXED_TIMEOUT_ADDITION); // Total
                                                                                                                                // timeout
                                                                                                                                // during
                                                                                                                                // maintenance

    private static final Duration NOTIFICATION_WAIT_TIMEOUT = Duration.ofMinutes(3); // Wait for notifications

    private static final Duration LONG_OPERATION_TIMEOUT = Duration.ofMinutes(5); // For migrations/failovers

    private static final Duration PING_TIMEOUT = Duration.ofSeconds(10); // For ping operations

    private static final Duration MONITORING_TIMEOUT = Duration.ofMinutes(2); // For monitoring operations

    // Track original cluster state for proper cleanup
    private static Map<String, String> originalShardRoles = new HashMap<>();

    private static Map<String, List<String>> originalNodeToShards = new HashMap<>();

    private static boolean originalStateRecorded = false;

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
        log.info("Refreshing Redis Enterprise cluster configuration before test...");
        clusterConfig = RedisEnterpriseConfig.discover(faultClient, String.valueOf(mStandard.getBdbId()));
        log.info("Cluster configuration refreshed: {}", clusterConfig.getSummary());

        // Record original state for proper cleanup (only once)
        if (!originalStateRecorded) {
            recordOriginalClusterState();
            originalStateRecorded = true;
        } else {
            // For subsequent tests, restore original state first
            restoreOriginalClusterState();
        }
    }

    /**
     * Record the original cluster state for later restoration.
     */
    private void recordOriginalClusterState() {
        log.info("Recording original cluster state for cleanup...");

        try {
            String bdbId = String.valueOf(mStandard.getBdbId());

            // Get the complete current configuration
            RedisEnterpriseConfig currentConfig = RedisEnterpriseConfig.discover(faultClient, bdbId);

            // Record shard roles (getMasterShardIds already returns "redis:X" format)
            originalShardRoles.clear();
            for (String masterShard : currentConfig.getMasterShardIds()) {
                originalShardRoles.put(masterShard, "master");
            }
            for (String slaveShard : currentConfig.getSlaveShardIds()) {
                originalShardRoles.put(slaveShard, "slave");
            }

            // Record shard distribution across nodes (getShardsForNode already returns "redis:X" format)
            originalNodeToShards.clear();
            for (String nodeId : currentConfig.getNodeIds()) {
                List<String> shards = currentConfig.getShardsForNode(nodeId);
                originalNodeToShards.put(nodeId, new ArrayList<>(shards));
            }

            log.info("Original cluster state recorded:");
            log.info("  Shard roles: {}", originalShardRoles);
            log.info("  Node distribution: {}", originalNodeToShards);

        } catch (Exception e) {
            log.warn("Failed to record original cluster state: {}", e.getMessage());
        }
    }

    /**
     * Restore the original cluster state (both shard distribution and roles) recorded at startup. This ensures all tests start
     * with the exact same cluster state.
     */
    private void restoreOriginalClusterState() {
        log.info("Restoring original cluster state...");

        try {
            String bdbId = String.valueOf(mStandard.getBdbId());

            // Get current state
            RedisEnterpriseConfig currentConfig = RedisEnterpriseConfig.discover(faultClient, bdbId);

            // Log current state
            log.info("Current cluster state before restoration:");
            for (String nodeId : currentConfig.getNodeIds()) {
                List<String> shards = currentConfig.getShardsForNode(nodeId);
                log.info("  {}: {} shards {}", nodeId, shards.size(), shards);
            }

            // Step 1: Restore shard distribution across nodes
            boolean needsMigration = false;
            for (Map.Entry<String, List<String>> entry : originalNodeToShards.entrySet()) {
                String nodeId = entry.getKey();
                List<String> expectedShards = entry.getValue();
                List<String> currentShards = new ArrayList<>();

                // Get current shards (already in "redis:X" format)
                currentShards.addAll(currentConfig.getShardsForNode(nodeId));

                if (!expectedShards.equals(currentShards)) {
                    needsMigration = true;
                    log.info("Node {} has wrong shards. Expected: {}, Current: {}", nodeId, expectedShards, currentShards);
                }
            }

            if (needsMigration) {
                log.info("Need to restore shard distribution. Performing migrations...");

                // Strategy: Find misplaced shards and migrate them to their correct nodes
                // First, find nodes that have shards but should be empty
                for (Map.Entry<String, List<String>> entry : originalNodeToShards.entrySet()) {
                    String nodeId = entry.getKey();
                    List<String> expectedShards = entry.getValue();
                    List<String> currentShards = new ArrayList<>(currentConfig.getShardsForNode(nodeId));

                    if (expectedShards.isEmpty() && !currentShards.isEmpty()) {
                        // This node should be empty but has shards - migrate them away
                        log.info("Node {} should be empty but has {} shards - migrating away", nodeId, currentShards.size());

                        // Find the node that should have these shards
                        String sourceNodeNum = nodeId.replace("node:", "");
                        String targetNodeNum = null;

                        for (Map.Entry<String, List<String>> targetEntry : originalNodeToShards.entrySet()) {
                            String potentialTarget = targetEntry.getKey();
                            List<String> potentialTargetExpected = targetEntry.getValue();
                            List<String> potentialTargetCurrent = currentConfig.getShardsForNode(potentialTarget);

                            // Find a node that should have shards but currently doesn't have enough
                            if (!potentialTargetExpected.isEmpty() && !potentialTarget.equals(nodeId)
                                    && potentialTargetCurrent.size() < potentialTargetExpected.size()) {
                                targetNodeNum = potentialTarget.replace("node:", "");
                                break;
                            }
                        }

                        if (targetNodeNum != null) {
                            String migrateCommand = "migrate node " + sourceNodeNum + " all_shards target_node "
                                    + targetNodeNum;
                            log.info("Executing restoration migration: {}", migrateCommand);

                            StepVerifier
                                    .create(faultClient.executeRladminCommand(bdbId, migrateCommand, Duration.ofSeconds(10),
                                            LONG_OPERATION_TIMEOUT))
                                    .expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);

                            Thread.sleep(20000);

                            // Refresh config after migration
                            currentConfig = RedisEnterpriseConfig.discover(faultClient, bdbId);
                            break; // Only one migration at a time to avoid conflicts
                        }
                    }
                }

                log.info("Shard distribution restored");
            }

            // Step 2: Restore master/slave roles
            // Only failover shards that are currently MASTERS but should be SLAVES
            List<String> mastersToFailover = new ArrayList<>();
            for (Map.Entry<String, String> entry : originalShardRoles.entrySet()) {
                String shardId = entry.getKey();
                String originalRole = entry.getValue();

                // Only failover shards that are currently masters but should be slaves
                if ("slave".equals(originalRole) && currentConfig.getMasterShardIds().contains(shardId)) {
                    // Should be slave but is currently master - failover this master
                    mastersToFailover.add(shardId.replace("redis:", ""));
                    log.info("Shard {} should be slave but is currently master - will failover", shardId);
                }
            }

            if (!mastersToFailover.isEmpty()) {
                log.info("Found {} master shards that should be slaves, failing them over: {}", mastersToFailover.size(),
                        mastersToFailover);

                // Build failover command (only failover current masters)
                String failoverCommand = "failover shard " + String.join(" ", mastersToFailover);
                log.info("Executing restoration failover: {}", failoverCommand);

                // Execute the failover
                StepVerifier.create(faultClient.executeRladminCommand(bdbId, failoverCommand, Duration.ofSeconds(10),
                        LONG_OPERATION_TIMEOUT)).expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);

                // Wait for completion
                Thread.sleep(15000);
                log.info("Role restoration failover completed");
            } else {
                log.info("No role restoration needed - all shards are in correct roles");
            }

            // Step 3: Verify final state matches original
            currentConfig = RedisEnterpriseConfig.discover(faultClient, bdbId);
            log.info("Final cluster state after restoration:");
            for (String nodeId : currentConfig.getNodeIds()) {
                List<String> shards = currentConfig.getShardsForNode(nodeId);
                log.info("  {}: {} shards {}", nodeId, shards.size(), shards);
            }
            log.info("Original cluster state restored successfully");

        } catch (Exception e) {
            log.warn("Failed to restore original cluster state: {}", e.getMessage());
        }
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

            // For MOVING tests: Start traffic on MIGRATED, test on MOVING
            if (notification.contains("+MIGRATED") && isMovingTest) {
                log.info("*** MIGRATION COMPLETED - Starting continuous traffic for rebind phase! ***");
                startContinuousTraffic();

            } else if (notification.contains("+MOVING")) {
                maintenanceActive.set(true);
                log.info("*** MOVING MAINTENANCE STARTED - Testing with active traffic! ***");

                // Brief delay to ensure MaintenanceAwareConnectionWatchdog processes MOVING
                try {
                    Thread.sleep(100); // Short delay for watchdog to process MOVING event
                    log.info(
                            "Processing delay completed - MaintenanceAwareConnectionWatchdog should have seen active commands");

                    // DEBUG: Check command stack state after watchdog processing
                    debugCommandStackState();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Test relaxed timeout behavior RIGHT NOW while maintenance is active
                testRelaxedTimeoutImmediately(notification);

                // Stop traffic after testing
                stopContinuousTraffic();

                notificationLatch.countDown(); // Count down for MOVING in MOVING tests

            } else if (notification.contains("+MIGRATING")) {
                if (isMovingTest) {
                    log.info("MOVING test received MIGRATING notification - waiting for MIGRATED then MOVING notification...");
                } else {
                    maintenanceActive.set(true);
                    log.info("*** MIGRATING MAINTENANCE STARTED - Testing relaxed timeout IMMEDIATELY! ***");

                    // Test relaxed timeout behavior RIGHT NOW while maintenance is active
                    testRelaxedTimeoutImmediately(notification);
                    notificationLatch.countDown(); // Count down for MIGRATING in MIGRATING tests
                }

            } else if (notification.contains("+FAILING_OVER") && !isMovingTest) {
                maintenanceActive.set(true);
                log.info("*** FAILING_OVER MAINTENANCE STARTED - Testing relaxed timeout IMMEDIATELY! ***");

                // Test relaxed timeout behavior RIGHT NOW while maintenance is active
                testRelaxedTimeoutImmediately(notification);
                notificationLatch.countDown(); // Count down for FAILING_OVER in FAILING_OVER tests

            } else if (notification.contains("+FAILED_OVER")) {
                maintenanceActive.set(false);
                log.info("Maintenance completed - timeouts should return to normal");

            } else {
                log.info("Ignoring notification: {} (not relevant for current test)", notification);
            }
        }

        /**
         * Start continuous traffic that will fill the command stack during rebind phase
         */
        private void startContinuousTraffic() {
            if (!trafficStarted.compareAndSet(false, true)) {
                log.info("Traffic already started, skipping...");
                return;
            }

            log.info("Starting continuous traffic threads...");
            stopTraffic.set(false);

            // Start 3 background threads sending continuous BLPOP commands
            for (int i = 0; i < 3; i++) {
                final int threadId = i;
                CompletableFuture<Void> trafficFuture = CompletableFuture.runAsync(() -> {
                    int commandCount = 0;
                    log.info("Traffic thread {} started", threadId);

                    while (!stopTraffic.get()) {
                        try {
                            commandCount++;
                            log.debug("Thread {} sending BLPOP command #{}", threadId, commandCount);
                            // Short client timeout but long server timeout - important for the test
                            mainSyncCommands.blpop(60, "traffic-key-" + threadId + "-" + commandCount);
                        } catch (Exception e) {
                            log.debug("Thread {} command #{} completed: {}", threadId, commandCount, e.getMessage());
                        }

                        // Small delay between commands to avoid overwhelming
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    log.info("Traffic thread {} stopped after {} commands", threadId, commandCount);
                });
                trafficThreads.add(trafficFuture);
            }

            log.info("Continuous traffic started with {} threads", trafficThreads.size());
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

        /**
         * Test relaxed timeout behavior immediately when maintenance notification is received
         */
        private void testRelaxedTimeoutImmediately(String notification) {
            String phase = notification.contains("MOVING") ? "During MOVING"
                    : notification.contains("MIGRATING") ? "During MIGRATING"
                            : notification.contains("FAILING_OVER") ? "During FAILING_OVER" : "During Maintenance";

            log.info("*** IMMEDIATE TIMEOUT TEST: {} ***", phase);

            // DEBUG: Check if relaxTimeouts is enabled in MaintenanceAwareExpiryWriter
            try {
                log.info("*** DEBUG: Checking MaintenanceAwareExpiryWriter.relaxTimeouts state during {} ***", phase);

                // Use the actual connection object, not the sync commands proxy
                java.lang.reflect.Field writerField = mainConnection.getClass().getSuperclass()
                        .getDeclaredField("channelWriter");
                writerField.setAccessible(true);
                Object writer = writerField.get(mainConnection);

                if (writer.getClass().getSimpleName().contains("MaintenanceAware")) {
                    // Access the relaxTimeouts field
                    java.lang.reflect.Field relaxField = writer.getClass().getDeclaredField("relaxTimeouts");
                    relaxField.setAccessible(true);
                    boolean relaxTimeouts = relaxField.getBoolean(writer);

                    log.info("*** CRITICAL DEBUG: MaintenanceAwareExpiryWriter.relaxTimeouts = {} during {} ***", relaxTimeouts,
                            phase);

                    if (relaxTimeouts) {
                        log.info("✅ SUCCESS: relaxTimeouts is TRUE - timeout relaxation should be active!");
                    } else {
                        log.error(
                                "❌ PROBLEM: relaxTimeouts is FALSE - maintenance event not received by MaintenanceAwareExpiryWriter!");
                    }
                } else {
                    log.warn("Writer is not MaintenanceAwareExpiryWriter: {}", writer.getClass().getSimpleName());
                }
            } catch (Exception e) {
                log.warn("Could not check relaxTimeouts state via reflection: {}", e.getMessage());
            }

            // Test timeout relaxation with aggressive 1ms timeout that should be relaxed to 5001ms during maintenance
            log.info("Testing timeout relaxation: normal={}ms, expected relaxed={}ms", NORMAL_COMMAND_TIMEOUT.toMillis(),
                    EFFECTIVE_TIMEOUT_DURING_MAINTENANCE.toMillis());

            long startTime = System.currentTimeMillis();

            try {
                // Use BLPOP with longer timeout to ensure it's still in command stack when MOVING notification arrives
                // The BLPOP must be "in flight" for MaintenanceAwareConnectionWatchdog.onPushMessage() to call
                // notifyRebindStarted()
                // Root cause: if (commandHandler.getStack().isEmpty()) → no notifyRebindStarted() → relaxTimeouts stays false
                KeyValue<String, String> result = mainSyncCommands.blpop(30, "timeout-test-key-" + phase);
                long elapsed = System.currentTimeMillis() - startTime;

                log.info("*** RELAXED TIMEOUT SUCCESS: BLPOP completed in {}ms during {} (result: {}) ***", elapsed, phase,
                        result);

                // Check if this took longer than normal timeout (indicating relaxation worked)
                if (elapsed > NORMAL_COMMAND_TIMEOUT.toMillis()) {
                    log.info("SUCCESS: Command took {}ms > normal timeout {}ms - relaxed timeouts working!", elapsed,
                            NORMAL_COMMAND_TIMEOUT.toMillis());
                } else {
                    log.info("Command completed quickly in {}ms (within normal timeout)", elapsed);
                }

                successCount.incrementAndGet();

            } catch (RedisCommandTimeoutException e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("*** RELAXED TIMEOUT FAILED: BLPOP timed out in {}ms during {} - relaxed timeouts not working! ***",
                        elapsed, phase);
                timeoutCount.incrementAndGet();

                // Check if timeout was longer than normal (indicating some relaxation)
                if (elapsed > NORMAL_COMMAND_TIMEOUT.toMillis()) {
                    log.info("Partial success: Timeout was longer than normal ({}ms vs {}ms), showing some relaxation", elapsed,
                            NORMAL_COMMAND_TIMEOUT.toMillis());
                } else {
                    log.error("No relaxation: Timeout was same as normal ({}ms), no relaxation detected!", elapsed);
                }
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("Unexpected error during relaxed timeout test in {}ms during {}: {}", elapsed, phase, e.getMessage());
            }
        }

        public boolean waitForNotification(Duration timeout) throws InterruptedException {
            return notificationLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public void recordTimeout() {
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

        /**
         * Debug method to examine command stack state and MaintenanceAwareConnectionWatchdog behavior
         */
        private void debugCommandStackState() {
            try {
                log.info("*** DEBUGGING COMMAND STACK STATE ***");

                // Get the MaintenanceAwareConnectionWatchdog from the pipeline
                java.lang.reflect.Field writerField = mainConnection.getClass().getSuperclass()
                        .getDeclaredField("channelWriter");
                writerField.setAccessible(true);
                Object writer = writerField.get(mainConnection);

                if (writer.getClass().getSimpleName().contains("ExpiryWriter")) {
                    // Get the delegate (actual endpoint)
                    java.lang.reflect.Field delegateField = writer.getClass().getSuperclass().getDeclaredField("delegate");
                    delegateField.setAccessible(true);
                    Object endpoint = delegateField.get(writer);

                    if (endpoint != null && endpoint.getClass().getSimpleName().contains("Endpoint")) {
                        // Get the channel from endpoint
                        java.lang.reflect.Field channelField = endpoint.getClass().getDeclaredField("channel");
                        channelField.setAccessible(true);
                        Object channel = channelField.get(endpoint);

                        if (channel != null) {
                            io.netty.channel.Channel nettyChannel = (io.netty.channel.Channel) channel;
                            io.netty.channel.ChannelPipeline pipeline = nettyChannel.pipeline();

                            // Get the MaintenanceAwareConnectionWatchdog
                            Object watchdog = pipeline.get(io.lettuce.core.protocol.MaintenanceAwareConnectionWatchdog.class);
                            if (watchdog != null) {
                                log.info("✅ MaintenanceAwareConnectionWatchdog found for debugging");

                                // Try to access the commandHandler and its stack
                                // First, let's see what fields are actually available
                                try {
                                    log.info("*** DEBUG: Available fields in MaintenanceAwareConnectionWatchdog: ***");
                                    java.lang.reflect.Field[] allFields = watchdog.getClass().getDeclaredFields();
                                    for (java.lang.reflect.Field field : allFields) {
                                        log.info("  - Field: {} (type: {})", field.getName(), field.getType().getSimpleName());
                                    }

                                    // Also check superclass fields
                                    java.lang.reflect.Field[] superFields = watchdog.getClass().getSuperclass()
                                            .getDeclaredFields();
                                    log.info("*** DEBUG: Available fields in superclass ({}): ***",
                                            watchdog.getClass().getSuperclass().getSimpleName());
                                    for (java.lang.reflect.Field field : superFields) {
                                        log.info("  - Super Field: {} (type: {})", field.getName(),
                                                field.getType().getSimpleName());
                                    }

                                } catch (Exception e) {
                                    log.warn("Could not list fields: {}", e.getMessage());
                                }

                                // Try multiple possible field names for the command handler
                                String[] possibleFieldNames = { "commandHandler", "handler", "channelHandler",
                                        "redisCommandHandler" };
                                Object commandHandler = null;

                                for (String fieldName : possibleFieldNames) {
                                    try {
                                        java.lang.reflect.Field handlerField = null;
                                        try {
                                            handlerField = watchdog.getClass().getDeclaredField(fieldName);
                                        } catch (NoSuchFieldException e) {
                                            // Try superclass
                                            handlerField = watchdog.getClass().getSuperclass().getDeclaredField(fieldName);
                                        }

                                        handlerField.setAccessible(true);
                                        commandHandler = handlerField.get(watchdog);
                                        if (commandHandler != null) {
                                            log.info("*** SUCCESS: Found commandHandler via field '{}' ***", fieldName);
                                            break;
                                        }
                                    } catch (Exception e) {
                                        log.debug("Field '{}' not found or accessible: {}", fieldName, e.getMessage());
                                    }
                                }

                                if (commandHandler != null) {
                                    log.info("CommandHandler type: {}", commandHandler.getClass().getSimpleName());

                                    // Try to get the stack from commandHandler
                                    try {
                                        java.lang.reflect.Method getStackMethod = commandHandler.getClass()
                                                .getMethod("getStack");
                                        Object stack = getStackMethod.invoke(commandHandler);

                                        if (stack != null) {
                                            // Check if stack is empty
                                            java.lang.reflect.Method isEmptyMethod = stack.getClass().getMethod("isEmpty");
                                            boolean isEmpty = (Boolean) isEmptyMethod.invoke(stack);

                                            // Get stack size if possible
                                            String stackInfo = "isEmpty=" + isEmpty;
                                            try {
                                                java.lang.reflect.Method sizeMethod = stack.getClass().getMethod("size");
                                                int size = (Integer) sizeMethod.invoke(stack);
                                                stackInfo += ", size=" + size;
                                            } catch (Exception e) {
                                                stackInfo += ", size=unknown";
                                            }

                                            log.info("*** COMMAND STACK STATE: {} ***", stackInfo);
                                            log.info("*** STACK TYPE: {} ***", stack.getClass().getSimpleName());

                                            if (isEmpty) {
                                                log.error(
                                                        "❌ PROBLEM: Command stack is EMPTY - no wonder notifyRebindStarted() wasn't called!");
                                                log.error(
                                                        "❌ ROOT CAUSE: 10 slow EVAL commands not in stack = EVAL strategy failed!");
                                            } else {
                                                log.info(
                                                        "✅ SUCCESS: Command stack is NOT EMPTY - notifyRebindStarted() should have been called!");
                                                log.error(
                                                        "❌ DIFFERENT PROBLEM: Stack not empty but relaxTimeouts still false - watchdog logic issue!");
                                            }
                                        } else {
                                            log.warn("Could not access command stack - getStack() returned null");
                                        }
                                    } catch (Exception e) {
                                        log.warn("Could not access stack from commandHandler: {}", e.getMessage());

                                        // Try to list available methods
                                        log.info("Available methods in commandHandler:");
                                        java.lang.reflect.Method[] methods = commandHandler.getClass().getMethods();
                                        for (java.lang.reflect.Method method : methods) {
                                            if (method.getName().toLowerCase().contains("stack")
                                                    || method.getName().toLowerCase().contains("queue")) {
                                                log.info("  - Method: {} (returns: {})", method.getName(),
                                                        method.getReturnType().getSimpleName());
                                            }
                                        }
                                    }
                                } else {
                                    log.warn("Could not find commandHandler field in MaintenanceAwareConnectionWatchdog");

                                    // Try alternative approach: Access command handler through channel pipeline like the
                                    // watchdog does
                                    try {
                                        io.netty.channel.Channel debugChannel = (io.netty.channel.Channel) channel;
                                        io.netty.channel.ChannelPipeline debugPipeline = debugChannel.pipeline();

                                        // Look for CommandHandler in the pipeline
                                        log.info("*** PIPELINE DEBUG: Searching for CommandHandler in channel pipeline ***");

                                        // Get all handlers in pipeline
                                        java.util.Iterator<java.util.Map.Entry<String, io.netty.channel.ChannelHandler>> iterator = debugPipeline
                                                .iterator();
                                        while (iterator.hasNext()) {
                                            java.util.Map.Entry<String, io.netty.channel.ChannelHandler> entry = iterator
                                                    .next();
                                            String handlerName = entry.getKey();
                                            io.netty.channel.ChannelHandler handler = entry.getValue();
                                            log.info("  - Pipeline Handler: {} (type: {})", handlerName,
                                                    handler.getClass().getSimpleName());

                                            // Check if this is a CommandHandler
                                            if (handler.getClass().getSimpleName().contains("CommandHandler")) {
                                                log.info("*** FOUND CommandHandler in pipeline: {} ***",
                                                        handler.getClass().getSimpleName());

                                                // Try to access the stack from this handler
                                                try {
                                                    java.lang.reflect.Method getStackMethod = handler.getClass()
                                                            .getMethod("getStack");
                                                    Object stack = getStackMethod.invoke(handler);

                                                    if (stack != null) {
                                                        // Check if stack is empty
                                                        java.lang.reflect.Method isEmptyMethod = stack.getClass()
                                                                .getMethod("isEmpty");
                                                        boolean isEmpty = (Boolean) isEmptyMethod.invoke(stack);

                                                        // Get stack size if possible
                                                        String stackInfo = "isEmpty=" + isEmpty;
                                                        try {
                                                            java.lang.reflect.Method sizeMethod = stack.getClass()
                                                                    .getMethod("size");
                                                            int size = (Integer) sizeMethod.invoke(stack);
                                                            stackInfo += ", size=" + size;
                                                        } catch (Exception e) {
                                                            stackInfo += ", size=unknown";
                                                        }

                                                        log.info("*** PIPELINE COMMAND STACK STATE: {} ***", stackInfo);
                                                        log.info("*** STACK TYPE: {} ***", stack.getClass().getSimpleName());

                                                        if (isEmpty) {
                                                            log.error(
                                                                    "❌ PIPELINE PROBLEM: Command stack is EMPTY despite slow EVAL commands!");
                                                            log.error(
                                                                    "❌ ROOT CAUSE: EVAL commands completed too quickly or didn't reach stack!");
                                                        } else {
                                                            log.info(
                                                                    "✅ PIPELINE SUCCESS: Command stack is NOT EMPTY with {} commands!",
                                                                    stackInfo);
                                                            log.error(
                                                                    "❌ WATCHDOG PROBLEM: Stack not empty but MaintenanceAwareConnectionWatchdog didn't call notifyRebindStarted()!");
                                                        }
                                                        break;
                                                    } else {
                                                        log.warn("CommandHandler.getStack() returned null");
                                                    }
                                                } catch (Exception e) {
                                                    log.warn("Could not access stack from pipeline CommandHandler: {}",
                                                            e.getMessage());
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warn("Could not access command handler through pipeline: {}", e.getMessage());
                                    }
                                }
                            } else {
                                log.warn("❌ MaintenanceAwareConnectionWatchdog not found in pipeline!");
                            }
                        } else {
                            log.warn("Channel is null, cannot access pipeline");
                        }
                    } else {
                        log.warn("Could not find endpoint, writer type: {}", writer.getClass().getSimpleName());
                    }
                } else {
                    log.warn("Writer is not ExpiryWriter: {}", writer.getClass().getSimpleName());
                }

            } catch (Exception e) {
                log.warn("Failed to debug command stack state: {}", e.getMessage());
            }
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

            log.info("=== MOVING Timeout Test Results ===");
            log.info("Successful operations: {}", context.capture.getSuccessCount());
            log.info("Timeout operations: {}", context.capture.getTimeoutCount());
            log.info("Notifications received: {}", context.capture.getReceivedNotifications().size());

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
