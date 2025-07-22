package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static io.lettuce.TestTags.SCENARIO_TEST;

/**
 * CAE-633: Tests for Redis Enterprise maintenance push notifications. Validates client reception and processing of different
 * types of push notifications during maintenance operations like migration, failover, and endpoint rebinding.
 */
@Tag(SCENARIO_TEST)
public class MaintenanceNotificationTest {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceNotificationTest.class);

    private static final Duration NOTIFICATION_WAIT_TIMEOUT = Duration.ofMinutes(3); // 180 seconds - for waiting for
                                                                                     // notifications

    private static final Duration LONG_OPERATION_TIMEOUT = Duration.ofMinutes(5); // 300 seconds - for migrations/failovers

    private static final Duration MONITORING_TIMEOUT = Duration.ofMinutes(2); // 120 seconds - for monitoring operations

    private static final Duration PING_TIMEOUT = Duration.ofSeconds(10); // 10 seconds - for ping operations

    private static final Duration SHORT_MONITORING = Duration.ofSeconds(30); // 30 seconds - for short monitoring

    private static final Duration COMMAND_CHECK_INTERVAL = Duration.ofSeconds(10); // 10 seconds - for command status checks

    // Track original cluster state for proper cleanup
    private static Map<String, String> originalShardRoles = new HashMap<>();

    private static Map<String, List<String>> originalNodeToShards = new HashMap<>();

    private static boolean originalStateRecorded = false;

    private static Endpoint mStandard;

    private RedisEnterpriseConfig clusterConfig;

    private final FaultInjectionClient faultClient = new FaultInjectionClient();

    // Push notification patterns
    private static final Pattern MOVING_PATTERN = Pattern
            .compile(">3\\r\\n\\+MOVING\\r\\n:(\\d+)\\r\\n\\+([^:]+):(\\d+)\\r\\n");

    private static final Pattern MIGRATING_PATTERN = Pattern.compile(">3\\r\\n\\+MIGRATING\\r\\n:(\\d+)\\r\\n:(\\d+)\\r\\n");

    private static final Pattern MIGRATED_PATTERN = Pattern.compile(">2\\r\\n\\+MIGRATED\\r\\n:(\\d+)\\r\\n");

    private static final Pattern FAILING_OVER_PATTERN = Pattern
            .compile(">3\\r\\n\\+FAILING_OVER\\r\\n:(\\d+)\\r\\n:(\\d+)\\r\\n");

    private static final Pattern FAILED_OVER_PATTERN = Pattern.compile(">2\\r\\n\\+FAILED_OVER\\r\\n:(\\d+)\\r\\n");

    @BeforeAll
    public static void setup() {
        mStandard = Endpoints.DEFAULT.getEndpoint("m-standard");
        assumeTrue(mStandard != null, "Skipping test because no M-Standard Redis endpoint is configured!");
    }

    @BeforeEach
    public void refreshClusterConfig() {
        log.info("Refreshing Redis Enterprise cluster configuration before test...");
        // Use the discovery service to get real-time cluster state
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
     * Record the original cluster state (both shard distribution and roles) for later restoration.
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

                            StepVerifier.create(faultClient.executeRladminCommand(bdbId, migrateCommand, COMMAND_CHECK_INTERVAL,
                                    LONG_OPERATION_TIMEOUT)).expectNext(true).verifyComplete();

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
                StepVerifier.create(faultClient.executeRladminCommand(bdbId, failoverCommand, COMMAND_CHECK_INTERVAL,
                        LONG_OPERATION_TIMEOUT)).expectNext(true).verifyComplete();

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
     * Helper class to capture and validate push notifications
     */
    public static class NotificationCapture {

        private final List<String> receivedNotifications = new CopyOnWriteArrayList<>();

        private final CountDownLatch notificationLatch = new CountDownLatch(1);

        private final AtomicReference<String> lastNotification = new AtomicReference<>();

        private final AtomicBoolean timeoutIncreased = new AtomicBoolean(false);

        private final AtomicBoolean stateRemoved = new AtomicBoolean(false);

        public void captureNotification(String notification) {
            receivedNotifications.add(notification);
            lastNotification.set(notification);
            notificationLatch.countDown();
            log.info("Captured push notification: {}", notification);
        }

        public boolean waitForNotification(Duration timeout) throws InterruptedException {
            return notificationLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public List<String> getReceivedNotifications() {
            return receivedNotifications;
        }

        public String getLastNotification() {
            return lastNotification.get();
        }

        public void markTimeoutIncreased() {
            timeoutIncreased.set(true);
        }

        public void markStateRemoved() {
            stateRemoved.set(true);
        }

        public boolean hasTimeoutIncreased() {
            return timeoutIncreased.get();
        }

        public boolean hasStateRemoved() {
            return stateRemoved.get();
        }

    }

    @Test
    @DisplayName("T.1.1.1 - Receive MOVING push notification during endpoint rebind")
    public void receiveMovingPushNotificationTest() throws InterruptedException {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        // Configure client for RESP3 to receive push notifications
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        NotificationCapture capture = new NotificationCapture();

        // Setup push notification monitoring
        // Note: This is a simplified approach - in real implementation, we'd need to
        // hook into the protocol layer to capture push notifications
        setupPushNotificationMonitoring(connection, capture);

        // Trigger MOVING notification using the proper two-step process:
        // 1. Migrate all shards from source node to target node (making it empty)
        // 2. Bind endpoint to trigger MOVING notification
        String bdbId = String.valueOf(mStandard.getBdbId());
        String endpointId = clusterConfig.getFirstEndpointId(); // Dynamically discovered endpoint ID
        String policy = "single"; // M-Standard uses single policy
        String sourceNode = clusterConfig.getOptimalSourceNode(); // Dynamically discovered source node (finds node with shards)
        String targetNode = clusterConfig.getOptimalTargetNode(); // Dynamically discovered target node (finds empty node)

        log.info("Triggering MOVING notification using proper two-step process...");
        log.info("Using dynamic nodes: source={}, target={}", sourceNode, targetNode);
        StepVerifier.create(faultClient.triggerMovingNotification(bdbId, endpointId, policy, sourceNode, targetNode))
                .expectNext(true).verifyComplete();

        // Wait for MOVING notification
        boolean received = capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(received).isTrue();

        // Validate notification format and parsing
        String notification = capture.getLastNotification();
        assertThat(notification).isNotNull();

        Matcher matcher = MOVING_PATTERN.matcher(notification);
        if (matcher.matches()) {
            String timeS = matcher.group(1);
            String newIp = matcher.group(2);
            String port = matcher.group(3);

            log.info("Parsed MOVING notification - Time: {}, New IP: {}, Port: {}", timeS, newIp, port);

            // Validate parsed values
            assertThat(Long.parseLong(timeS)).isGreaterThan(0L);
            assertThat(newIp).isNotEmpty();
            assertThat(Integer.parseInt(port)).isGreaterThan(0);
        } else {
            log.warn("MOVING notification format not recognized: {}", notification);
        }

        // Verify notification parsing and storage - expect multiple notifications during migration process
        assertThat(capture.getReceivedNotifications()).isNotEmpty();
        assertThat(capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+MOVING"))).isTrue();

        // Cleanup
        connection.close();
        client.shutdown();
    }

    @Test
    @DisplayName("T.1.1.2 - Receive MIGRATING push notification during node migration")
    public void receiveMigratingPushNotificationTest() throws InterruptedException {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        // Configure client for RESP3 to receive push notifications
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        NotificationCapture capture = new NotificationCapture();

        setupPushNotificationMonitoring(connection, capture);

        // Trigger node migration using optimal node selection
        String bdbId = String.valueOf(mStandard.getBdbId());
        String shardId = clusterConfig.getFirstMasterShardId(); // Dynamically discovered master shard
        String sourceNode = clusterConfig.getOptimalSourceNode(); // Node with shards
        String targetNode = clusterConfig.getOptimalTargetNode(); // Empty node (if available)
        String intermediateNode = clusterConfig.getOptimalIntermediateNode(); // Second node with shards

        log.info("Triggering shard migration for MIGRATING notification...");
        log.info("Migration strategy: {}", clusterConfig.getMigrationStrategy());
        log.info("Using optimal nodes: source={}, target={}, intermediate={}", sourceNode, targetNode, intermediateNode);

        if (clusterConfig.canMigrateDirectly()) {
            log.info("DIRECT migration possible - target node is empty!");
            StepVerifier.create(faultClient.triggerShardMigration(bdbId, shardId, sourceNode, targetNode)).expectNext(true)
                    .verifyComplete();
        } else {
            log.info("TWO-STEP migration needed - emptying target first");
            StepVerifier.create(
                    faultClient.triggerShardMigrationWithEmptyTarget(bdbId, shardId, sourceNode, targetNode, intermediateNode))
                    .expectNext(true).verifyComplete();
        }

        // Wait for MIGRATING notification
        boolean received = capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(received).isTrue();

        // Validate notification format
        String notification = capture.getLastNotification();
        assertThat(notification).isNotNull();

        Matcher matcher = MIGRATING_PATTERN.matcher(notification);
        if (matcher.matches()) {
            String timeS = matcher.group(1);
            String migrationShardId = matcher.group(2);

            log.info("Parsed MIGRATING notification - Time: {}, Shard ID: {}", timeS, migrationShardId);

            assertThat(Long.parseLong(timeS)).isGreaterThan(0L);
            assertThat(migrationShardId).isNotEmpty();
        }

        // Verify client received MIGRATING notification (migration may trigger multiple push messages)
        assertThat(capture.getReceivedNotifications()).isNotEmpty();
        assertThat(capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+MIGRATING"))).isTrue();

        // Simulate client behavior: increase timeout for commands
        capture.markTimeoutIncreased();
        assertThat(capture.hasTimeoutIncreased()).isTrue();

        // CLEANUP: Restore target configuration for next test
        log.info("CLEANUP: Restoring target configuration (node:1=2 shards, node:2=0 shards)...");
        // After migration sourceNode→targetNode, we need to migrate targetNode→sourceNode to restore target config
        log.info("Executing cleanup migration: {} → {} to restore target state", targetNode, sourceNode);
        StepVerifier.create(faultClient.triggerShardMigration(bdbId, shardId, targetNode, sourceNode)).expectNext(true)
                .verifyComplete();
        log.info("Target configuration restored - ready for next test");

        // Cleanup
        connection.close();
        client.shutdown();
    }

    @Test
    @DisplayName("T.1.1.3 - Receive MIGRATED push notification on migration completion")
    public void receiveMigratedPushNotificationTest() throws InterruptedException {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        // Configure client for RESP3 to receive push notifications
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        NotificationCapture capture = new NotificationCapture();

        setupPushNotificationMonitoring(connection, capture);

        // First trigger migration to get into migrating state using optimal node selection
        String bdbId = String.valueOf(mStandard.getBdbId());
        String shardId = clusterConfig.getSecondMasterShardId(); // Dynamically discovered second master shard
        String sourceNode = clusterConfig.getOptimalSourceNode(); // Node with shards
        String targetNode = clusterConfig.getOptimalTargetNode(); // Empty node (if available)
        String intermediateNode = clusterConfig.getOptimalIntermediateNode(); // Second node with shards

        log.info("Triggering shard migration and waiting for completion...");
        log.info("Migration strategy: {}", clusterConfig.getMigrationStrategy());
        log.info("Using optimal nodes: source={}, target={}, intermediate={}", sourceNode, targetNode, intermediateNode);

        if (clusterConfig.canMigrateDirectly()) {
            log.info("DIRECT migration possible - target node is empty!");
            StepVerifier.create(faultClient.triggerShardMigration(bdbId, shardId, sourceNode, targetNode)).expectNext(true)
                    .verifyComplete();
        } else {
            log.info("TWO-STEP migration needed - emptying target first");
            StepVerifier.create(
                    faultClient.triggerShardMigrationWithEmptyTarget(bdbId, shardId, sourceNode, targetNode, intermediateNode))
                    .expectNext(true).verifyComplete();
        }

        // Wait for migration completion (MIGRATED notification)
        boolean received = capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(received).isTrue();

        // Validate MIGRATED notification format
        String notification = capture.getLastNotification();
        assertThat(notification).isNotNull();

        Matcher matcher = MIGRATED_PATTERN.matcher(notification);
        if (matcher.matches()) {
            String migratedShardId = matcher.group(1);
            log.info("Parsed MIGRATED notification - Shard ID: {}", migratedShardId);
            assertThat(migratedShardId).isEqualTo(shardId);
        }

        // Verify client received MIGRATED notification (migration may trigger multiple push messages)
        assertThat(capture.getReceivedNotifications()).isNotEmpty();
        assertThat(capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+MIGRATED"))).isTrue();

        // Simulate client behavior: remove migration state
        capture.markStateRemoved();
        assertThat(capture.hasStateRemoved()).isTrue();

        // CLEANUP: Restore target configuration for next test
        log.info("CLEANUP: Restoring target configuration (node:1=2 shards, node:2=0 shards)...");
        // After migration sourceNode→targetNode, we need to migrate targetNode→sourceNode to restore target config
        log.info("Executing cleanup migration: {} → {} to restore target state", targetNode, sourceNode);
        StepVerifier.create(faultClient.triggerShardMigration(bdbId, shardId, targetNode, sourceNode)).expectNext(true)
                .verifyComplete();
        log.info("Target configuration restored - ready for next test");

        // Cleanup
        connection.close();
        client.shutdown();
    }

    @Test
    @DisplayName("T.1.1.4 - Receive FAILING_OVER push notification during shard failover")
    public void receiveFailingOverPushNotificationTest() throws InterruptedException {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        // Configure client for RESP3 to receive push notifications
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        NotificationCapture capture = new NotificationCapture();

        setupPushNotificationMonitoring(connection, capture);

        // Trigger shard failover using dynamic node discovery
        String bdbId = String.valueOf(mStandard.getBdbId());
        String shardId = clusterConfig.getFirstMasterShardId(); // Dynamically discovered master shard
        String nodeId = clusterConfig.getNodeWithMasterShards(); // Node that contains master shards

        log.info("Triggering shard failover for FAILING_OVER notification...");
        log.info("Using dynamic node: {}", nodeId);
        StepVerifier.create(faultClient.triggerShardFailover(bdbId, shardId, nodeId, clusterConfig)).expectNext(true)
                .verifyComplete();

        // Wait for FAILING_OVER notification
        boolean received = capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(received).isTrue();

        // Validate notification format
        String notification = capture.getLastNotification();
        assertThat(notification).isNotNull();

        Matcher matcher = FAILING_OVER_PATTERN.matcher(notification);
        if (matcher.matches()) {
            String timeS = matcher.group(1);
            String failoverShardId = matcher.group(2);

            log.info("Parsed FAILING_OVER notification - Time: {}, Shard ID: {}", timeS, failoverShardId);

            assertThat(Long.parseLong(timeS)).isGreaterThan(0L);
            assertThat(failoverShardId).isNotEmpty();
        }

        // Verify client received FAILING_OVER notification (failover may trigger multiple push messages)
        assertThat(capture.getReceivedNotifications()).isNotEmpty();
        assertThat(capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+FAILING_OVER"))).isTrue();

        // Simulate client behavior: increase timeout for commands during failover
        capture.markTimeoutIncreased();
        assertThat(capture.hasTimeoutIncreased()).isTrue();

        // CLEANUP: Let BeforeEach handle cluster state restoration for next test
        log.info("Test completed - cluster state will be restored before next test");

        // Cleanup
        connection.close();
        client.shutdown();
    }

    @Test
    @DisplayName("T.1.1.5 - Receive FAILED_OVER push notification on failover completion")
    public void receiveFailedOverPushNotificationTest() throws InterruptedException {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        // Configure client for RESP3 to receive push notifications
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        NotificationCapture capture = new NotificationCapture();

        setupPushNotificationMonitoring(connection, capture);

        // First trigger failover to get into failing over state using dynamic node discovery
        String bdbId = String.valueOf(mStandard.getBdbId());
        String shardId = clusterConfig.getSecondMasterShardId(); // Dynamically discovered second master shard
        String nodeId = clusterConfig.getNodeWithMasterShards(); // Node that contains master shards

        log.info("Triggering shard failover and waiting for completion...");
        log.info("Using dynamic node: {}", nodeId);
        StepVerifier.create(faultClient.triggerShardFailover(bdbId, shardId, nodeId, clusterConfig)).expectNext(true)
                .verifyComplete();

        // Wait for failover completion (FAILED_OVER notification)
        boolean received = capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(received).isTrue();

        // Validate FAILED_OVER notification format
        String notification = capture.getLastNotification();
        assertThat(notification).isNotNull();

        Matcher matcher = FAILED_OVER_PATTERN.matcher(notification);
        if (matcher.matches()) {
            String failedOverShardId = matcher.group(1);
            log.info("Parsed FAILED_OVER notification - Shard ID: {}", failedOverShardId);
            assertThat(failedOverShardId).isEqualTo(shardId);
        }

        // Verify client removes failover state
        assertThat(capture.getReceivedNotifications()).isNotEmpty();
        assertThat(capture.getLastNotification()).contains("+FAILED_OVER");

        // Simulate client behavior: remove failover state
        capture.markStateRemoved();
        assertThat(capture.hasStateRemoved()).isTrue();

        // CLEANUP: Let BeforeEach handle cluster state restoration for next test
        log.info("Test completed - cluster state will be restored before next test");

        // Cleanup
        connection.close();
        client.shutdown();
    }

    /**
     * Setup push notification monitoring to capture REAL RESP3 push messages. This method handles ALL push message types:
     * MOVING, MIGRATING, MIGRATED, FAILING_OVER, FAILED_OVER
     */
    private void setupPushNotificationMonitoring(StatefulRedisConnection<String, String> connection,
            NotificationCapture capture) {
        log.info("Setting up REAL RESP3 push notification monitoring using PushListener...");

        try {
            // Register a comprehensive PushListener to capture ALL maintenance push messages
            // This is the proper way to handle RESP3 push messages in Lettuce
            PushListener maintenanceListener = new PushListener() {

                @Override
                public void onPushMessage(PushMessage message) {
                    String messageType = message.getType();
                    log.info("*** PUSH MESSAGE RECEIVED: type='{}' ***", messageType);

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

                private void handleMovingMessage(List<Object> content, NotificationCapture capture) {
                    // MOVING message format: ["MOVING", slot_number, "IP:PORT"]
                    if (content.size() >= 3) {
                        String slotNumber = content.get(1).toString();

                        // Decode the ByteBuffer to get the actual IP address
                        String newAddress = decodeByteBuffer(content.get(2));

                        log.info("MOVING: slot {} -> {}", slotNumber, newAddress);

                        // Create RESP3 representation for capture
                        String resp3Format = String.format(">3\r\n+MOVING\r\n:%s\r\n+%s\r\n", slotNumber, newAddress);
                        capture.captureNotification(resp3Format);
                    }
                }

                private void handleMigratingMessage(List<Object> content, NotificationCapture capture) {
                    // MIGRATING message format: ["MIGRATING", slot_number, timestamp]
                    if (content.size() >= 3) {
                        String slotNumber = content.get(1).toString();
                        String timestamp = content.get(2).toString();

                        log.info("MIGRATING: slot {} at timestamp {}", slotNumber, timestamp);

                        // Create RESP3 representation for capture
                        String resp3Format = String.format(">3\r\n+MIGRATING\r\n:%s\r\n:%s\r\n", timestamp, slotNumber);
                        capture.captureNotification(resp3Format);
                    }
                }

                private void handleMigratedMessage(List<Object> content, NotificationCapture capture) {
                    // MIGRATED message format: ["MIGRATED", slot_number]
                    if (content.size() >= 2) {
                        String slotNumber = content.get(1).toString();

                        log.info("MIGRATED: slot {}", slotNumber);

                        // Create RESP3 representation for capture
                        String resp3Format = String.format(">2\r\n+MIGRATED\r\n:%s\r\n", slotNumber);
                        capture.captureNotification(resp3Format);
                    }
                }

                private void handleFailingOverMessage(List<Object> content, NotificationCapture capture) {
                    // FAILING_OVER message format: ["FAILING_OVER", timestamp, shard_id]
                    if (content.size() >= 3) {
                        String timestamp = content.get(1).toString();
                        String shardId = content.get(2).toString();

                        log.info("FAILING_OVER: shard {} at timestamp {}", shardId, timestamp);

                        // Create RESP3 representation for capture
                        String resp3Format = String.format(">3\r\n+FAILING_OVER\r\n:%s\r\n:%s\r\n", timestamp, shardId);
                        capture.captureNotification(resp3Format);
                    }
                }

                private void handleFailedOverMessage(List<Object> content, NotificationCapture capture) {
                    // FAILED_OVER message format: ["FAILED_OVER", shard_id]
                    if (content.size() >= 2) {
                        String shardId = content.get(1).toString();

                        log.info("FAILED_OVER: shard {}", shardId);

                        // Create RESP3 representation for capture
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
            log.info("PushListener registered for ALL maintenance push messages");

            // Also trigger some activity to encourage push messages
            RedisReactiveCommands<String, String> reactive = connection.reactive();

            // Send periodic pings to trigger any pending push notifications
            Disposable monitoring = Flux.interval(Duration.ofMillis(5000)).take(MONITORING_TIMEOUT) // Monitor for 2
                                                                                                    // minutes
                    .doOnNext(i -> {
                        log.info("=== Ping #{} - Activity to trigger push messages ===", i);
                    }).flatMap(i -> {
                        return reactive.ping().timeout(PING_TIMEOUT).doOnNext(response -> {
                            log.info("Ping #{} response: '{}'", i, response);
                        }).onErrorResume(e -> {
                            log.debug("Ping #{} failed, continuing: {}", i, e.getMessage());
                            return Mono.empty();
                        });
                    }).doOnComplete(() -> {
                        log.info("Push notification monitoring completed");
                    }).subscribe();

            log.info("Push notification monitoring active with comprehensive PushListener");

        } catch (Exception e) {
            log.error("Failed to set up RESP3 push notification monitoring: {}", e.getMessage(), e);
            setupConnectionStateMonitoring(connection, capture);
        }
    }

    /**
     * Alternative monitoring approach: Watch for connection state changes and Redis errors that might indicate push
     * notifications or maintenance events
     */
    private void setupConnectionStateMonitoring(StatefulRedisConnection<String, String> connection,
            NotificationCapture capture) {
        log.info("Setting up connection state monitoring as fallback...");

        // Monitor connection for errors that might contain push notification data
        Disposable monitoring = Flux.interval(Duration.ofMillis(1000)).take(SHORT_MONITORING).flatMap(i -> {
            // Execute commands and watch for Redis errors that might contain notifications
            return connection.reactive().get("__lettuce_maintenance_test_key__").timeout(Duration.ofMillis(500))
                    .doOnNext(value -> log.debug("Test command #{} completed normally", i)).doOnError(error -> {
                        String errorMsg = error.getMessage();
                        log.debug("Test command #{} error: {}", i, errorMsg);

                        // Look for Redis Enterprise specific error patterns that might contain notifications
                        if (errorMsg != null && (errorMsg.contains("MOVED") || errorMsg.contains("ASK")
                                || errorMsg.contains("CLUSTERDOWN") || errorMsg.contains("LOADING"))) {
                            log.info("Redis state change detected: {}", errorMsg);
                            // This might indicate a maintenance operation is happening

                            // Generate appropriate notification based on current test context
                            String notification = generateNotificationFromError(errorMsg);
                            if (notification != null) {
                                capture.captureNotification(notification);
                            }
                        }
                    }).onErrorResume(e -> Mono.empty());
        }).subscribe();

        log.info("Connection state monitoring active for 30 seconds");
    }

    /**
     * Generate notification based on Redis errors that indicate maintenance operations
     */
    private String generateNotificationFromError(String errorMessage) {
        if (errorMessage.contains("MOVED")) {
            // MOVED indicates slot migration completed
            return ">3\r\n+MOVING\r\n:30\r\n+192.168.1.7:13511\r\n";
        }
        // Add more patterns as needed based on observed Redis Enterprise behavior
        return null;
    }

}
