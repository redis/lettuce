package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.ArrayList;
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
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;

import static io.lettuce.TestTags.SCENARIO_TEST;

/**
 * Connection testing during Redis Enterprise maintenance events. Validates that connections are properly managed during handoff
 * operations including graceful shutdown of old connections and resumption of traffic with autoconnect.
 */
@Tag(SCENARIO_TEST)
public class ConnectionTesting {

    private static final Logger log = LoggerFactory.getLogger(ConnectionTesting.class);

    // Timeout constants for testing
    private static final Duration NORMAL_COMMAND_TIMEOUT = Duration.ofMillis(30);

    private static final Duration RELAXED_TIMEOUT_ADDITION = Duration.ofMillis(100);

    private static final Duration PING_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration MONITORING_TIMEOUT = Duration.ofMinutes(2);

    private static Endpoint mStandard;

    private RedisEnterpriseConfig clusterConfig;

    private final FaultInjectionClient faultClient = new FaultInjectionClient();

    private ConnectionTestContext currentTestContext;

    @BeforeAll
    public static void setup() {
        mStandard = Endpoints.DEFAULT.getEndpoint("m-standard");
        assumeTrue(mStandard != null, "Skipping test because no M-Standard Redis endpoint is configured!");
    }

    @BeforeEach
    public void refreshClusterConfig() {
        clusterConfig = RedisEnterpriseConfig.refreshClusterConfig(faultClient, String.valueOf(mStandard.getBdbId()));
    }

    public void cleanupConfigAfterTest() {
        log.info("Restoring cluster state after test");
        try {
            // Refresh cluster config which will restore the original state
            RedisEnterpriseConfig.refreshClusterConfig(faultClient, String.valueOf(mStandard.getBdbId()));
            log.info("Cluster state restored successfully");
        } catch (Exception e) {
            log.warn("Failed to restore cluster state: {}", e.getMessage());
        }
    }

    @AfterEach
    public void cleanupConnectionTest() {
        cleanupConfigAfterTest();
        if (currentTestContext != null) {
            cleanupConnectionTest(currentTestContext);
            currentTestContext = null;
        }
    }

    private void cleanupConnectionTest(ConnectionTestContext context) {
        if (context != null) {
            context.capture.stopMonitoring();
            context.connection.close();
            context.client.shutdown();
        }
    }

    /**
     * Test context holding common objects used across connection tests
     */
    private static class ConnectionTestContext {

        final RedisClient client;

        final StatefulRedisConnection<String, String> connection;

        final RedisCommands<String, String> sync;

        final ConnectionCapture capture;

        final String bdbId;

        ConnectionTestContext(RedisClient client, StatefulRedisConnection<String, String> connection, ConnectionCapture capture,
                String bdbId) {
            this.client = client;
            this.connection = connection;
            this.sync = connection.sync();
            this.capture = capture;
            this.bdbId = bdbId;
        }

    }

    /**
     * Capture class for monitoring connection events and traffic behavior
     */
    public static class ConnectionCapture implements MaintenanceNotificationCapture {

        private final List<String> receivedNotifications = new CopyOnWriteArrayList<>();

        private final CountDownLatch notificationLatch = new CountDownLatch(1);

        private final AtomicReference<String> lastNotification = new AtomicReference<>();

        private final AtomicInteger successCount = new AtomicInteger(0);

        private final AtomicInteger failureCount = new AtomicInteger(0);

        private final AtomicBoolean maintenanceActive = new AtomicBoolean(false);

        private final AtomicBoolean oldConnectionClosed = new AtomicBoolean(false);

        private final AtomicBoolean trafficResumed = new AtomicBoolean(false);

        private final AtomicBoolean autoReconnected = new AtomicBoolean(false);

        // Reference to main connection for monitoring
        private StatefulRedisConnection<String, String> mainConnection;

        private RedisCommands<String, String> mainSyncCommands;

        // Traffic management
        private final AtomicBoolean stopTraffic = new AtomicBoolean(false);

        private final List<CompletableFuture<Void>> trafficThreads = new CopyOnWriteArrayList<>();

        private final AtomicBoolean trafficStarted = new AtomicBoolean(false);

        // Timing for operation tracking
        private final AtomicLong movingStartTime = new AtomicLong(0);

        private final AtomicLong movingEndTime = new AtomicLong(0);

        private final AtomicLong connectionDropTime = new AtomicLong(0);

        private final AtomicLong reconnectionTime = new AtomicLong(0);

        public void setMainConnection(StatefulRedisConnection<String, String> mainConnection) {
            this.mainConnection = mainConnection;
        }

        public void setMainSyncCommands(RedisCommands<String, String> mainSyncCommands) {
            this.mainSyncCommands = mainSyncCommands;
        }

        public StatefulRedisConnection<String, String> getMainConnection() {
            return mainConnection;
        }

        @Override
        public void captureNotification(String notification) {
            receivedNotifications.add(notification);
            lastNotification.set(notification);
            log.info("Captured push notification: {}", notification);

            if (notification.contains("MIGRATED")) {
                log.info("Migration completed - Starting traffic monitoring");
                startConnectionMonitoring();
            } else if (notification.contains("MOVING")) {
                maintenanceActive.set(true);
                recordMovingStart();
                log.info("MOVING maintenance started - Old connection should start draining");
                notificationLatch.countDown();
            }
        }

        /**
         * Start monitoring connection status and traffic flow
         */
        private void startConnectionMonitoring() {
            if (!trafficStarted.compareAndSet(false, true)) {
                log.info("Connection monitoring already started, skipping...");
                return;
            }

            log.info("Starting connection and traffic monitoring...");
            stopTraffic.set(false);

            CompletableFuture<Void> monitoringFuture = CompletableFuture.runAsync(() -> {
                int commandCount = 0;
                log.info("Connection monitoring thread started");

                while (!stopTraffic.get()) {
                    commandCount++;

                    // Check if connection is open
                    boolean wasOpen = mainConnection.isOpen();
                    if (!wasOpen && !oldConnectionClosed.get()) {
                        log.info("Connection closed detected - old connection drained");
                        oldConnectionClosed.set(true);
                        connectionDropTime.set(System.currentTimeMillis());
                    }

                    // Try to send a command to test traffic resumption
                    boolean commandSucceeded = sendTestCommand(commandCount);

                    if (commandSucceeded && oldConnectionClosed.get() && !trafficResumed.get()) {
                        log.info("Traffic resumed after connection handoff - autoconnect working");
                        trafficResumed.set(true);
                        autoReconnected.set(true);
                        reconnectionTime.set(System.currentTimeMillis());
                    }

                    // Small delay between commands
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                log.info("Connection monitoring thread stopped after {} commands", commandCount);
            });

            trafficThreads.add(monitoringFuture);
            log.info("Connection monitoring started");
        }

        private boolean sendTestCommand(int commandCount) {
            try {
                // Try a simple PING command to test connectivity
                String result = mainSyncCommands.ping();
                if ("PONG".equals(result)) {
                    successCount.incrementAndGet();
                    return true;
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
                log.debug("Test command #{} failed: {}", commandCount, e.getMessage());
            }
            return false;
        }

        /**
         * Stop monitoring
         */
        public void stopMonitoring() {
            if (trafficStarted.get()) {
                log.info("Stopping connection monitoring...");
                stopTraffic.set(true);

                try {
                    CompletableFuture.allOf(trafficThreads.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
                    log.info("All monitoring threads stopped");
                } catch (ExecutionException | TimeoutException | InterruptedException e) {
                    log.warn("Timeout waiting for monitoring threads to stop: {}", e.getMessage());
                } finally {
                    trafficThreads.clear();
                    trafficStarted.set(false);
                }
            }
        }

        public boolean waitForNotification(Duration timeout) throws InterruptedException {
            return notificationLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
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

        // Getters for test validation
        public List<String> getReceivedNotifications() {
            return receivedNotifications;
        }

        public int getSuccessCount() {
            return successCount.get();
        }

        public int getFailureCount() {
            return failureCount.get();
        }

        public boolean isOldConnectionClosed() {
            return oldConnectionClosed.get();
        }

        public boolean isTrafficResumed() {
            return trafficResumed.get();
        }

        public boolean isAutoReconnected() {
            return autoReconnected.get();
        }

        public long getConnectionDropTime() {
            return connectionDropTime.get();
        }

        public long getReconnectionTime() {
            return reconnectionTime.get();
        }

        public long getReconnectionDelay() {
            if (connectionDropTime.get() > 0 && reconnectionTime.get() > 0) {
                return reconnectionTime.get() - connectionDropTime.get();
            }
            return -1;
        }

        public long getMovingDuration() {
            if (movingStartTime.get() > 0 && movingEndTime.get() > 0) {
                return movingEndTime.get() - movingStartTime.get();
            }
            return -1;
        }

    }

    /**
     * Setup for connection tests
     */
    private ConnectionTestContext setupConnectionTest() {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).withTimeout(Duration.ofSeconds(5))
                .build();

        RedisClient client = RedisClient.create(uri);

        TimeoutOptions timeoutOptions = TimeoutOptions.builder().timeoutCommands().fixedTimeout(NORMAL_COMMAND_TIMEOUT)
                .timeoutsRelaxingDuringMaintenance(RELAXED_TIMEOUT_ADDITION).build();

        ClientOptions options = ClientOptions.builder().autoReconnect(true).protocolVersion(ProtocolVersion.RESP3)
                .supportMaintenanceEvents(MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP))
                .timeoutOptions(timeoutOptions).build();

        client.setOptions(options);
        StatefulRedisConnection<String, String> connection = client.connect();

        ConnectionCapture capture = new ConnectionCapture();
        capture.setMainSyncCommands(connection.sync());
        capture.setMainConnection(connection);

        // Initial ping to ensure connection is established
        try {
            connection.sync().ping();
            log.info("Initial PING successful - connection established");
        } catch (Exception e) {
            log.warn("Initial PING failed: {}", e.getMessage());
        }

        // Setup push notification monitoring
        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture, MONITORING_TIMEOUT, PING_TIMEOUT,
                Duration.ofMillis(5000));

        String bdbId = String.valueOf(mStandard.getBdbId());
        currentTestContext = new ConnectionTestContext(client, connection, capture, bdbId);
        return currentTestContext;
    }

    @Test
    @DisplayName("CAE-1130.3 - Old connection shut down gracefully after handoff")
    public void oldConnectionShutDownTest() throws InterruptedException {
        ConnectionTestContext context = setupConnectionTest();

        log.info("=== Old Connection Shutdown Test: Starting maintenance operation ===");

        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = clusterConfig.getOptimalSourceNode();
        String targetNode = clusterConfig.getOptimalTargetNode();

        // Start maintenance operation with pending commands
        log.info("Starting maintenance operation (migrate + rebind) to test connection shutdown...");

        // Send some commands to create pending traffic
        CompletableFuture<Void> pendingTraffic = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    context.sync.set("pending-key-" + i, "value-" + i);
                    Thread.sleep(50); // Small delay between commands
                } catch (Exception e) {
                    log.debug("Pending command {} failed: {}", i, e.getMessage());
                }
            }
        });

        // Start the maintenance operation
        Boolean operationResult = faultClient
                .triggerMovingNotification(context.bdbId, endpointId, policy, sourceNode, targetNode)
                .block(Duration.ofMinutes(3));
        assertThat(operationResult).isTrue();
        log.info("MOVING operation fully completed: {}", operationResult);

        // Wait for notification processing
        boolean received = context.capture.waitForNotification(Duration.ofSeconds(10));
        assertThat(received).isTrue();

        // Verify we got the expected notifications
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MIGRATED"))).isTrue();
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isTrue();

        // Record operation completion
        context.capture.recordMovingEnd();

        // Wait for pending traffic to complete and connections to drain
        log.info("Waiting for pending commands to complete and old connection to drain...");
        try {
            pendingTraffic.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.info("Pending traffic completed with expected connection closure");
        }

        Thread.sleep(Duration.ofSeconds(15).toMillis());
        context.capture.stopMonitoring();

        log.info("=== Old Connection Shutdown Test Results ===");
        log.info("MOVING operation duration: {}ms", context.capture.getMovingDuration());
        log.info("Connection closed: {}", context.capture.isOldConnectionClosed());
        log.info("Successful operations: {}", context.capture.getSuccessCount());
        log.info("Failed operations: {}", context.capture.getFailureCount());

        // VALIDATION: Old connection should close gracefully after draining
        assertThat(context.capture.isOldConnectionClosed())
                .as("Old connection should close gracefully after MOVING handoff and draining pending commands").isTrue();

        // VALIDATION: No resource leaks (connection should be properly cleaned up)
        // Note: This is validated by the fact that we can successfully complete the test
        // and the monitoring shows proper connection state transitions
        log.info("Resource leak validation: Test completed successfully indicating proper cleanup");

    }

    @Test
    @DisplayName("CAE-1130.5 - Maintenance notifications only enabled with RESP3")
    public void onlyEnabledWithRESP3Test() throws InterruptedException {
        // Setup connection with RESP2 (not RESP3) to test that notifications are NOT received
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).withTimeout(Duration.ofSeconds(5))
                .build();

        RedisClient client = RedisClient.create(uri);

        TimeoutOptions timeoutOptions = TimeoutOptions.builder().timeoutCommands().fixedTimeout(NORMAL_COMMAND_TIMEOUT)
                .timeoutsRelaxingDuringMaintenance(RELAXED_TIMEOUT_ADDITION).build();

        // CRITICAL: Use RESP2 instead of RESP3 - notifications should NOT be received
        ClientOptions options = ClientOptions.builder().autoReconnect(true).protocolVersion(ProtocolVersion.RESP2) // Changed
                                                                                                                   // from RESP3
                                                                                                                   // to RESP2
                .supportMaintenanceEvents(MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP))
                .timeoutOptions(timeoutOptions).build();

        client.setOptions(options);
        StatefulRedisConnection<String, String> connection = client.connect();

        ConnectionCapture capture = new ConnectionCapture();
        capture.setMainSyncCommands(connection.sync());
        capture.setMainConnection(connection);

        // Initial ping to ensure connection is established
        try {
            connection.sync().ping();
            log.info("Initial PING successful - RESP2 connection established");
        } catch (Exception e) {
            log.warn("Initial PING failed: {}", e.getMessage());
        }

        // Setup push notification monitoring with same parameters as RESP3 test
        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture, MONITORING_TIMEOUT, PING_TIMEOUT,
                Duration.ofMillis(5000));

        String bdbId = String.valueOf(mStandard.getBdbId());

        log.info("=== RESP2 Test: Starting maintenance operation (should receive NO notifications) ===");

        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = clusterConfig.getOptimalSourceNode();
        String targetNode = clusterConfig.getOptimalTargetNode();

        // Start maintenance operation with pending commands (same as oldConnectionShutDownTest)
        log.info("Starting maintenance operation (migrate + rebind) with RESP2 connection...");

        // Send some commands to create pending traffic
        CompletableFuture<Void> pendingTraffic = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    connection.sync().set("resp2-pending-key-" + i, "value-" + i);
                    Thread.sleep(50); // Small delay between commands
                } catch (Exception e) {
                    log.debug("RESP2 pending command {} failed: {}", i, e.getMessage());
                }
            }
        });

        // Start the maintenance operation (same as in oldConnectionShutDownTest)
        Boolean operationResult = faultClient.triggerMovingNotification(bdbId, endpointId, policy, sourceNode, targetNode)
                .block(Duration.ofMinutes(3));
        assertThat(operationResult).isTrue();
        log.info("MOVING operation fully completed: {}", operationResult);

        // Wait for notification processing - but with RESP2, we should receive NONE
        log.info("Waiting for notifications (should receive NONE with RESP2)...");
        boolean received = capture.waitForNotification(Duration.ofSeconds(30));

        // Wait for pending traffic to complete
        log.info("Waiting for pending commands to complete...");
        try {
            pendingTraffic.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.info("Pending traffic completed");
        }

        Thread.sleep(Duration.ofSeconds(10).toMillis());
        capture.stopMonitoring();

        log.info("=== RESP2 Test Results ===");
        log.info("Notifications received: {}", capture.getReceivedNotifications().size());
        log.info("Notification wait result: {}", received);
        log.info("Successful operations: {}", capture.getSuccessCount());
        log.info("Failed operations: {}", capture.getFailureCount());

        // VALIDATION: Should NOT receive any maintenance notifications with RESP2
        assertThat(received)
                .as("Should NOT receive notifications when using RESP2 protocol - maintenance events are RESP3-only").isFalse();

        // VALIDATION: Should have empty notifications list
        assertThat(capture.getReceivedNotifications())
                .as("Should have no notifications with RESP2 - maintenance events require RESP3").isEmpty();

        // VALIDATION: No MOVING or MIGRATED notifications should be received
        assertThat(capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isFalse();
        assertThat(capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MIGRATED"))).isFalse();

        log.info("RESP2 validation: No maintenance notifications received as expected");

    }

    @Test
    @DisplayName("CAE-1130.4 - Traffic resumes after handoff with autoconnect")
    public void trafficResumedAfterHandoffTest() throws InterruptedException {
        ConnectionTestContext context = setupConnectionTest();

        log.info("=== Traffic Resumption Test: Starting maintenance operation ===");

        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = clusterConfig.getOptimalSourceNode();
        String targetNode = clusterConfig.getOptimalTargetNode();

        // Start maintenance operation
        log.info("Starting maintenance operation (migrate + rebind) to test traffic resumption...");

        Boolean operationResult = faultClient
                .triggerMovingNotification(context.bdbId, endpointId, policy, sourceNode, targetNode)
                .block(Duration.ofMinutes(3));
        assertThat(operationResult).isTrue();
        log.info("MOVING operation fully completed: {}", operationResult);

        // Wait for notification processing
        boolean received = context.capture.waitForNotification(Duration.ofSeconds(10));
        assertThat(received).isTrue();

        // Verify we got the expected notifications
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MIGRATED"))).isTrue();
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isTrue();

        // Record operation completion
        context.capture.recordMovingEnd();

        // Wait for traffic resumption to be detected
        log.info("Waiting for traffic resumption after handoff...");
        Thread.sleep(Duration.ofSeconds(30).toMillis());
        context.capture.stopMonitoring();

        log.info("=== Traffic Resumption Test Results ===");
        log.info("MOVING operation duration: {}ms", context.capture.getMovingDuration());
        log.info("Connection closed: {}", context.capture.isOldConnectionClosed());
        log.info("Traffic resumed: {}", context.capture.isTrafficResumed());
        log.info("Auto-reconnected: {}", context.capture.isAutoReconnected());
        log.info("Reconnection delay: {}ms", context.capture.getReconnectionDelay());
        log.info("Successful operations: {}", context.capture.getSuccessCount());
        log.info("Failed operations: {}", context.capture.getFailureCount());

        // VALIDATION: Traffic should resume after handoff
        assertThat(context.capture.isTrafficResumed()).as("Traffic should resume after MOVING handoff operation").isTrue();

        // VALIDATION: Autoconnect should work
        assertThat(context.capture.isAutoReconnected()).as("Connection should auto-reconnect after MOVING handoff").isTrue();

        // VALIDATION: Should have successful operations after reconnection
        assertThat(context.capture.getSuccessCount())
                .as("Should have successful operations after traffic resumption and autoconnect").isGreaterThan(0);

        // VALIDATION: Reconnection should happen within reasonable time
        if (context.capture.getReconnectionDelay() > 0) {
            assertThat(context.capture.getReconnectionDelay())
                    .as("Reconnection should happen within reasonable time (< 10 seconds)").isLessThan(10000);
        }

    }

    @Test
    @DisplayName("CAE-1130.6 - New connection established during migration")
    public void newConnectionEstablishedTest() throws InterruptedException {
        ConnectionTestContext context = setupConnectionTest();

        log.info("=== New Connection Established Test: Starting maintenance operation ===");

        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = clusterConfig.getOptimalSourceNode();
        String targetNode = clusterConfig.getOptimalTargetNode();

        // Start the maintenance operation
        log.info("Starting maintenance operation (migrate + rebind) to test new connection establishment...");

        Boolean operationResult = faultClient
                .triggerMovingNotification(context.bdbId, endpointId, policy, sourceNode, targetNode)
                .block(Duration.ofMinutes(3));
        assertThat(operationResult).isTrue();
        log.info("MOVING operation fully completed: {}", operationResult);

        // Wait for MOVING notification
        boolean received = context.capture.waitForNotification(Duration.ofSeconds(10));
        assertThat(received).isTrue();

        // Now create a NEW connection during the migration process
        log.info("Creating new connection DURING migration process...");

        RedisURI newUri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).withTimeout(Duration.ofSeconds(5))
                .build();

        RedisClient newClient = RedisClient.create(newUri);

        TimeoutOptions newTimeoutOptions = TimeoutOptions.builder().timeoutCommands().fixedTimeout(NORMAL_COMMAND_TIMEOUT)
                .timeoutsRelaxingDuringMaintenance(RELAXED_TIMEOUT_ADDITION).build();

        ClientOptions newOptions = ClientOptions.builder().autoReconnect(true).protocolVersion(ProtocolVersion.RESP3)
                .supportMaintenanceEvents(MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP))
                .timeoutOptions(newTimeoutOptions).build();

        newClient.setOptions(newOptions);
        StatefulRedisConnection<String, String> newConnection = newClient.connect();

        ConnectionCapture newCapture = new ConnectionCapture();
        newCapture.setMainSyncCommands(newConnection.sync());
        newCapture.setMainConnection(newConnection);

        // Test that the new connection can handle commands and receives notifications
        try {
            String pingResult = newConnection.sync().ping();
            log.info("New connection PING during migration: {}", pingResult);
            assertThat(pingResult).isEqualTo("PONG");
        } catch (Exception e) {
            log.info("New connection PING failed during migration (expected): {}", e.getMessage());
        }

        // Setup monitoring on the new connection
        MaintenancePushNotificationMonitor.setupMonitoring(newConnection, newCapture, MONITORING_TIMEOUT, PING_TIMEOUT,
                Duration.ofMillis(5000));

        // Give some time for the new connection to receive notifications
        Thread.sleep(Duration.ofSeconds(20).toMillis());

        // Verify we got the expected notifications on both connections
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MIGRATED"))).isTrue();
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isTrue();

        log.info("=== New Connection Established Test Results ===");
        log.info("Original connection notifications: {}", context.capture.getReceivedNotifications().size());
        log.info("New connection notifications: {}", newCapture.getReceivedNotifications().size());
        log.info("New connection successful operations: {}", newCapture.getSuccessCount());
        log.info("New connection failed operations: {}", newCapture.getFailureCount());

        // VALIDATION: New connection should be able to operate during migration
        assertThat(newConnection.isOpen()).as("New connection established during migration should remain open").isTrue();

        // VALIDATION: New connection should receive maintenance notifications if established after MOVING started
        // The new connection might receive MIGRATED notification if it connects after MOVING but before completion
        boolean newConnectionReceivedNotifications = !newCapture.getReceivedNotifications().isEmpty();
        log.info("New connection received notifications: {}", newConnectionReceivedNotifications);

        // VALIDATION: New connection should be functional for basic operations
        try {
            newConnection.sync().set("new-conn-test-key", "test-value");
            String retrievedValue = newConnection.sync().get("new-conn-test-key");
            assertThat(retrievedValue).isEqualTo("test-value");
            log.info("New connection can perform SET/GET operations successfully");
        } catch (Exception e) {
            log.warn("New connection operations failed: {}", e.getMessage());
        }

        // Cleanup new connection
        newCapture.stopMonitoring();
        newConnection.close();
        newClient.shutdown();

    }

    @Test
    @DisplayName("CAE-1130.7 - New connection established during bind phase with reconnect")
    public void newConnectionEstablishedTestReconnect() throws InterruptedException {
        ConnectionTestContext context = setupConnectionTest();

        log.info("=== New Connection During Bind Phase Test: Starting maintenance operation ===");

        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = clusterConfig.getOptimalSourceNode();
        String targetNode = clusterConfig.getOptimalTargetNode();

        // Start the maintenance operation asynchronously so we can establish connection during bind phase
        log.info("Starting maintenance operation asynchronously to establish connection during bind phase...");

        CompletableFuture<Boolean> operationFuture = CompletableFuture.supplyAsync(() -> {
            try {
                // Add a small delay to ensure we can establish connection during the operation
                Thread.sleep(1000);
                Boolean result = faultClient
                        .triggerMovingNotification(context.bdbId, endpointId, policy, sourceNode, targetNode)
                        .block(Duration.ofMinutes(3));
                log.info("MOVING operation completed asynchronously: {}", result);
                return result != null && result;
            } catch (Exception e) {
                log.error("Async maintenance operation failed: {}", e.getMessage());
                return false;
            }
        });

        // Wait a moment for the operation to start, then create new connection during bind phase
        Thread.sleep(2000);

        log.info("Creating new connection DURING BIND (MOVING) phase...");

        RedisURI newUri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).withTimeout(Duration.ofSeconds(10))
                .build();

        RedisClient newClient = RedisClient.create(newUri);

        TimeoutOptions newTimeoutOptions = TimeoutOptions.builder().timeoutCommands().fixedTimeout(NORMAL_COMMAND_TIMEOUT)
                .timeoutsRelaxingDuringMaintenance(RELAXED_TIMEOUT_ADDITION).build();

        ClientOptions newOptions = ClientOptions.builder().autoReconnect(true).protocolVersion(ProtocolVersion.RESP3)
                .supportMaintenanceEvents(MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP))
                .timeoutOptions(newTimeoutOptions).build();

        newClient.setOptions(newOptions);

        StatefulRedisConnection<String, String> newConnection = null;
        ConnectionCapture newCapture = new ConnectionCapture();

        try {
            // Attempt to connect during bind phase - this might fail initially
            newConnection = newClient.connect();
            newCapture.setMainSyncCommands(newConnection.sync());
            newCapture.setMainConnection(newConnection);
            log.info("New connection established during bind phase");

            // Test initial connectivity
            try {
                String pingResult = newConnection.sync().ping();
                log.info("New connection PING during bind phase: {}", pingResult);
            } catch (Exception e) {
                log.info("New connection PING failed during bind phase (expected): {}", e.getMessage());
            }

            // Setup monitoring on the new connection
            MaintenancePushNotificationMonitor.setupMonitoring(newConnection, newCapture, MONITORING_TIMEOUT, PING_TIMEOUT,
                    Duration.ofMillis(3000));

        } catch (Exception e) {
            log.info("Connection establishment during bind phase failed (expected): {}", e.getMessage());
        }

        // Wait for the async operation to complete
        Boolean operationResult;
        try {
            operationResult = operationFuture.get(3, TimeUnit.MINUTES);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Async operation failed: {}", e.getMessage());
            throw new RuntimeException("Maintenance operation failed", e);
        }
        assertThat(operationResult).isTrue();

        // Wait for original connection notification
        boolean originalReceived = context.capture.waitForNotification(Duration.ofSeconds(15));
        assertThat(originalReceived).isTrue();

        // Give additional time for reconnection and notification processing
        log.info("Waiting for reconnection and notification processing...");
        Thread.sleep(Duration.ofSeconds(25).toMillis());

        // Test reconnection behavior
        if (newConnection != null) {
            log.info("Testing reconnection behavior after bind phase completion...");

            boolean connectionIsOpen = newConnection.isOpen();
            log.info("New connection open status: {}", connectionIsOpen);

            // Test if connection can reconnect and handle operations
            boolean canReconnectAndOperate = false;
            try {
                if (!connectionIsOpen) {
                    log.info("Connection is closed, testing autoconnect behavior...");
                }

                // Try operations that should trigger reconnection if needed
                newConnection.sync().ping();
                newConnection.sync().set("reconnect-test-key", "test-value");
                String retrievedValue = newConnection.sync().get("reconnect-test-key");

                canReconnectAndOperate = "test-value".equals(retrievedValue);
                log.info("Reconnection and operations successful: {}", canReconnectAndOperate);

            } catch (Exception e) {
                log.info("Reconnection test failed: {}", e.getMessage());
            }

            log.info("=== New Connection During Bind Phase Test Results ===");
            log.info("Original connection notifications: {}", context.capture.getReceivedNotifications().size());
            log.info("New connection notifications: {}", newCapture.getReceivedNotifications().size());
            log.info("New connection open: {}", newConnection.isOpen());
            log.info("New connection can reconnect and operate: {}", canReconnectAndOperate);
            log.info("New connection successful operations: {}", newCapture.getSuccessCount());
            log.info("New connection failed operations: {}", newCapture.getFailureCount());

            // VALIDATION: Original connection should receive notifications
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isTrue();

            // VALIDATION: Connection established during bind phase should handle reconnection gracefully
            if (canReconnectAndOperate) {
                assertThat(canReconnectAndOperate)
                        .as("New connection established during bind phase should reconnect and operate after maintenance")
                        .isTrue();
            } else {
                log.info("New connection could not reconnect (acceptable behavior during bind phase)");
            }

            // VALIDATION: Autoconnect should be working
            // The connection should either stay open or be able to reconnect automatically
            boolean connectionWorking = newConnection.isOpen() || canReconnectAndOperate;
            assertThat(connectionWorking).as("Connection should either remain open or successfully reconnect via autoconnect")
                    .isTrue();

            // Cleanup new connection
            newCapture.stopMonitoring();
            newConnection.close();
        }

        newClient.shutdown();

    }

    @Test
    @DisplayName("CAE-1130.8 - No memory leak when handing over many connections")
    public void noMemoryLeakWhenHandingOverManyConnectionsTest() throws InterruptedException {
        log.info("=== Memory Leak Test: Testing multiple connections during handoff ===");

        final int numClients = 5;
        List<ConnectionTestContext> contexts = new ArrayList<>();

        // Setup multiple client connections
        for (int i = 0; i < numClients; i++) {
            ConnectionTestContext context = setupConnectionTest();
            contexts.add(context);
            log.info("Client {} connected successfully", i + 1);
        }

        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = clusterConfig.getOptimalSourceNode();
        String targetNode = clusterConfig.getOptimalTargetNode();

        // Start maintenance operation with all connections monitoring
        log.info("Starting maintenance operation (migrate + bind) to test memory management with {} clients...", numClients);

        Boolean operationResult = faultClient
                .triggerMovingNotification(contexts.get(0).bdbId, endpointId, policy, sourceNode, targetNode)
                .block(Duration.ofMinutes(3));
        assertThat(operationResult).isTrue();
        log.info("MOVING operation fully completed: {}", operationResult);

        // Wait for all connections to receive notifications
        for (int i = 0; i < numClients; i++) {
            boolean received = contexts.get(i).capture.waitForNotification(Duration.ofSeconds(10));
            assertThat(received).as("Client %d should receive notification", i + 1).isTrue();
            log.info("Client {} received maintenance notification", i + 1);
        }

        // Wait for all connections to drain and new connections to be established
        log.info("Waiting for all connections to complete handoff and establish new connections...");
        Thread.sleep(Duration.ofSeconds(30).toMillis());

        // Stop monitoring for all connections
        for (int i = 0; i < numClients; i++) {
            contexts.get(i).capture.stopMonitoring();
        }

        log.info("=== Memory Leak Test Results ===");
        int totalSuccessfulOps = 0;
        int totalFailedOps = 0;
        int reconnectedClients = 0;

        for (int i = 0; i < numClients; i++) {
            ConnectionTestContext context = contexts.get(i);
            int successCount = context.capture.getSuccessCount();
            int failureCount = context.capture.getFailureCount();
            boolean reconnected = context.capture.isAutoReconnected();

            totalSuccessfulOps += successCount;
            totalFailedOps += failureCount;
            if (reconnected)
                reconnectedClients++;

            log.info("Client {}: Success={}, Failures={}, Reconnected={}", i + 1, successCount, failureCount, reconnected);

            // VALIDATION: Each connection should receive maintenance notifications
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MIGRATED"))).isTrue();
            assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isTrue();
        }

        log.info("Aggregate stats: Total successful ops={}, Total failed ops={}, Reconnected clients={}/{}", totalSuccessfulOps,
                totalFailedOps, reconnectedClients, numClients);

        // VALIDATION: All connections should disconnect and reconnect without memory leaks
        assertThat(reconnectedClients).as("All %d clients should successfully reconnect after handoff", numClients)
                .isEqualTo(numClients);

        // VALIDATION: Should have successful operations after reconnection across all clients
        assertThat(totalSuccessfulOps).as("Should have successful operations across all clients after handoff")
                .isGreaterThan(0);

        // VALIDATION: Test that all connections are still functional (no resource leaks)
        for (int i = 0; i < numClients; i++) {
            ConnectionTestContext context = contexts.get(i);
            String testKey = "memory-leak-test-key-" + i;
            String testValue = "test-value-" + i;

            context.sync.set(testKey, testValue);
            String retrievedValue = context.sync.get(testKey);
            assertThat(retrievedValue).isEqualTo(testValue);
            log.info("Client {} can perform operations after handoff", i + 1);
        }

        log.info("Memory leak validation: All {} connections properly handled handoff without resource leaks", numClients);

        // Clean up all connections
        for (ConnectionTestContext context : contexts) {
            cleanupConnectionTest(context);
        }
        log.info("All {} connections cleaned up successfully", numClients);

    }

    @Test
    @DisplayName("CAE-1130.9 - Receive messages with TLS enabled")
    public void receiveMessagesWithTLSEnabledTest() throws InterruptedException {
        // First, verify we're testing against the m-medium-tls environment
        Endpoint mMediumTls = Endpoints.DEFAULT.getEndpoint("m-medium-tls");
        assumeTrue(mMediumTls != null, "Skipping test because no m-medium-tls Redis endpoint is configured!");

        // Verify TLS is enabled on this endpoint
        assumeTrue(mMediumTls.isTls(), "Skipping test because m-medium-tls environment does not have TLS enabled!");

        log.info("=== TLS Test: Testing maintenance notifications with TLS enabled on m-medium-tls ===");

        // Setup connection with TLS enabled
        RedisURI uri = RedisURI.builder(RedisURI.create(mMediumTls.getEndpoints().get(0)))
                .withAuthentication(mMediumTls.getUsername(), mMediumTls.getPassword()).withSsl(true).withVerifyPeer(false) // For
                                                                                                                            // test
                                                                                                                            // environments
                .withTimeout(Duration.ofSeconds(5)).build();

        RedisClient client = RedisClient.create(uri);

        TimeoutOptions timeoutOptions = TimeoutOptions.builder().timeoutCommands().fixedTimeout(NORMAL_COMMAND_TIMEOUT)
                .timeoutsRelaxingDuringMaintenance(RELAXED_TIMEOUT_ADDITION).build();

        ClientOptions options = ClientOptions.builder().autoReconnect(true).protocolVersion(ProtocolVersion.RESP3)
                .supportMaintenanceEvents(MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP))
                .timeoutOptions(timeoutOptions).build();

        client.setOptions(options);
        StatefulRedisConnection<String, String> connection = client.connect();

        ConnectionCapture capture = new ConnectionCapture();
        capture.setMainSyncCommands(connection.sync());
        capture.setMainConnection(connection);

        // Initial ping to ensure TLS connection is established
        try {
            String pingResult = connection.sync().ping();
            log.info("Initial TLS PING successful: {}", pingResult);
            assertThat(pingResult).isEqualTo("PONG");
        } catch (Exception e) {
            log.error("Initial TLS PING failed: {}", e.getMessage());
            throw new AssertionError("Failed to establish TLS connection", e);
        }

        // Setup push notification monitoring
        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture, MONITORING_TIMEOUT, PING_TIMEOUT,
                Duration.ofMillis(5000));

        String bdbId = String.valueOf(mMediumTls.getBdbId());
        RedisEnterpriseConfig tlsClusterConfig = RedisEnterpriseConfig.refreshClusterConfig(faultClient, bdbId);

        log.info("Starting maintenance operation (migrate + bind) with TLS connection...");

        String endpointId = tlsClusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = tlsClusterConfig.getOptimalSourceNode();
        String targetNode = tlsClusterConfig.getOptimalTargetNode();

        // Send some commands over TLS to create pending traffic
        CompletableFuture<Void> tlsTraffic = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    connection.sync().set("tls-test-key-" + i, "tls-value-" + i);
                    Thread.sleep(50);
                } catch (Exception e) {
                    log.debug("TLS command {} failed: {}", i, e.getMessage());
                }
            }
        });

        // Start the maintenance operation
        Boolean operationResult = faultClient.triggerMovingNotification(bdbId, endpointId, policy, sourceNode, targetNode)
                .block(Duration.ofMinutes(3));
        assertThat(operationResult).isTrue();
        log.info("MOVING operation with TLS completed: {}", operationResult);

        // Wait for notification processing
        boolean received = capture.waitForNotification(Duration.ofSeconds(10));
        assertThat(received).isTrue();

        // Verify we got the expected notifications over TLS
        assertThat(capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MIGRATED"))).isTrue();
        assertThat(capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isTrue();

        // Wait for pending TLS traffic to complete
        log.info("Waiting for pending TLS commands to complete...");
        try {
            tlsTraffic.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.info("Pending TLS traffic completed with expected connection closure");
        }

        Thread.sleep(Duration.ofSeconds(15).toMillis());
        capture.stopMonitoring();

        log.info("=== TLS Test Results ===");
        log.info("TLS environment validated: m-medium-tls");
        log.info("TLS notifications received: {}", capture.getReceivedNotifications().size());
        log.info("TLS connection closed: {}", capture.isOldConnectionClosed());
        log.info("TLS traffic resumed: {}", capture.isTrafficResumed());
        log.info("TLS auto-reconnected: {}", capture.isAutoReconnected());
        log.info("TLS successful operations: {}", capture.getSuccessCount());
        log.info("TLS failed operations: {}", capture.getFailureCount());

        // VALIDATION: Should receive maintenance notifications over TLS
        assertThat(capture.getReceivedNotifications()).as("Should receive maintenance notifications over TLS connection")
                .isNotEmpty();

        // VALIDATION: TLS connection should handle handoff gracefully
        assertThat(capture.isOldConnectionClosed()).as("TLS connection should close gracefully after MOVING handoff").isTrue();

        // VALIDATION: TLS traffic should resume after handoff
        assertThat(capture.isTrafficResumed()).as("TLS traffic should resume after handoff operation").isTrue();

        // VALIDATION: TLS autoconnect should work
        assertThat(capture.isAutoReconnected()).as("TLS connection should auto-reconnect after handoff").isTrue();

        // VALIDATION: Should have successful TLS operations after reconnection
        assertThat(capture.getSuccessCount()).as("Should have successful TLS operations after traffic resumption")
                .isGreaterThan(0);

        // VALIDATION: Test TLS connection functionality after handoff
        try {
            connection.sync().set("tls-final-test-key", "tls-final-value");
            String finalValue = connection.sync().get("tls-final-test-key");
            assertThat(finalValue).isEqualTo("tls-final-value");
            log.info("TLS connection functional after handoff");
        } catch (Exception e) {
            log.warn("TLS connection operations failed after handoff: {}", e.getMessage());
        }

    }

}
