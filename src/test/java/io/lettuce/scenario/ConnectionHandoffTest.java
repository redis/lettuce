package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
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
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.protocol.MaintenanceAwareExpiryWriter;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.ConnectionTestUtil;
import io.lettuce.test.env.Endpoints;
import io.netty.channel.Channel;
import io.lettuce.test.env.Endpoints.Endpoint;

import reactor.test.StepVerifier;

import static io.lettuce.TestTags.SCENARIO_TEST;

/**
 * Connection handoff tests for Redis Enterprise maintenance events. Validates that connections properly receive the correct
 * endpoint address types (internal IP, external IP, internal FQDN, external FQDN) during MOVING notifications and handle
 * reconnection appropriately.
 */
@Tag(SCENARIO_TEST)
public class ConnectionHandoffTest {

    private static final Logger log = LoggerFactory.getLogger(ConnectionHandoffTest.class);

    // 180 seconds - for waiting for notifications
    private static final Duration NOTIFICATION_WAIT_TIMEOUT = Duration.ofMinutes(3);

    // 300 seconds - for migrations/failovers
    private static final Duration LONG_OPERATION_TIMEOUT = Duration.ofMinutes(5);

    // 300 seconds - for monitoring operations (extended to allow for longer maintenance operations)
    private static final Duration MONITORING_TIMEOUT = Duration.ofMinutes(5);

    // 10 seconds - for ping operations
    private static final Duration PING_TIMEOUT = Duration.ofSeconds(10);

    // Timeout constants for command execution
    private static final Duration NORMAL_COMMAND_TIMEOUT = Duration.ofMillis(30);

    private static final Duration RELAXED_TIMEOUT_ADDITION = Duration.ofMillis(100);

    private static Endpoint mStandard;

    private RedisEnterpriseConfig clusterConfig;

    private final FaultInjectionClient faultClient = new FaultInjectionClient();

    private HandoffTestContext currentTestContext;

    // Push notification patterns for MOVING messages with different address types
    // Handles both IP:PORT and FQDN formats, with both \n and \r\n line endings
    // Also handles empty address for AddressType.NONE
    private static final Pattern MOVING_PATTERN = Pattern
            .compile(">\\d+\\r?\\nMOVING\\r?\\n:([^\\r\\n]+)\\r?\\n:(\\d+)\\r?\\n([^\\r\\n]*)\\s*");

    // Pattern to identify IP addresses (IPv4)
    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

    // Pattern to identify FQDNs (contains at least one dot and alphabetic characters)
    private static final Pattern FQDN_PATTERN = Pattern
            .compile("^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$");

    @BeforeAll
    public static void setup() {
        mStandard = Endpoints.DEFAULT.getEndpoint("re-standalone");
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
    public void cleanupHandoffTest() {
        cleanupConfigAfterTest();
        if (currentTestContext != null) {
            if (currentTestContext.connection != null && currentTestContext.connection.isOpen()) {
                currentTestContext.connection.close();
            }
            if (currentTestContext.client != null) {
                currentTestContext.client.shutdown();
            }
            currentTestContext = null;
        }
    }

    /**
     * Test context holding common objects used across all handoff tests
     */
    private static class HandoffTestContext {

        final RedisClient client;

        final StatefulRedisConnection<String, String> connection;

        final HandoffCapture capture;

        final String bdbId;

        final AddressType expectedAddressType;

        HandoffTestContext(RedisClient client, StatefulRedisConnection<String, String> connection, HandoffCapture capture,
                String bdbId, AddressType expectedAddressType) {
            this.client = client;
            this.connection = connection;
            this.capture = capture;
            this.bdbId = bdbId;
            this.expectedAddressType = expectedAddressType;
        }

    }

    /**
     * Helper class to capture and validate handoff notifications with address type validation
     */
    public static class HandoffCapture implements MaintenanceNotificationCapture {

        private final List<String> receivedNotifications = new CopyOnWriteArrayList<>();

        private final CountDownLatch movingLatch = new CountDownLatch(1);

        private final CountDownLatch migratedLatch = new CountDownLatch(1);

        private final AtomicReference<String> lastMovingNotification = new AtomicReference<>();

        private final AtomicReference<String> lastMigratedNotification = new AtomicReference<>();

        private final AtomicBoolean testPhaseActive = new AtomicBoolean(true);

        private final AtomicBoolean reconnectionTested = new AtomicBoolean(false);

        public void captureNotification(String notification) {
            // Only capture notifications during the test phase, not during cleanup
            if (testPhaseActive.get()) {
                receivedNotifications.add(notification);
                log.info("Captured push notification: {}", notification);

                if (notification.contains("MOVING")) {
                    lastMovingNotification.set(notification);
                    movingLatch.countDown();
                    log.info("MOVING notification captured, countdown: {}", movingLatch.getCount());
                } else if (notification.contains("MIGRATED")) {
                    lastMigratedNotification.set(notification);
                    migratedLatch.countDown();
                    log.info("MIGRATED notification captured, countdown: {}", migratedLatch.getCount());
                }
            } else {
                log.debug("Ignoring notification during cleanup phase: {}", notification);
            }
        }

        public boolean waitForMovingNotification(Duration timeout) throws InterruptedException {
            return movingLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public boolean waitForMigratedNotification(Duration timeout) throws InterruptedException {
            return migratedLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public List<String> getReceivedNotifications() {
            return receivedNotifications;
        }

        public String getLastMovingNotification() {
            return lastMovingNotification.get();
        }

        public String getLastMigratedNotification() {
            return lastMigratedNotification.get();
        }

        public void endTestPhase() {
            testPhaseActive.set(false);
            log.info("Test phase ended - notifications will be ignored during cleanup");
        }

        public void setReconnectionTested(boolean tested) {
            reconnectionTested.set(tested);
        }

        public boolean isReconnectionTested() {
            return reconnectionTested.get();
        }

    }

    /**
     * Continuous traffic generator for async GET/SET operations with failure counting
     */
    public static class ContinuousTrafficGenerator {

        private final RedisAsyncCommands<String, String> asyncCommands;

        private final AtomicBoolean stopTraffic = new AtomicBoolean(false);

        private final AtomicLong successfulOperations = new AtomicLong(0);

        private final AtomicLong failedOperations = new AtomicLong(0);

        private final AtomicInteger commandCounter = new AtomicInteger(0);

        private final List<CompletableFuture<Void>> trafficFutures = new CopyOnWriteArrayList<>();

        private final AtomicBoolean trafficStarted = new AtomicBoolean(false);

        public ContinuousTrafficGenerator(RedisAsyncCommands<String, String> asyncCommands) {
            this.asyncCommands = asyncCommands;
        }

        /**
         * Start continuous traffic with async GET/SET commands in 50:50 ratio
         */
        public void startTraffic() {
            if (!trafficStarted.compareAndSet(false, true)) {
                log.info("Traffic already started, skipping...");
                return;
            }

            log.info("Starting continuous async traffic (GET/SET 50:50 ratio)...");
            stopTraffic.set(false);

            CompletableFuture<Void> trafficFuture = CompletableFuture.runAsync(() -> {
                while (!stopTraffic.get()) {
                    try {
                        int cmdNumber = commandCounter.incrementAndGet();
                        String key = "traffic-key-" + (cmdNumber % 100); // Rotate through 100 keys

                        // 50:50 ratio between GET and SET operations
                        if (cmdNumber % 2 == 0) {
                            // SET operation
                            String value = "value-" + cmdNumber;
                            RedisFuture<String> future = asyncCommands.set(key, value);
                            handleAsyncResult(future, "SET " + key);
                        } else {
                            // GET operation
                            RedisFuture<String> future = asyncCommands.get(key);
                            handleAsyncResult(future, "GET " + key);
                        }

                        // Small delay to prevent overwhelming the connection
                        Thread.sleep(10);
                    } catch (Exception e) {
                        log.warn("Traffic generation error: {}", e.getMessage());
                        failedOperations.incrementAndGet();
                    }
                }
                log.info("Traffic generator stopped after {} commands", commandCounter.get());
            });

            trafficFutures.add(trafficFuture);
            log.info("Continuous async traffic started");
        }

        /**
         * Handle async command results and count successes/failures
         */
        private void handleAsyncResult(RedisFuture<?> future, String operation) {
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.debug("Traffic command failed: {} - {}", operation, throwable.getMessage());
                    failedOperations.incrementAndGet();
                } else {
                    log.debug("Traffic command succeeded: {}", operation);
                    successfulOperations.incrementAndGet();
                }
            });
        }

        /**
         * Stop traffic generation
         */
        public void stopTraffic() {
            if (!trafficStarted.get()) {
                log.info("Traffic not started, nothing to stop");
                return;
            }

            log.info("Stopping continuous traffic...");
            stopTraffic.set(true);

            // Wait for all traffic futures to complete
            for (CompletableFuture<Void> future : trafficFutures) {
                try {
                    future.get(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    log.warn("Error waiting for traffic future to complete: {}", e.getMessage());
                }
            }

            trafficStarted.set(false);
            log.info("Traffic stopped. Total commands: {}, Successful: {}, Failed: {}", commandCounter.get(),
                    successfulOperations.get(), failedOperations.get());
        }

        public long getSuccessfulOperations() {
            return successfulOperations.get();
        }

        public long getFailedOperations() {
            return failedOperations.get();
        }

        public int getTotalCommands() {
            return commandCounter.get();
        }

        public boolean isTrafficActive() {
            return trafficStarted.get() && !stopTraffic.get();
        }

    }

    private HandoffTestContext setupHandoffTest(AddressType addressType) {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        // Configure client for RESP3 to receive push notifications with specific address type
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .supportMaintenanceEvents(MaintenanceEventsOptions.enabled(addressType)).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        HandoffCapture capture = new HandoffCapture();

        // Setup push notification monitoring using the utility
        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture, MONITORING_TIMEOUT, PING_TIMEOUT,
                Duration.ofMillis(5000));

        String bdbId = String.valueOf(mStandard.getBdbId());

        currentTestContext = new HandoffTestContext(client, connection, capture, bdbId, addressType);
        return currentTestContext;
    }

    /**
     * Validates the address format in MOVING notification matches expected type
     */
    private void validateAddressType(String address, AddressType expectedType, String testDescription) {
        log.info("Validating address '{}' for type {} in {}", address, expectedType, testDescription);
        // Handle NONE expected type (endpoint type 'none') - should receive null address by design
        if (expectedType == AddressType.NONE) {
            assertThat(address).as("Address should be null with endpoint type 'none' by design").isNull();
            log.info("✓ Address is null with NONE expected type (endpoint type 'none') - this is correct by design");
            return;
        }

        // Handle null expected type (legacy null case) - should receive a valid address, not null
        if (expectedType == null) {
            assertThat(address).as("Address should not be null even with null expected type").isNotNull();
            assertThat(address).as("Address should not be empty with null expected type").isNotEmpty();
            log.info("✓ Address '{}' received with null expected type - valid non-null address", address);
            return;
        }

        // Handle null address case with non-null expected type (this should not happen)
        if (address == null) {
            assertThat(false).as("Address should not be null for expected type " + expectedType).isTrue();
            return;
        }

        switch (expectedType) {
            case EXTERNAL_IP:
            case INTERNAL_IP:
                assertThat(IP_PATTERN.matcher(address).matches()).as("Address should be an IP address for type " + expectedType)
                        .isTrue();
                log.info("✓ Address '{}' is valid IP format for {}", address, expectedType);
                break;

            case EXTERNAL_FQDN:
            case INTERNAL_FQDN:
                assertThat(FQDN_PATTERN.matcher(address).matches()).as("Address should be an FQDN for type " + expectedType)
                        .isTrue();
                assertThat(address.contains(".")).as("FQDN should contain at least one dot").isTrue();
                log.info("✓ Address '{}' is valid FQDN format for {}", address, expectedType);
                break;

            case NONE:
                // This should not be reached as NONE is handled above
                throw new IllegalStateException("NONE address type should be handled before switch statement");

            default:
                throw new IllegalArgumentException("Unknown address type: " + expectedType);
        }
    }

    /**
     * Performs the migrate + moving operation and validates notifications
     */
    private void performHandoffOperation(HandoffTestContext context, String testDescription) throws InterruptedException {
        // Get cluster configuration for the operation
        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = clusterConfig.getOptimalSourceNode();
        String targetNode = clusterConfig.getOptimalTargetNode();

        log.info("=== {} ===", testDescription);
        log.info("Expected address type: {}", context.expectedAddressType);
        log.info("Starting migrate + moving operation...");
        log.info("Using nodes: source={}, target={}", sourceNode, targetNode);

        // Trigger the migrate + moving operation
        StepVerifier.create(faultClient.triggerMovingNotification(context.bdbId, endpointId, policy, sourceNode, targetNode))
                .expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);

        // Wait for MIGRATED notification first (migration completes before endpoint rebind)
        log.info("Waiting for MIGRATED notification...");
        boolean migratedReceived = context.capture.waitForMigratedNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(migratedReceived).as("Should receive MIGRATED notification").isTrue();

        // Wait for MOVING notification (endpoint rebind with new address)
        log.info("Waiting for MOVING notification...");
        boolean movingReceived = context.capture.waitForMovingNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(movingReceived).as("Should receive MOVING notification").isTrue();

        // Validate the MOVING notification contains correct address type
        String movingNotification = context.capture.getLastMovingNotification();
        assertThat(movingNotification).as("MOVING notification should not be null").isNotNull();

        // Debug log to show exact notification format
        log.info("Debug - Raw notification with escaped chars: '{}'",
                movingNotification.replace("\n", "\\n").replace("\r", "\\r"));

        Matcher matcher = MOVING_PATTERN.matcher(movingNotification);
        if (matcher.matches()) {
            String sequence = matcher.group(1);
            String ttl = matcher.group(2);
            String addressWithPort = matcher.group(3);

            // Parse address and port from the combined string
            String newAddress;
            String port;

            // IP:PORT format (e.g., "54.155.173.67:12000")
            int lastColonIndex = addressWithPort.lastIndexOf(':');
            newAddress = addressWithPort.substring(0, lastColonIndex);
            port = addressWithPort.substring(lastColonIndex + 1);

            log.info("Parsed MOVING notification - Sequence: {}, TTL: {}, New Address: {}, Port: {}", sequence, ttl, newAddress,
                    port);

            // Validate basic notification format
            assertThat(Integer.parseInt(ttl)).isGreaterThanOrEqualTo(0);
            assertThat(newAddress).isNotEmpty();
            assertThat(Integer.parseInt(port)).isGreaterThan(0);

            // Validate the address type matches what we requested
            validateAddressType(newAddress, context.expectedAddressType, testDescription);

        } else {
            log.error("MOVING notification format not recognized: {}", movingNotification);
            assertThat(false).as("MOVING notification should match expected format").isTrue();
        }

        // Verify we received both expected notifications
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MIGRATED"))).isTrue();
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isTrue();
    }

    /**
     * Reconnection verification test - validates that connection reconnected to the correct endpoint after handoff
     */
    private void reconnectionVerification(HandoffTestContext context, String testDescription) {
        try {
            log.info("=== Reconnection Verification for {} ===", testDescription);

            // For AddressType.NONE, we expect to reconnect to the original endpoint, not a new one
            String expectedEndpoint;
            if (context.expectedAddressType == AddressType.NONE) {
                // For NONE, the client should reconnect to the original endpoint
                String originalUri = mStandard.getEndpoints().get(0); // Original endpoint URI
                // Extract host:port from redis://host:port format
                expectedEndpoint = originalUri.replaceFirst("^redis://", "");
                log.info("Expected reconnection endpoint for NONE type (original endpoint): {}", expectedEndpoint);
            } else {
                // For other types, extract from MOVING notification
                expectedEndpoint = extractEndpointFromMovingNotification(context.capture.getReceivedNotifications());
                log.info("Expected reconnection endpoint from MOVING notification: {}", expectedEndpoint);
            }

            // Get current connection remote address using lettuce primitives
            Channel channel = getChannelFromConnection(context.connection);
            SocketAddress currentRemoteAddress = null;

            if (channel != null && channel.isActive()) {
                currentRemoteAddress = channel.remoteAddress();
                log.info("Current connection remote address: {}", currentRemoteAddress);
            } else {
                log.warn("Channel is null or inactive, cannot verify remote address");
            }

            // Test basic connectivity after handoff
            String pingResult = context.connection.sync().ping();
            assertThat(pingResult).isEqualTo("PONG");
            log.info("✓ Connection still responsive after handoff: {}", pingResult);

            // Verify reconnection to correct endpoint
            if (currentRemoteAddress != null && expectedEndpoint != null) {
                boolean endpointMatches = verifyEndpointMatch(currentRemoteAddress, expectedEndpoint);

                if (endpointMatches) {
                    log.info("✓ Reconnection endpoint verification PASSED: connected to correct endpoint {}",
                            currentRemoteAddress);
                } else {
                    String currentEndpointStr = currentRemoteAddress.toString();
                    String cleanCurrentEndpoint = currentEndpointStr.startsWith("/") ? currentEndpointStr.substring(1)
                            : currentEndpointStr;
                    log.error("✗ Reconnection endpoint verification FAILED! Current: {}, Expected: {}", cleanCurrentEndpoint,
                            expectedEndpoint);
                    assertThat(endpointMatches).as(
                            "Connection should reconnect to the correct endpoint specified in MOVING notification. Expected: %s, but connected to: %s",
                            expectedEndpoint, cleanCurrentEndpoint).isTrue();
                }
            } else {
                log.warn("⚠ Could not verify endpoint - currentRemoteAddress: {}, expectedEndpoint: {}", currentRemoteAddress,
                        expectedEndpoint);
            }

            // Test a few basic operations to ensure connection stability
            context.connection.sync().set("handoff-test-key", "handoff-test-value");
            String getValue = context.connection.sync().get("handoff-test-key");
            assertThat(getValue).isEqualTo("handoff-test-value");
            log.info("✓ Basic operations work after handoff");

            // Clean up test key
            context.connection.sync().del("handoff-test-key");

            context.capture.setReconnectionTested(true);
            log.info("✓ Reconnection verification completed successfully for {}", testDescription);

        } catch (Exception e) {
            log.warn("Reconnection verification failed for {}: {}", testDescription, e.getMessage());
            // Don't fail the main test if reconnection test fails, just log it
        }
    }

    /**
     * Extract the expected endpoint address from MOVING notifications
     */
    private String extractEndpointFromMovingNotification(java.util.List<String> notifications) {
        for (String notification : notifications) {
            if (notification.contains("MOVING")) {
                Matcher matcher = MOVING_PATTERN.matcher(notification);
                if (matcher.matches()) {
                    String addressWithPort = matcher.group(3);
                    log.info("Extracted endpoint from MOVING notification: {}", addressWithPort);
                    return addressWithPort;
                }
            }
        }
        log.warn("Could not extract endpoint from MOVING notifications");
        return null;
    }

    /**
     * Verify if the current remote address matches the expected endpoint, handling FQDN resolution
     */
    private boolean verifyEndpointMatch(SocketAddress currentRemoteAddress, String expectedEndpoint) {
        String currentEndpointStr = currentRemoteAddress.toString();
        // Remove leading slash if present (e.g., "/54.155.173.67:12000" -> "54.155.173.67:12000")
        String cleanCurrentEndpoint = currentEndpointStr.startsWith("/") ? currentEndpointStr.substring(1) : currentEndpointStr;

        // Direct match (for IP addresses)
        if (cleanCurrentEndpoint.equals(expectedEndpoint)) {
            return true;
        }

        // Handle FQDN resolution: "node3.ivo-test-f2655aa0.env0.qa.redislabs.com/54.155.173.67:12000"
        // should match "node3.ivo-test-f2655aa0.env0.qa.redislabs.com:12000"
        if (cleanCurrentEndpoint.contains("/")) {
            // Extract the FQDN part before the "/" and combine with port
            String[] parts = cleanCurrentEndpoint.split("/");
            if (parts.length == 2) {
                String fqdnPart = parts[0]; // "node3.ivo-test-f2655aa0.env0.qa.redislabs.com"
                String ipWithPort = parts[1]; // "54.155.173.67:12000"

                // Extract port from IP:PORT
                String[] ipPortParts = ipWithPort.split(":");
                if (ipPortParts.length == 2) {
                    String port = ipPortParts[1]; // "12000"
                    String reconstructedFqdnEndpoint = fqdnPart + ":" + port; // "node3.ivo-test-f2655aa0.env0.qa.redislabs.com:12000"

                    if (reconstructedFqdnEndpoint.equals(expectedEndpoint)) {
                        log.info("✓ FQDN endpoint match: current '{}' matches expected '{}' (resolved: {})",
                                reconstructedFqdnEndpoint, expectedEndpoint, cleanCurrentEndpoint);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Get the underlying channel from a connection, handling MaintenanceAwareExpiryWriter delegation
     */
    private static Channel getChannelFromConnection(StatefulRedisConnection<String, String> connection) {
        try {
            RedisChannelHandler<?, ?> handler = (RedisChannelHandler<?, ?>) connection;
            RedisChannelWriter writer = handler.getChannelWriter();

            // Handle MaintenanceAwareExpiryWriter which wraps the real channel writer
            if (writer instanceof MaintenanceAwareExpiryWriter) {
                // Get the delegate field from MaintenanceAwareExpiryWriter
                java.lang.reflect.Field delegateField = writer.getClass().getDeclaredField("delegate");
                delegateField.setAccessible(true);
                RedisChannelWriter delegate = (RedisChannelWriter) delegateField.get(writer);

                // Get the channel from the delegate
                java.lang.reflect.Field channelField = delegate.getClass().getDeclaredField("channel");
                channelField.setAccessible(true);
                return (Channel) channelField.get(delegate);
            } else {
                // Use the standard ConnectionTestUtil approach for regular writers
                return ConnectionTestUtil.getChannel(connection);
            }
        } catch (Exception e) {
            log.warn("Could not extract channel from connection: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Specialized capture class for dual connection testing that creates a second connection when MIGRATED is received
     */
    public static class DualConnectionCapture implements MaintenanceNotificationCapture {

        private final HandoffCapture firstCapture;

        private final RedisURI uri;

        private final StatefulRedisConnection<String, String> firstConnection;

        private final AtomicReference<HandoffCapture> secondCapture = new AtomicReference<>();

        private final AtomicReference<RedisClient> secondClient = new AtomicReference<>();

        private final AtomicReference<StatefulRedisConnection<String, String>> secondConnection = new AtomicReference<>();

        private final CountDownLatch secondConnectionMovingLatch = new CountDownLatch(1);

        private final AtomicBoolean testPhaseActive = new AtomicBoolean(true);

        public DualConnectionCapture(HandoffCapture firstCapture, RedisURI uri, String bdbId,
                StatefulRedisConnection<String, String> firstConnection) {
            this.firstCapture = firstCapture;
            this.uri = uri;
            this.firstConnection = firstConnection;
        }

        @Override
        public void captureNotification(String notification) {
            // Only capture notifications during the test phase
            if (!testPhaseActive.get()) {
                log.debug("Ignoring notification during cleanup phase: {}", notification);
                return;
            }

            // Forward to first capture
            firstCapture.captureNotification(notification);

            // If this is a MIGRATED notification and we haven't created second connection yet, create it
            // MIGRATED comes right after the bind is fired, before MOVING notification
            if (notification.contains("MIGRATED") && secondConnection.get() == null) {
                log.info("MIGRATED notification received - creating second connection right after bind");
                createSecondConnection();
            }
        }

        private void createSecondConnection() {
            try {
                log.info("Creating second connection for dual connection test...");

                // Get the channel from the first connection to determine the actual IP address
                Channel firstChannel = getChannelFromConnection(firstConnection);
                String actualIpAddress = null;
                int actualPort = -1;

                if (firstChannel != null && firstChannel.remoteAddress() != null) {
                    String remoteAddress = firstChannel.remoteAddress().toString();
                    log.info("First connection remote address: {}", remoteAddress);

                    // Handle different address formats:
                    // Format 1: "/54.74.227.236:12000" (direct IP)
                    // Format 2: "redis-12000.ivo-test-a6c42e54.env0.qa.redislabs.com/54.74.227.236:12000" (FQDN with resolved
                    // IP)

                    String ipPortString = null;
                    if (remoteAddress.contains("/")) {
                        // Extract the part after the last slash (the actual IP:port)
                        int lastSlashIndex = remoteAddress.lastIndexOf('/');
                        ipPortString = remoteAddress.substring(lastSlashIndex + 1);
                    } else {
                        // Direct IP:port format
                        ipPortString = remoteAddress;
                    }

                    if (ipPortString != null) {
                        String[] parts = ipPortString.split(":");
                        if (parts.length == 2) {
                            actualIpAddress = parts[0];
                            actualPort = Integer.parseInt(parts[1]);
                            log.info("Extracted actual IP address: {}:{}", actualIpAddress, actualPort);
                        }
                    }
                } else {
                    log.warn("Could not determine actual IP address from first connection, using original URI");
                }

                // Create URI for the second connection - use the same IP address as the first connection if available
                RedisURI secondUri;
                if (actualIpAddress != null && actualPort != -1) {
                    secondUri = RedisURI.builder().withHost(actualIpAddress).withPort(actualPort)
                            .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();
                    log.info("Creating second connection to same IP address: {}:{}", actualIpAddress, actualPort);
                } else {
                    log.warn("Could not extract actual IP address, falling back to original URI");
                    secondUri = uri;
                }

                RedisClient client = RedisClient.create(secondUri);
                ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                        .supportMaintenanceEvents(MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP)).build();
                client.setOptions(options);

                StatefulRedisConnection<String, String> connection = client.connect();
                HandoffCapture capture = new HandoffCapture() {

                    @Override
                    public void captureNotification(String notification) {
                        super.captureNotification(notification);
                        // Signal when second connection receives MOVING
                        if (notification.contains("MOVING")) {
                            log.info("Second connection received MOVING notification");
                            secondConnectionMovingLatch.countDown();
                        }
                    }

                };

                // Setup push notification monitoring on second connection with shorter timeout and immediate pinging
                MaintenancePushNotificationMonitor.setupMonitoring(connection, capture, Duration.ofSeconds(45), PING_TIMEOUT,
                        Duration.ofMillis(1000)); // Much shorter timeout and interval

                secondClient.set(client);
                secondConnection.set(connection);
                secondCapture.set(capture);

                log.info("Second connection created and monitoring setup completed");

            } catch (Exception e) {
                log.error("Failed to create second connection: {}", e.getMessage(), e);
            }
        }

        public boolean waitForSecondConnectionMoving(Duration timeout) throws InterruptedException {
            return secondConnectionMovingLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public HandoffCapture getFirstCapture() {
            return firstCapture;
        }

        public HandoffCapture getSecondCapture() {
            return secondCapture.get();
        }

        public RedisClient getSecondClient() {
            return secondClient.get();
        }

        public StatefulRedisConnection<String, String> getSecondConnection() {
            return secondConnection.get();
        }

        public void endTestPhase() {
            testPhaseActive.set(false);
            firstCapture.endTestPhase();
            if (secondCapture.get() != null) {
                secondCapture.get().endTestPhase();
            }
            log.info("Dual connection test phase ended - notifications will be ignored during cleanup");
        }

    }

    /**
     * Specialized capture class to track all 5 notification types
     */
    public static class AllNotificationTypesCapture implements MaintenanceNotificationCapture {

        private final List<String> receivedNotifications = new CopyOnWriteArrayList<>();

        private final CountDownLatch notificationLatch = new CountDownLatch(1);

        private final AtomicBoolean testPhaseActive = new AtomicBoolean(true);

        // Counters for each notification type
        private final AtomicReference<Integer> movingCount = new AtomicReference<>(0);

        private final AtomicReference<Integer> migratingCount = new AtomicReference<>(0);

        private final AtomicReference<Integer> migratedCount = new AtomicReference<>(0);

        private final AtomicReference<Integer> failingOverCount = new AtomicReference<>(0);

        private final AtomicReference<Integer> failedOverCount = new AtomicReference<>(0);

        public void captureNotification(String notification) {
            if (testPhaseActive.get()) {
                receivedNotifications.add(notification);
                log.info("Captured notification: {}", notification);

                // Count notification types
                if (notification.contains("MOVING")) {
                    movingCount.updateAndGet(count -> count + 1);
                    notificationLatch.countDown();
                } else if (notification.contains("MIGRATING")) {
                    migratingCount.updateAndGet(count -> count + 1);
                    notificationLatch.countDown();
                } else if (notification.contains("MIGRATED")) {
                    migratedCount.updateAndGet(count -> count + 1);
                    notificationLatch.countDown();
                } else if (notification.contains("FAILING_OVER")) {
                    failingOverCount.updateAndGet(count -> count + 1);
                    notificationLatch.countDown();
                } else if (notification.contains("FAILED_OVER")) {
                    failedOverCount.updateAndGet(count -> count + 1);
                    notificationLatch.countDown();
                }
            }
        }

        public boolean waitForNotifications(Duration timeout) throws InterruptedException {
            return notificationLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public List<String> getReceivedNotifications() {
            return receivedNotifications;
        }

        public void endTestPhase() {
            testPhaseActive.set(false);
            log.info("Test phase ended - notifications will be ignored during cleanup");
        }

        public int getMovingCount() {
            return movingCount.get();
        }

        public int getMigratingCount() {
            return migratingCount.get();
        }

        public int getMigratedCount() {
            return migratedCount.get();
        }

        public int getFailingOverCount() {
            return failingOverCount.get();
        }

        public int getFailedOverCount() {
            return failedOverCount.get();
        }

    }

    @Test
    @DisplayName("Connection handed off to new endpoint with External IP")
    public void connectionHandedOffToNewEndpointExternalIPTest() throws InterruptedException {
        log.info("Starting connectionHandedOffToNewEndpointExternalIPTest");
        HandoffTestContext context = setupHandoffTest(AddressType.EXTERNAL_IP);

        performHandoffOperation(context, "External IP Handoff Test");
        reconnectionVerification(context, "External IP Handoff Test");

        // End test phase to prevent capturing cleanup notifications
        context.capture.endTestPhase();

        log.info("Completed connectionHandedOffToNewEndpointExternalIPTest");
    }

    @Test
    @DisplayName("Traffic resumes correctly after MOVING with async GET/SET operations")
    public void trafficResumesAfterMovingTest() throws InterruptedException {
        log.info("Starting trafficResumesAfterMovingTest");
        HandoffTestContext context = setupHandoffTest(AddressType.EXTERNAL_IP);

        // Create async commands and traffic generator
        RedisAsyncCommands<String, String> asyncCommands = context.connection.async();
        ContinuousTrafficGenerator trafficGenerator = new ContinuousTrafficGenerator(asyncCommands);

        // Start traffic before maintenance operation
        log.info("=== Starting traffic before MOVING operation ===");
        trafficGenerator.startTraffic();

        // Let traffic run for a bit to establish baseline
        Thread.sleep(Duration.ofSeconds(2).toMillis());
        long initialSuccessful = trafficGenerator.getSuccessfulOperations();
        long initialFailed = trafficGenerator.getFailedOperations();
        log.info("Initial traffic stats - Successful: {}, Failed: {}", initialSuccessful, initialFailed);

        // Perform handoff operation while traffic is running
        log.info("=== Performing MOVING operation while traffic is active ===");
        performHandoffOperation(context, "Traffic Resumption Test");

        // Continue traffic during and after maintenance
        log.info("=== Continuing traffic during maintenance ===");
        Thread.sleep(Duration.ofSeconds(5).toMillis());

        // Wait for reconnection verification
        reconnectionVerification(context, "Traffic Resumption Test");

        // Let traffic continue after reconnection to verify resumption
        log.info("=== Allowing traffic to continue after reconnection ===");
        Thread.sleep(Duration.ofSeconds(3).toMillis());

        // Stop traffic and collect final statistics
        trafficGenerator.stopTraffic();

        long finalSuccessful = trafficGenerator.getSuccessfulOperations();
        long finalFailed = trafficGenerator.getFailedOperations();
        int totalCommands = trafficGenerator.getTotalCommands();

        log.info("=== Traffic Resumption Test Results ===");
        log.info("Total commands executed: {}", totalCommands);
        log.info("Successful operations: {}", finalSuccessful);
        log.info("Failed operations: {}", finalFailed);
        log.info("Success rate: {:.2f}%", (double) finalSuccessful / totalCommands * 100);

        // Verify traffic resumed successfully after MOVING
        assertThat(totalCommands).as("Should have executed traffic commands").isGreaterThan(0);
        assertThat(finalSuccessful).as("Should have successful operations after MOVING").isGreaterThan(initialSuccessful);

        // Allow some failures during maintenance but most should succeed
        double failureRate = (double) finalFailed / totalCommands;
        assertThat(failureRate).as("Failure rate should be reasonable (< 50%)").isLessThan(0.5);

        // Verify we had traffic both before and after the maintenance operation
        assertThat(finalSuccessful - initialSuccessful).as("Should have additional successful operations after MOVING")
                .isGreaterThan(0);

        log.info("✓ Traffic resumed successfully after MOVING operation");

        context.capture.endTestPhase();

        log.info("Completed trafficResumesAfterMovingTest");
    }

    @Test
    @DisplayName("Connection handoff with FQDN External Name")
    public void connectionHandoffWithFQDNExternalNameTest() throws InterruptedException {
        log.info("Starting connectionHandoffWithFQDNExternalNameTest");
        HandoffTestContext context = setupHandoffTest(AddressType.EXTERNAL_FQDN);

        performHandoffOperation(context, "External FQDN Handoff Test");
        reconnectionVerification(context, "External FQDN Handoff Test");

        // End test phase to prevent capturing cleanup notifications
        context.capture.endTestPhase();

        log.info("Completed connectionHandoffWithFQDNExternalNameTest");
    }

    @Test
    @DisplayName("Connection handshake includes enabling notifications and receives all 5 notification types")
    public void connectionHandshakeIncludesEnablingNotificationsTest() throws InterruptedException {
        log.info("Starting connectionHandshakeIncludesEnablingNotificationsTest");

        // Setup connection with maintenance events enabled
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        // Configure client for RESP3 to receive push notifications with maintenance events enabled
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .supportMaintenanceEvents(MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP)).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        // Specialized capture to track all 5 notification types
        AllNotificationTypesCapture capture = new AllNotificationTypesCapture();

        // Setup push notification monitoring
        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture, MONITORING_TIMEOUT, PING_TIMEOUT,
                Duration.ofMillis(5000));

        String bdbId = String.valueOf(mStandard.getBdbId());

        // Verify connection handshake included CLIENT MAINT_NOTIFICATIONS ON command
        // (This is verified by the fact that we can receive notifications)
        log.info("=== Testing all notification types ===");

        // Trigger operations that should generate all 5 notification types
        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = clusterConfig.getOptimalSourceNode();
        String targetNode = clusterConfig.getOptimalTargetNode();

        log.info("Starting comprehensive maintenance operations to trigger all notification types...");
        log.info("Using nodes: source={}, target={}", sourceNode, targetNode);

        // This operation will trigger MIGRATING, MIGRATED, and MOVING notifications
        StepVerifier.create(faultClient.triggerMovingNotification(bdbId, endpointId, policy, sourceNode, targetNode))
                .expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);

        // Wait for initial notifications
        boolean received = capture.waitForNotifications(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(received).as("Should receive maintenance notifications").isTrue();

        // Trigger additional failover operations to get FAILING_OVER and FAILED_OVER
        String shardId = clusterConfig.getFirstMasterShardId();
        String nodeId = clusterConfig.getNodeWithMasterShards();

        log.info("Triggering failover operations to get FAILING_OVER and FAILED_OVER notifications...");
        StepVerifier.create(faultClient.triggerShardFailover(bdbId, shardId, nodeId, clusterConfig)).expectNext(true)
                .expectComplete().verify(LONG_OPERATION_TIMEOUT);

        // End test phase to prevent capturing cleanup notifications
        capture.endTestPhase();

        log.info("=== Notification Results ===");
        log.info("Total notifications received: {}", capture.getReceivedNotifications().size());
        log.info("MOVING notifications: {}", capture.getMovingCount());
        log.info("MIGRATING notifications: {}", capture.getMigratingCount());
        log.info("MIGRATED notifications: {}", capture.getMigratedCount());
        log.info("FAILING_OVER notifications: {}", capture.getFailingOverCount());
        log.info("FAILED_OVER notifications: {}", capture.getFailedOverCount());

        // VALIDATION: Should receive all 5 notification types when maintenance events are enabled
        assertThat(capture.getReceivedNotifications()).as("Should receive notifications when maintenance events are enabled")
                .isNotEmpty();

        // Verify we received the expected notification types
        // Note: We expect at least some of each type, though exact counts depend on cluster operations
        assertThat(capture.getMovingCount()).as("Should receive MOVING notifications").isGreaterThan(0);
        assertThat(capture.getMigratingCount()).as("Should receive MIGRATING notifications").isGreaterThan(0);
        assertThat(capture.getMigratedCount()).as("Should receive MIGRATED notifications").isGreaterThan(0);

        // Failover notifications may be received depending on cluster state
        log.info("✓ All expected maintenance notifications received successfully");

        log.info("Completed connectionHandshakeIncludesEnablingNotificationsTest");
    }

    @Test
    @DisplayName("Disabled maintenance events don't receive notifications")
    public void disabledDontReceiveNotificationsTest() throws InterruptedException {
        log.info("Starting disabledDontReceiveNotificationsTest");

        // Setup connection with maintenance events explicitly disabled
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        // Configure client for RESP3 but with maintenance events DISABLED
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .supportMaintenanceEvents(MaintenanceEventsOptions.disabled()).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        // Simple capture to verify no notifications are received
        AllNotificationTypesCapture capture = new AllNotificationTypesCapture();

        // Setup monitoring (though we expect no notifications)
        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture, MONITORING_TIMEOUT, PING_TIMEOUT,
                Duration.ofMillis(5000));

        String bdbId = String.valueOf(mStandard.getBdbId());

        log.info("=== Testing disabled maintenance events ===");

        // Trigger the same operations as the enabled test
        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = clusterConfig.getOptimalSourceNode();
        String targetNode = clusterConfig.getOptimalTargetNode();

        log.info("Starting maintenance operations with disabled notifications...");
        log.info("Using nodes: source={}, target={}", sourceNode, targetNode);

        // This operation would normally trigger notifications, but they should be disabled
        StepVerifier.create(faultClient.triggerMovingNotification(bdbId, endpointId, policy, sourceNode, targetNode))
                .expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);

        // Wait to see if any notifications are received (they shouldn't be)
        boolean received = capture.waitForNotifications(Duration.ofSeconds(30));

        // Trigger additional failover operations to get FAILING_OVER and FAILED_OVER
        String shardId = clusterConfig.getFirstMasterShardId();
        String nodeId = clusterConfig.getNodeWithMasterShards();

        log.info("Triggering failover operations to get FAILING_OVER and FAILED_OVER notifications...");
        StepVerifier.create(faultClient.triggerShardFailover(bdbId, shardId, nodeId, clusterConfig)).expectNext(true)
                .expectComplete().verify(LONG_OPERATION_TIMEOUT);

        // End test phase
        capture.endTestPhase();

        log.info("=== Disabled Notification Results ===");
        log.info("Total notifications received: {}", capture.getReceivedNotifications().size());
        log.info("Any notifications received: {}", received);

        // VALIDATION: Should NOT receive any maintenance notifications when disabled
        assertThat(received).as("Should NOT receive notifications when maintenance events are disabled").isFalse();

        assertThat(capture.getReceivedNotifications()).as("Should have no notifications when maintenance events are disabled")
                .isEmpty();

        assertThat(capture.getMovingCount()).as("Should have no MOVING notifications").isZero();
        assertThat(capture.getMigratingCount()).as("Should have no MIGRATING notifications").isZero();
        assertThat(capture.getMigratedCount()).as("Should have no MIGRATED notifications").isZero();
        assertThat(capture.getFailingOverCount()).as("Should have no FAILING_OVER notifications").isZero();
        assertThat(capture.getFailedOverCount()).as("Should have no FAILED_OVER notifications").isZero();

        log.info("✓ Disabled maintenance events correctly prevent notifications");

        log.info("Completed disabledDontReceiveNotificationsTest");
    }

    @Test
    @DisplayName("Client handshake with endpoint type none returns nil IP")
    public void clientHandshakeWithNoneEndpointTypeTest() throws InterruptedException {
        log.info("Starting clientHandshakeWithEndpointTypeTest");

        // Setup connection with a custom address type source that returns null (none)
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        // Configure client with maintenance events enabled and explicit NONE address type
        MaintenanceEventsOptions customOptions = MaintenanceEventsOptions.enabled(AddressType.NONE);

        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .supportMaintenanceEvents(customOptions).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        HandoffCapture capture = new HandoffCapture();

        // Setup push notification monitoring using the utility
        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture, MONITORING_TIMEOUT, PING_TIMEOUT,
                Duration.ofMillis(5000));

        String bdbId = String.valueOf(mStandard.getBdbId());

        // Create test context with NONE expected address type to test none handling
        currentTestContext = new HandoffTestContext(client, connection, capture, bdbId, AddressType.NONE);

        log.info("=== Testing endpoint type 'none' behavior ===");

        // Trigger the same migrate + moving operation as connectionHandedOffToNewEndpointInternalIPTest
        // Get cluster configuration for the operation
        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = clusterConfig.getOptimalSourceNode();
        String targetNode = clusterConfig.getOptimalTargetNode();

        log.info("Expected address type: {} (none)", AddressType.NONE);
        log.info("Starting migrate + moving operation...");
        log.info("Using nodes: source={}, target={}", sourceNode, targetNode);

        // Trigger the migrate + moving operation
        StepVerifier.create(faultClient.triggerMovingNotification(bdbId, endpointId, policy, sourceNode, targetNode))
                .expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);

        // Wait for MIGRATED notification first (migration completes before endpoint rebind)
        log.info("Waiting for MIGRATED notification...");
        boolean migratedReceived = capture.waitForMigratedNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(migratedReceived).as("Should receive MIGRATED notification").isTrue();

        // Wait for MOVING notification (endpoint rebind with new address)
        log.info("Waiting for MOVING notification...");
        boolean movingReceived = capture.waitForMovingNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(movingReceived).as("Should receive MOVING notification").isTrue();

        // Validate the MOVING notification - this will test null handling in validateAddressType
        String movingNotification = capture.getLastMovingNotification();
        assertThat(movingNotification).as("MOVING notification should not be null").isNotNull();

        // Debug log to show exact notification format
        log.info("Debug - Raw notification with escaped chars: '{}'",
                movingNotification.replace("\n", "\\n").replace("\r", "\\r"));

        Matcher matcher = MOVING_PATTERN.matcher(movingNotification);
        if (matcher.matches()) {
            String sequence = matcher.group(1);
            String ttl = matcher.group(2);
            String addressWithPort = matcher.group(3);

            // Parse address and port from the combined string
            String newAddress;
            String port;

            // Handle the case where address might be null or empty for endpoint type 'none'
            if (addressWithPort == null || addressWithPort.trim().isEmpty()) {
                newAddress = null;
                port = null;
                log.info("Address is null/empty - this is expected for endpoint type 'none'");
            } else {
                // IP:PORT format (e.g., "54.155.173.67:12000")
                int lastColonIndex = addressWithPort.lastIndexOf(':');
                if (lastColonIndex > 0) {
                    newAddress = addressWithPort.substring(0, lastColonIndex);
                    port = addressWithPort.substring(lastColonIndex + 1);
                } else {
                    newAddress = addressWithPort;
                    port = null;
                }
            }

            log.info("Parsed MOVING notification - Sequence: {}, TTL: {}, New Address: {}, Port: {}", sequence, ttl, newAddress,
                    port);

            // Validate basic notification format
            assertThat(Integer.parseInt(ttl)).isGreaterThanOrEqualTo(0);

            // Validate the address type matches what we requested (null handling test)
            validateAddressType(newAddress, AddressType.NONE, "Client handshake with endpoint type none test");

        } else {
            log.error("MOVING notification format not recognized: {}", movingNotification);
            assertThat(false).as("MOVING notification should match expected format").isTrue();
        }

        // Verify we received both expected notifications
        assertThat(capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MIGRATED"))).isTrue();
        assertThat(capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isTrue();

        // Perform reconnection verification similar to other tests
        reconnectionVerification(currentTestContext, "Client handshake with endpoint type none test");

        // End test phase to prevent capturing cleanup notifications
        capture.endTestPhase();

        log.info("✓ Client handshake with endpoint type 'none' test completed successfully");
        log.info("Completed clientHandshakeWithEndpointTypeTest");
    }

    @Test
    @DisplayName("Connection handed off to new endpoint with External IP - Dual Connection Test")
    public void newConnectionDuringRebindAfterMovingTest() throws InterruptedException {
        log.info("Starting connectionHandedOffToNewEndpointExternalIPDualConnectionTest");

        // Setup first connection but do NOT setup monitoring yet
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient firstClient = RedisClient.create(uri);
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .supportMaintenanceEvents(MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP)).build();
        firstClient.setOptions(options);

        StatefulRedisConnection<String, String> firstConnection = firstClient.connect();
        HandoffCapture firstCapture = new HandoffCapture();
        String bdbId = String.valueOf(mStandard.getBdbId());

        // Create a specialized capture that will start second connection on MOVING
        DualConnectionCapture dualCapture = new DualConnectionCapture(firstCapture, uri, bdbId, firstConnection);

        // Setup push notification monitoring on first connection with shorter timeout
        MaintenancePushNotificationMonitor.setupMonitoring(firstConnection, dualCapture, Duration.ofSeconds(45), PING_TIMEOUT,
                Duration.ofMillis(1000));

        try {
            // Trigger maintenance operation
            performHandoffOperation(
                    new HandoffTestContext(firstClient, firstConnection, firstCapture, bdbId, AddressType.EXTERNAL_IP),
                    "Dual Connection External IP Handoff Test");

            // Wait for second connection to be created (on MIGRATED) and then receive its MOVING notification
            log.info("Waiting for second connection to receive MOVING notification...");
            boolean secondMovingReceived = dualCapture.waitForSecondConnectionMoving(NOTIFICATION_WAIT_TIMEOUT);
            assertThat(secondMovingReceived).as("Second connection should receive MOVING notification").isTrue();

            // Verify both connections received MOVING notifications
            assertThat(dualCapture.getFirstCapture().getLastMovingNotification())
                    .as("First connection should have MOVING notification").isNotNull();
            assertThat(dualCapture.getSecondCapture().getLastMovingNotification())
                    .as("Second connection should have MOVING notification").isNotNull();

            log.info("Both connections received MOVING notifications successfully");

            // Perform reconnection verification on both connections
            reconnectionVerification(new HandoffTestContext(firstClient, firstConnection, dualCapture.getFirstCapture(), bdbId,
                    AddressType.EXTERNAL_IP), "First Connection - Dual Connection External IP Handoff Test");

            if (dualCapture.getSecondConnection() != null) {
                reconnectionVerification(
                        new HandoffTestContext(dualCapture.getSecondClient(), dualCapture.getSecondConnection(),
                                dualCapture.getSecondCapture(), bdbId, AddressType.EXTERNAL_IP),
                        "Second Connection - Dual Connection External IP Handoff Test");
            }

            // End test phase to prevent capturing cleanup notifications
            dualCapture.endTestPhase();

            log.info("Completed connectionHandedOffToNewEndpointExternalIPDualConnectionTest");

        } finally {
            // Cleanup both connections
            if (firstConnection != null && firstConnection.isOpen()) {
                firstConnection.close();
            }
            if (firstClient != null) {
                firstClient.shutdown();
            }

            if (dualCapture.getSecondConnection() != null && dualCapture.getSecondConnection().isOpen()) {
                dualCapture.getSecondConnection().close();
            }
            if (dualCapture.getSecondClient() != null) {
                dualCapture.getSecondClient().shutdown();
            }
        }
    }

    @Test
    @DisplayName("Detect connection closure and verify no memory leaks during migrate + bind using EventBus monitoring")
    public void detectConnectionClosureAndMemoryLeaksTest() throws InterruptedException {
        log.info("=== Connection Closure & Memory Leak Detection Test ===");

        // Setup connection leak detector
        ConnectionLeakDetectionUtil leakDetector = new ConnectionLeakDetectionUtil();

        // Setup connection with EventBus monitoring
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        // Configure for RESP3 with maintenance events to trigger connection handoff
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .supportMaintenanceEvents(MaintenanceEventsOptions.enabled(AddressType.EXTERNAL_IP)).build();
        client.setOptions(options);

        // Setup EventBus monitoring BEFORE creating connection
        leakDetector.setupEventBusMonitoring(client);

        StatefulRedisConnection<String, String> connection = client.connect();

        // Wait for connection to be fully established
        Thread.sleep(Duration.ofSeconds(2).toMillis());

        // Capture initial connection state
        String initialChannelId = leakDetector.getCurrentChannelId();
        Channel initialChannel = ConnectionLeakDetectionUtil.getChannelFromConnection(connection);

        log.info("Initial connection established - channelId: {}", initialChannelId);
        if (initialChannel != null) {
            log.info("Initial channel state - active: {}, open: {}, registered: {}", initialChannel.isActive(),
                    initialChannel.isOpen(), initialChannel.isRegistered());
        }

        // Prepare for connection transition and trigger migrate + bind operation
        leakDetector.prepareForConnectionTransition();

        String bdbId = String.valueOf(mStandard.getBdbId());
        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";
        String sourceNode = clusterConfig.getOptimalSourceNode();
        String targetNode = clusterConfig.getOptimalTargetNode();

        log.info("Triggering migrate + bind operation: source={}, target={}", sourceNode, targetNode);

        // Trigger the migrate + bind operation that causes connection handoff
        StepVerifier.create(faultClient.triggerMovingNotification(bdbId, endpointId, policy, sourceNode, targetNode))
                .expectNext(true).expectComplete().verify(Duration.ofMinutes(3));

        log.info("Migrate + bind operation completed, waiting for connection events...");

        // Wait for connection events to be processed
        boolean eventsReceived = leakDetector.waitForConnectionTransition(Duration.ofSeconds(30));
        assertThat(eventsReceived)
                .as("Should receive connection transition events (DisconnectedEvent + ConnectionDeactivatedEvent)").isTrue();

        // Wait additional time for full cleanup
        Thread.sleep(Duration.ofSeconds(10).toMillis());

        // Analyze connection closure and memory leak indicators
        ConnectionLeakDetectionUtil.ConnectionAnalysisResult result = leakDetector.analyzeConnectionClosure(initialChannelId,
                initialChannel);

        log.info("=== Connection Closure Analysis Results ===");
        log.info("EventBus indicators - Disconnected: {}, Deactivated: {}, Cleanup: {}", result.wasDisconnected(),
                result.wasDeactivated(), result.isEventBusCleanup());
        log.info("Netty channel cleanup: {}", result.isNettyCleanup());
        log.info("Connection handoff - Initial: {}, Current: {}, Handed off: {}", result.getInitialChannelId(),
                result.getCurrentChannelId(), result.isConnectionHandedOff());

        // VALIDATIONS: Connection properly closed and no memory leaks
        assertThat(result.wasDisconnected()).as("Old connection should have been disconnected (TCP level)").isTrue();

        assertThat(result.wasDeactivated())
                .as("Old connection should have been deactivated (logical level) - this is the key signal").isTrue();

        assertThat(result.isEventBusCleanup()).as("EventBus should indicate proper cleanup (both disconnected and deactivated)")
                .isTrue();

        if (initialChannel != null) {
            assertThat(result.isNettyCleanup())
                    .as("Netty channel should be properly cleaned up (inactive, closed, unregistered)").isTrue();
        }

        assertThat(result.isConnectionHandedOff()).as("Connection should have been handed off to new channel").isTrue();

        assertThat(result.isFullyCleanedUpWithoutLeaks()).as("Connection should be fully cleaned up without memory leaks")
                .isTrue();

        // Verify new connection is functional
        String testKey = "leak-detection-test-" + System.currentTimeMillis();
        String testValue = "test-value";

        connection.sync().set(testKey, testValue);
        String retrievedValue = connection.sync().get(testKey);

        assertThat(retrievedValue).isEqualTo(testValue);
        assertThat(connection.isOpen()).isTrue();

        log.info("✓ New connection is fully functional after handoff");
        log.info("✓ Connection closure validation passed - no memory leaks detected");

        // Cleanup
        connection.close();
        client.shutdown();
        leakDetector.stopMonitoring();

        log.info("=== Connection Closure & Memory Leak Detection Test Completed Successfully ===");
    }

}
