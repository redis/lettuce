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

import org.junit.jupiter.api.AfterEach;
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
        clusterConfig = RedisEnterpriseConfig.refreshClusterConfig(faultClient, String.valueOf(mStandard.getBdbId()));
    }

    @AfterEach
    public void cleanupAfterTest() {
        log.info("=== CLEANUP: Restoring cluster state after test ===");
        try {
            // Refresh cluster config which will restore the original state
            // This is the same method used in @BeforeEach but it will restore state for the next test
            RedisEnterpriseConfig.refreshClusterConfig(faultClient, String.valueOf(mStandard.getBdbId()));
            log.info("=== CLEANUP: Cluster state restored successfully ===");
        } catch (Exception e) {
            log.warn("=== CLEANUP: Failed to restore cluster state: {} ===", e.getMessage());
        }
    }

    /**
     * Test context holding common objects used across all notification tests
     */
    private static class NotificationTestContext {

        final RedisClient client;

        final StatefulRedisConnection<String, String> connection;

        final NotificationCapture capture;

        final String bdbId;

        NotificationTestContext(RedisClient client, StatefulRedisConnection<String, String> connection,
                NotificationCapture capture, String bdbId) {
            this.client = client;
            this.connection = connection;
            this.capture = capture;
            this.bdbId = bdbId;
        }

    }

    /**
     * Helper class to capture and validate push notifications
     */
    public static class NotificationCapture {

        private final List<String> receivedNotifications = new CopyOnWriteArrayList<>();

        private final CountDownLatch notificationLatch = new CountDownLatch(1);

        private final AtomicReference<String> lastNotification = new AtomicReference<>();

        private final AtomicBoolean testPhaseActive = new AtomicBoolean(true);

        public void captureNotification(String notification) {
            // Only capture notifications during the test phase, not during cleanup
            if (testPhaseActive.get()) {
                receivedNotifications.add(notification);
                lastNotification.set(notification);
                notificationLatch.countDown();
                log.info("Captured push notification: {}", notification);
            } else {
                log.debug("Ignoring notification during cleanup phase: {}", notification);
            }
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

        public void endTestPhase() {
            testPhaseActive.set(false);
            log.info("Test phase ended - notifications will be ignored during cleanup");
        }

    }

    /**
     * Common setup for all notification tests
     */
    private NotificationTestContext setupNotificationTest() {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        // Configure client for RESP3 to receive push notifications
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        NotificationCapture capture = new NotificationCapture();

        // Setup push notification monitoring
        setupPushNotificationMonitoring(connection, capture);

        String bdbId = String.valueOf(mStandard.getBdbId());

        return new NotificationTestContext(client, connection, capture, bdbId);
    }

    /**
     * Common cleanup for all notification tests
     */
    private void cleanupNotificationTest(NotificationTestContext context) {
        context.connection.close();
        context.client.shutdown();
    }

    @Test
    @DisplayName("T.1.1.1 - Receive MOVING push notification during endpoint rebind")
    public void receiveMovingPushNotificationTest() throws InterruptedException {
        log.info("=== STARTING TEST: T.1.1.1 - Receive MOVING push notification during endpoint rebind ===");
        NotificationTestContext context = setupNotificationTest();

        // Trigger MOVING notification using the proper two-step process:
        // 1. Migrate all shards from source node to target node (making it empty)
        // 2. Bind endpoint to trigger MOVING notification
        String endpointId = clusterConfig.getFirstEndpointId(); // Dynamically discovered endpoint ID
        String policy = "single"; // M-Standard uses single policy
        String sourceNode = clusterConfig.getOptimalSourceNode(); // Dynamically discovered source node (finds node with shards)
        String targetNode = clusterConfig.getOptimalTargetNode(); // Dynamically discovered target node (finds empty node)

        log.info("Triggering MOVING notification using proper two-step process...");
        log.info("Using dynamic nodes: source={}, target={}", sourceNode, targetNode);
        StepVerifier.create(faultClient.triggerMovingNotification(context.bdbId, endpointId, policy, sourceNode, targetNode))
                .expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);

        // Wait for MOVING notification
        boolean received = context.capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(received).isTrue();

        // Validate notification format and parsing
        String notification = context.capture.getLastNotification();
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
        assertThat(context.capture.getReceivedNotifications()).isNotEmpty();
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+MOVING"))).isTrue();

        // End test phase to prevent capturing cleanup notifications
        context.capture.endTestPhase();

        log.info("=== COMPLETED TEST: T.1.1.1 - Receive MOVING push notification during endpoint rebind ===");

        // Cleanup test resources
        cleanupNotificationTest(context);
    }

    @Test
    @DisplayName("T.1.1.2 - Receive MIGRATING push notification during node migration")
    public void receiveMigratingPushNotificationTest() throws InterruptedException {
        log.info("=== STARTING TEST: T.1.1.2 - Receive MIGRATING push notification during node migration ===");
        NotificationTestContext context = setupNotificationTest();

        // Trigger node migration using optimal node selection
        String shardId = clusterConfig.getFirstMasterShardId(); // Dynamically discovered master shard
        String sourceNode = clusterConfig.getOptimalSourceNode(); // Node with shards
        String targetNode = clusterConfig.getOptimalTargetNode(); // Empty node (if available)
        String intermediateNode = clusterConfig.getOptimalIntermediateNode(); // Second node with shards

        log.info("Triggering shard migration for MIGRATING notification...");
        log.info("Migration strategy: {}", clusterConfig.getMigrationStrategy());
        log.info("Using optimal nodes: source={}, target={}, intermediate={}", sourceNode, targetNode, intermediateNode);

        if (clusterConfig.canMigrateDirectly()) {
            log.info("DIRECT migration possible - target node is empty!");
            StepVerifier.create(faultClient.triggerShardMigration(context.bdbId, shardId, sourceNode, targetNode))
                    .expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);
        } else {
            log.info("TWO-STEP migration needed - emptying target first");
            StepVerifier.create(faultClient.triggerShardMigrationWithEmptyTarget(context.bdbId, shardId, sourceNode, targetNode,
                    intermediateNode)).expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);
        }

        // Wait for MIGRATING notification
        boolean received = context.capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(received).isTrue();

        // Validate notification format
        String notification = context.capture.getLastNotification();
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
        assertThat(context.capture.getReceivedNotifications()).isNotEmpty();
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+MIGRATING"))).isTrue();

        // End test phase to prevent capturing cleanup notifications
        context.capture.endTestPhase();

        log.info("=== COMPLETED TEST: T.1.1.2 - Receive MIGRATING push notification during node migration ===");

        // Cleanup test resources
        cleanupNotificationTest(context);
    }

    @Test
    @DisplayName("T.1.1.3 - Receive MIGRATED push notification on migration completion")
    public void receiveMigratedPushNotificationTest() throws InterruptedException {
        log.info("=== STARTING TEST: T.1.1.3 - Receive MIGRATED push notification on migration completion ===");
        NotificationTestContext context = setupNotificationTest();

        // First trigger migration to get into migrating state using optimal node selection
        String shardId = clusterConfig.getSecondMasterShardId(); // Dynamically discovered second master shard
        String sourceNode = clusterConfig.getOptimalSourceNode(); // Node with shards
        String targetNode = clusterConfig.getOptimalTargetNode(); // Empty node (if available)
        String intermediateNode = clusterConfig.getOptimalIntermediateNode(); // Second node with shards

        log.info("Triggering shard migration and waiting for completion...");
        log.info("Migration strategy: {}", clusterConfig.getMigrationStrategy());
        log.info("Using optimal nodes: source={}, target={}, intermediate={}", sourceNode, targetNode, intermediateNode);

        if (clusterConfig.canMigrateDirectly()) {
            log.info("DIRECT migration possible - target node is empty!");
            StepVerifier.create(faultClient.triggerShardMigration(context.bdbId, shardId, sourceNode, targetNode))
                    .expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);
        } else {
            log.info("TWO-STEP migration needed - emptying target first");
            StepVerifier.create(faultClient.triggerShardMigrationWithEmptyTarget(context.bdbId, shardId, sourceNode, targetNode,
                    intermediateNode)).expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);
        }

        // Wait for migration completion (MIGRATED notification)
        boolean received = context.capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(received).isTrue();

        // Validate MIGRATED notification format
        String notification = context.capture.getLastNotification();
        assertThat(notification).isNotNull();

        Matcher matcher = MIGRATED_PATTERN.matcher(notification);
        if (matcher.matches()) {
            String migratedShardId = matcher.group(1);
            log.info("Parsed MIGRATED notification - Shard ID: {}", migratedShardId);
            assertThat(migratedShardId).isEqualTo(shardId);
        }

        // Verify client received MIGRATED notification (migration may trigger multiple push messages)
        assertThat(context.capture.getReceivedNotifications()).isNotEmpty();
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+MIGRATED"))).isTrue();

        // End test phase to prevent capturing cleanup notifications
        context.capture.endTestPhase();

        log.info("=== COMPLETED TEST: T.1.1.3 - Receive MIGRATED push notification on migration completion ===");

        // Cleanup test resources
        cleanupNotificationTest(context);
    }

    @Test
    @DisplayName("T.1.1.4 - Receive FAILING_OVER push notification during shard failover")
    public void receiveFailingOverPushNotificationTest() throws InterruptedException {
        log.info("=== STARTING TEST: T.1.1.4 - Receive FAILING_OVER push notification during shard failover ===");
        NotificationTestContext context = setupNotificationTest();

        // Trigger shard failover using dynamic node discovery
        String shardId = clusterConfig.getFirstMasterShardId(); // Dynamically discovered master shard
        String nodeId = clusterConfig.getNodeWithMasterShards(); // Node that contains master shards

        log.info("Triggering shard failover for FAILING_OVER notification...");
        log.info("Using dynamic node: {}", nodeId);
        StepVerifier.create(faultClient.triggerShardFailover(context.bdbId, shardId, nodeId, clusterConfig)).expectNext(true)
                .expectComplete().verify(LONG_OPERATION_TIMEOUT);

        // Wait for FAILING_OVER notification
        boolean received = context.capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(received).isTrue();

        // Validate notification format
        String notification = context.capture.getLastNotification();
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
        assertThat(context.capture.getReceivedNotifications()).isNotEmpty();
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("+FAILING_OVER"))).isTrue();

        // End test phase to prevent capturing cleanup notifications
        context.capture.endTestPhase();
        log.info("=== COMPLETED TEST: T.1.1.4 - Receive FAILING_OVER push notification during shard failover ===");

        // Cleanup test resources
        cleanupNotificationTest(context);
    }

    @Test
    @DisplayName("T.1.1.5 - Receive FAILED_OVER push notification on failover completion")
    public void receiveFailedOverPushNotificationTest() throws InterruptedException {
        log.info("=== STARTING TEST: T.1.1.5 - Receive FAILED_OVER push notification on failover completion ===");
        NotificationTestContext context = setupNotificationTest();

        // First trigger failover to get into failing over state using dynamic node discovery
        String shardId = clusterConfig.getSecondMasterShardId(); // Dynamically discovered second master shard
        String nodeId = clusterConfig.getNodeWithMasterShards(); // Node that contains master shards

        log.info("Triggering shard failover and waiting for completion...");
        log.info("Using dynamic node: {}", nodeId);
        StepVerifier.create(faultClient.triggerShardFailover(context.bdbId, shardId, nodeId, clusterConfig)).expectNext(true)
                .expectComplete().verify(LONG_OPERATION_TIMEOUT);

        // Wait for failover completion (FAILED_OVER notification)
        boolean received = context.capture.waitForNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(received).isTrue();

        // Validate FAILED_OVER notification format
        String notification = context.capture.getLastNotification();
        assertThat(notification).isNotNull();

        Matcher matcher = FAILED_OVER_PATTERN.matcher(notification);
        if (matcher.matches()) {
            String failedOverShardId = matcher.group(1);
            log.info("Parsed FAILED_OVER notification - Shard ID: {}", failedOverShardId);
            assertThat(failedOverShardId).isEqualTo(shardId);
        }

        // Verify client removes failover state
        assertThat(context.capture.getReceivedNotifications()).isNotEmpty();
        assertThat(context.capture.getLastNotification()).contains("+FAILED_OVER");

        // End test phase to prevent capturing cleanup notifications
        context.capture.endTestPhase();

        log.info("=== COMPLETED TEST: T.1.1.5 - Receive FAILED_OVER push notification on failover completion ===");

        // Cleanup test resources
        cleanupNotificationTest(context);
    }

    /**
     * Setup push notification monitoring to capture REAL RESP3 push messages. This method handles ALL push message types:
     * MOVING, MIGRATING, MIGRATED, FAILING_OVER, FAILED_OVER
     */
    private void setupPushNotificationMonitoring(StatefulRedisConnection<String, String> connection,
            NotificationCapture capture) {
        log.info("Setting up REAL RESP3 push notification monitoring using PushListener...");

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
    }

}
