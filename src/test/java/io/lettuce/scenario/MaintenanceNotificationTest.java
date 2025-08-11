package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.List;
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
import io.lettuce.core.MaintenanceEventsOptions;
import io.lettuce.core.MaintenanceEventsOptions.AddressType;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;

import reactor.test.StepVerifier;

import static io.lettuce.TestTags.SCENARIO_TEST;

/**
 * CAE-633: Tests for Redis Enterprise maintenance push notifications. Validates client reception and processing of different
 * types of push notifications during maintenance operations like migration, failover, and endpoint rebinding.
 */
@Tag(SCENARIO_TEST)
public class MaintenanceNotificationTest {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceNotificationTest.class);

    // 180 seconds - for waiting for notifications
    private static final Duration NOTIFICATION_WAIT_TIMEOUT = Duration.ofMinutes(3);

    // 300 seconds - for migrations/failovers
    private static final Duration LONG_OPERATION_TIMEOUT = Duration.ofMinutes(5);

    // 120 seconds - for monitoring operations
    private static final Duration MONITORING_TIMEOUT = Duration.ofMinutes(2);

    // 10 seconds - for ping operations
    private static final Duration PING_TIMEOUT = Duration.ofSeconds(10);

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
        log.info("Restoring cluster state after test");
        try {
            // Refresh cluster config which will restore the original state
            // This is the same method used in @BeforeEach but it will restore state for the next test
            RedisEnterpriseConfig.refreshClusterConfig(faultClient, String.valueOf(mStandard.getBdbId()));
            log.info("Cluster state restored successfully");
        } catch (Exception e) {
            log.warn("Failed to restore cluster state: {}", e.getMessage());
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
    public static class NotificationCapture implements MaintenanceNotificationCapture {

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
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .supportMaintenanceEvents(MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP)).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        NotificationCapture capture = new NotificationCapture();

        // Setup push notification monitoring using the utility
        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture, MONITORING_TIMEOUT, PING_TIMEOUT,
                Duration.ofMillis(5000));

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
        log.info("Starting test: T.1.1.1 - Receive MOVING push notification during endpoint rebind");
        NotificationTestContext context = setupNotificationTest();

        // Trigger MOVING notification using the proper two-step process:
        // 1. Migrate all shards from source node to target node (making it empty)
        // 2. Bind endpoint to trigger MOVING notification
        // Dynamically discovered endpoint ID
        String endpointId = clusterConfig.getFirstEndpointId();
        // M-Standard uses single policy
        String policy = "single";
        // Dynamically discovered source node (finds node with shards)
        String sourceNode = clusterConfig.getOptimalSourceNode();
        // Dynamically discovered target node (finds empty node)
        String targetNode = clusterConfig.getOptimalTargetNode();

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

        log.info("Completed test: T.1.1.1 - Receive MOVING push notification during endpoint rebind");

        // Cleanup test resources
        cleanupNotificationTest(context);
    }

    @Test
    @DisplayName("T.1.1.2 - Receive MIGRATING push notification during node migration")
    public void receiveMigratingPushNotificationTest() throws InterruptedException {
        log.info("Starting test: T.1.1.2 - Receive MIGRATING push notification during node migration");
        NotificationTestContext context = setupNotificationTest();

        // Trigger node migration using optimal node selection
        // Dynamically discovered master shard
        String shardId = clusterConfig.getFirstMasterShardId();
        // Node with shards
        String sourceNode = clusterConfig.getOptimalSourceNode();
        // Empty node (if available)
        String targetNode = clusterConfig.getOptimalTargetNode();
        // Second node with shards
        String intermediateNode = clusterConfig.getOptimalIntermediateNode();

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

        log.info("Completed test: T.1.1.2 - Receive MIGRATING push notification during node migration");

        // Cleanup test resources
        cleanupNotificationTest(context);
    }

    @Test
    @DisplayName("T.1.1.3 - Receive MIGRATED push notification on migration completion")
    public void receiveMigratedPushNotificationTest() throws InterruptedException {
        log.info("Starting test: T.1.1.3 - Receive MIGRATED push notification on migration completion");
        NotificationTestContext context = setupNotificationTest();

        // First trigger migration to get into migrating state using optimal node selection
        // Dynamically discovered second master shard
        String shardId = clusterConfig.getSecondMasterShardId();
        // Node with shards
        String sourceNode = clusterConfig.getOptimalSourceNode();
        // Empty node (if available)
        String targetNode = clusterConfig.getOptimalTargetNode();
        // Second node with shards
        String intermediateNode = clusterConfig.getOptimalIntermediateNode();

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

        log.info("Completed test: T.1.1.3 - Receive MIGRATED push notification on migration completion");

        // Cleanup test resources
        cleanupNotificationTest(context);
    }

    @Test
    @DisplayName("T.1.1.4 - Receive FAILING_OVER push notification during shard failover")
    public void receiveFailingOverPushNotificationTest() throws InterruptedException {
        log.info("Starting test: T.1.1.4 - Receive FAILING_OVER push notification during shard failover");
        NotificationTestContext context = setupNotificationTest();

        // Trigger shard failover using dynamic node discovery
        // Dynamically discovered master shard
        String shardId = clusterConfig.getFirstMasterShardId();
        // Node that contains master shards
        String nodeId = clusterConfig.getNodeWithMasterShards();

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
        log.info("Completed test: T.1.1.4 - Receive FAILING_OVER push notification during shard failover");

        // Cleanup test resources
        cleanupNotificationTest(context);
    }

    @Test
    @DisplayName("T.1.1.5 - Receive FAILED_OVER push notification on failover completion")
    public void receiveFailedOverPushNotificationTest() throws InterruptedException {
        log.info("Starting test: T.1.1.5 - Receive FAILED_OVER push notification on failover completion");
        NotificationTestContext context = setupNotificationTest();

        // First trigger failover to get into failing over state using dynamic node discovery
        // Dynamically discovered second master shard
        String shardId = clusterConfig.getSecondMasterShardId();
        // Node that contains master shards
        String nodeId = clusterConfig.getNodeWithMasterShards();

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

        log.info("Completed test: T.1.1.5 - Receive FAILED_OVER push notification on failover completion");

        // Cleanup test resources
        cleanupNotificationTest(context);
    }

}
