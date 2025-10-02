package io.lettuce.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.InetSocketAddress;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.MaintNotificationsConfig;
import io.lettuce.core.MaintNotificationsConfig.EndpointType;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.KeyValue;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.ConnectionTestUtil;
import io.lettuce.test.env.Endpoints;
import io.netty.channel.Channel;
import io.lettuce.test.env.Endpoints.Endpoint;

import reactor.test.StepVerifier;

import static io.lettuce.TestTags.SCENARIO_TEST;

@Tag(SCENARIO_TEST)
public class MaintenanceNotificationConnectionTest {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceNotificationConnectionTest.class);

    private static final Duration NOTIFICATION_WAIT_TIMEOUT = Duration.ofMinutes(3);

    private static final Duration LONG_OPERATION_TIMEOUT = Duration.ofMinutes(5);

    private static final int TRAFFIC_OPERATION_THRESHOLD = 1000;

    private static final Duration TRAFFIC_AWAIT_TIMEOUT = Duration.ofSeconds(5);

    private static Endpoint mStandard;

    private RedisEnterpriseConfig clusterConfig;

    private final FaultInjectionClient faultClient = new FaultInjectionClient();

    private TestContext currentTestContext;

    // Push notification patterns for MOVING messages with different address types
    // Handles both IP:PORT and FQDN formats, with both \n and \r\n line endings
    // Also handles empty address for EndpointType.NONE
    private static final Pattern MOVING_PATTERN = Pattern
            .compile(">\\d+\\r?\\nMOVING\\r?\\n:([^\\r\\n]+)\\r?\\n:(\\d+)\\r?\\n([^\\r\\n]*)\\s*");

    // Pattern to identify IP addresses (IPv4)
    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

    // Pattern to identify FQDNs (contains at least one dot and alphabetic characters)
    private static final Pattern FQDN_PATTERN = Pattern
            .compile("^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$");

    // Push notification patterns for all 5 notification types
    private static final Pattern MOVING_NOTIFICATION_PATTERN = Pattern
            .compile(">4\\r\\nMOVING\\r\\n:(\\d+)\\r\\n:(\\d+)\\r\\n([^:]+):(\\d+)\\r\\n");

    private static final Pattern MIGRATING_NOTIFICATION_PATTERN = Pattern
            .compile(">4\\r\\nMIGRATING\\r\\n:(\\d+)\\r\\n:(\\d+)\\r\\n:(.+)\\r\\n");

    private static final Pattern MIGRATED_NOTIFICATION_PATTERN = Pattern
            .compile(">3\\r\\nMIGRATED\\r\\n:(\\d+)\\r\\n:(.+)\\r\\n");

    private static final Pattern FAILING_OVER_NOTIFICATION_PATTERN = Pattern
            .compile(">4\\r\\nFAILING_OVER\\r\\n:(\\d+)\\r\\n:(\\d+)\\r\\n:(.+)\\r\\n");

    private static final Pattern FAILED_OVER_NOTIFICATION_PATTERN = Pattern
            .compile(">3\\r\\nFAILED_OVER\\r\\n:(\\d+)\\r\\n:(.+)\\r\\n");

    public enum NotificationType {

        MOVING(MOVING_NOTIFICATION_PATTERN, "MOVING", 4), MIGRATING(MIGRATING_NOTIFICATION_PATTERN, "MIGRATING", 3), MIGRATED(
                MIGRATED_NOTIFICATION_PATTERN, "MIGRATED", 2), FAILING_OVER(FAILING_OVER_NOTIFICATION_PATTERN, "FAILING_OVER",
                        3), FAILED_OVER(FAILED_OVER_NOTIFICATION_PATTERN, "FAILED_OVER", 2);

        private final Pattern pattern;

        private final String notificationName;

        private final int expectedGroups;

        NotificationType(Pattern pattern, String notificationName, int expectedGroups) {
            this.pattern = pattern;
            this.notificationName = notificationName;
            this.expectedGroups = expectedGroups;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public String getNotificationName() {
            return notificationName;
        }

        public int getExpectedGroups() {
            return expectedGroups;
        }

    }

    public static class ParsedNotification {

        private final String sequenceNumber;

        private final String timeSeconds;

        private final String shardId;

        private final String ipAddress;

        private final String port;

        private final NotificationType type;

        public ParsedNotification(NotificationType type, String sequenceNumber, String timeSeconds, String shardId,
                String ipAddress, String port) {
            this.type = type;
            this.sequenceNumber = sequenceNumber;
            this.timeSeconds = timeSeconds;
            this.shardId = shardId;
            this.ipAddress = ipAddress;
            this.port = port;
        }

        public String getSequenceNumber() {
            return sequenceNumber;
        }

        public String getTimeSeconds() {
            return timeSeconds;
        }

        public String getShardId() {
            return shardId;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public String getPort() {
            return port;
        }

        public NotificationType getType() {
            return type;
        }

    }

    public static ParsedNotification validateAndParseNotification(String notification, NotificationType expectedType) {
        assertThat(notification).as("Notification should not be null").isNotNull();
        assertThat(notification).as("Notification should contain " + expectedType.getNotificationName())
                .contains(expectedType.getNotificationName());

        log.info("Validating {} notification: {}", expectedType.getNotificationName(),
                notification.replace("\n", "\\n").replace("\r", "\\r"));

        Matcher matcher = expectedType.getPattern().matcher(notification);
        assertThat(matcher.matches()).as(expectedType.getNotificationName() + " notification should match expected format")
                .isTrue();

        String sequenceNumber = matcher.group(1);
        String timeSeconds = null;
        String shardId = null;
        String ipAddress = null;
        String port = null;

        // Parse based on notification type
        switch (expectedType) {
            case MOVING:
                timeSeconds = matcher.group(2);
                ipAddress = matcher.group(3);
                port = matcher.group(4);
                log.info("Parsed MOVING - Seq: {}, Time: {}, IP: {}, Port: {}", sequenceNumber, timeSeconds, ipAddress, port);
                break;
            case MIGRATING:
                timeSeconds = matcher.group(2);
                shardId = matcher.group(3); // This will be JSON array like ["2", "4"] for multiple shards
                log.info("Parsed MIGRATING - Seq: {}, Time: {}, Shard: {}", sequenceNumber, timeSeconds, shardId);
                break;
            case MIGRATED:
                shardId = matcher.group(2); // This will be JSON array like ["2", "4"] for multiple shards
                log.info("Parsed MIGRATED - Seq: {}, Shard: {}", sequenceNumber, shardId);
                break;
            case FAILING_OVER:
                timeSeconds = matcher.group(2);
                shardId = matcher.group(3); // This will be JSON array like ["2", "4"] for multiple shards
                log.info("Parsed FAILING_OVER - Seq: {}, Time: {}, Shard: {}", sequenceNumber, timeSeconds, shardId);
                break;
            case FAILED_OVER:
                shardId = matcher.group(2); // This will be JSON array like ["2", "4"] for multiple shards
                log.info("Parsed FAILED_OVER - Seq: {}, Shard: {}", sequenceNumber, shardId);
                break;
        }

        // Validate common fields
        assertThat(Long.parseLong(sequenceNumber)).as("Sequence number should be positive").isGreaterThan(0L);

        if (timeSeconds != null) {
            assertThat(Long.parseLong(timeSeconds)).as("Time seconds should be positive").isGreaterThan(0L);
        }

        if (shardId != null) {
            assertThat(shardId).as("Shard ID should not be empty").isNotEmpty();
            // Shard ID can be either a single number or JSON array like ["2", "4"]
            if (shardId.startsWith("[") && shardId.endsWith("]")) {
                log.info("Shard ID is JSON array format: {}", shardId);
            } else {
                // Single shard ID - validate it's numeric
                try {
                    Long.parseLong(shardId);
                } catch (NumberFormatException e) {
                    // If it's not numeric and not JSON array, it might be a different format
                    log.warn("Shard ID '{}' is neither numeric nor JSON array format", shardId);
                }
            }
        }

        if (ipAddress != null) {
            assertThat(ipAddress).as("IP address should not be empty").isNotEmpty();
        }

        if (port != null) {
            assertThat(Integer.parseInt(port)).as("Port should be positive").isGreaterThan(0);
        }

        return new ParsedNotification(expectedType, sequenceNumber, timeSeconds, shardId, ipAddress, port);
    }

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
            RedisEnterpriseConfig.refreshClusterConfig(faultClient, String.valueOf(mStandard.getBdbId()));
            log.info("Cluster state restored successfully");
        } catch (Exception e) {
            log.warn("Failed to restore cluster state: {}", e.getMessage());
        }
    }

    @AfterEach
    public void cleanupTest() {
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

    private static class TestContext {

        final RedisClient client;

        final StatefulRedisConnection<String, String> connection;

        final TestCapture capture;

        final String bdbId;

        final EndpointType expectedEndpointType;

        TestContext(RedisClient client, StatefulRedisConnection<String, String> connection, TestCapture capture, String bdbId,
                EndpointType expectedEndpointType) {
            this.client = client;
            this.connection = connection;
            this.capture = capture;
            this.bdbId = bdbId;
            this.expectedEndpointType = expectedEndpointType;
        }

    }

    public static class TestCapture implements MaintenanceNotificationCapture {

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

                        if (cmdNumber % 2 == 0) {
                            String value = "value-" + cmdNumber;
                            RedisFuture<String> future = asyncCommands.set(key, value);
                            handleAsyncResult(future, "SET " + key);
                        } else {
                            RedisFuture<String> future = asyncCommands.get(key);
                            handleAsyncResult(future, "GET " + key);
                        }

                        // Throttle traffic to ~1000 ops/sec to avoid memory pressure
                        Thread.sleep(1);

                    } catch (InterruptedException e) {
                        log.info("Traffic generator interrupted");
                        Thread.currentThread().interrupt();
                        break;
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

        public void stopTraffic() {
            if (!trafficStarted.get()) {
                log.info("Traffic not started, nothing to stop");
                return;
            }

            log.info("Stopping continuous traffic...");
            stopTraffic.set(true);

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

    private TestContext setupTest(EndpointType addressType) {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .maintNotificationsConfig(MaintNotificationsConfig.enabled(addressType)).build();
        client.setOptions(options);

        StatefulRedisConnection<String, String> connection = client.connect();

        TestCapture capture = new TestCapture();

        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture);

        String bdbId = String.valueOf(mStandard.getBdbId());

        currentTestContext = new TestContext(client, connection, capture, bdbId, addressType);
        return currentTestContext;
    }

    private void validateEndpointType(String address, EndpointType expectedType, String testDescription) {
        log.info("Validating address '{}' for type {} in {}", address, expectedType, testDescription);
        if (expectedType == EndpointType.NONE) {
            assertThat(address).as("Address should be null with endpoint type 'none' by design").isNull();
            log.info("✓ Address is null with NONE expected type (endpoint type 'none') - this is correct by design");
            return;
        }

        if (expectedType == null) {
            assertThat(address).as("Address should not be null even with null expected type").isNotNull();
            assertThat(address).as("Address should not be empty with null expected type").isNotEmpty();
            log.info("✓ Address '{}' received with null expected type - valid non-null address", address);
            return;
        }

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

            default:
                throw new IllegalArgumentException("Unknown address type: " + expectedType);
        }
    }

    private void performRebindOperation(TestContext context, String testDescription) throws InterruptedException {
        String endpointId = clusterConfig.getFirstEndpointId();
        String policy = "single";

        log.info("=== {} ===", testDescription);
        log.info("Expected address type: {}", context.expectedEndpointType);
        log.info("Starting migrate + moving operation with endpoint-aware node selection...");

        StepVerifier.create(faultClient.triggerMovingNotification(context.bdbId, endpointId, policy, clusterConfig))
                .expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);

        log.info("Waiting for MIGRATED notification...");
        boolean migratedReceived = context.capture.waitForMigratedNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(migratedReceived).as("Should receive MIGRATED notification").isTrue();

        log.info("Waiting for MOVING notification...");
        boolean movingReceived = context.capture.waitForMovingNotification(NOTIFICATION_WAIT_TIMEOUT);
        assertThat(movingReceived).as("Should receive MOVING notification").isTrue();

        String movingNotification = context.capture.getLastMovingNotification();
        assertThat(movingNotification).as("MOVING notification should not be null").isNotNull();

        log.info("Debug - Raw notification with escaped chars: '{}'",
                movingNotification.replace("\n", "\\n").replace("\r", "\\r"));

        Matcher matcher = MOVING_PATTERN.matcher(movingNotification);
        if (matcher.matches()) {
            String sequence = matcher.group(1);
            String ttl = matcher.group(2);
            String addressWithPort = matcher.group(3);

            String newAddress;
            String port;

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
                    // No colon found, treat entire string as address
                    newAddress = addressWithPort;
                    port = null;
                }
            }

            log.info("Parsed MOVING notification - Sequence: {}, TTL: {}, New Address: {}, Port: {}", sequence, ttl, newAddress,
                    port);

            assertThat(Integer.parseInt(ttl)).isGreaterThanOrEqualTo(0);

            if (context.expectedEndpointType != EndpointType.NONE) {
                assertThat(newAddress).isNotEmpty();
                assertThat(port).isNotNull();
                assertThat(Integer.parseInt(port)).isGreaterThan(0);
            }
            validateEndpointType(newAddress, context.expectedEndpointType, testDescription);

        } else {
            log.error("MOVING notification format not recognized: {}", movingNotification);
            assertThat(false).as("MOVING notification should match expected format").isTrue();
        }

        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MIGRATED"))).isTrue();
        assertThat(context.capture.getReceivedNotifications().stream().anyMatch(n -> n.contains("MOVING"))).isTrue();
    }

    private void reconnectionVerification(TestContext context, String testDescription) {
        try {
            log.info("=== Reconnection Verification for {} ===", testDescription);

            // For EndpointType.NONE, we expect to reconnect to the original endpoint, not a new one
            String expectedEndpoint;
            if (context.expectedEndpointType == EndpointType.NONE) {
                String originalUri = mStandard.getEndpoints().get(0); // Original endpoint URI
                // Extract host:port from redis://host:port format
                expectedEndpoint = originalUri.replaceFirst("^redis://", "");
                log.info("Expected reconnection endpoint for NONE type (original endpoint): {}", expectedEndpoint);
            } else {
                // For other types, extract from MOVING notification
                expectedEndpoint = extractEndpointFromMovingNotification(context.capture.getReceivedNotifications());
                log.info("Expected reconnection endpoint from MOVING notification: {}", expectedEndpoint);
            }

            Channel channel = ConnectionTestUtil.getChannel(context.connection);
            SocketAddress currentRemoteAddress = null;

            if (channel != null && channel.isActive()) {
                currentRemoteAddress = channel.remoteAddress();
                log.info("Current connection remote address: {}", currentRemoteAddress);
            } else {
                log.warn("Channel is null or inactive, cannot verify remote address");
            }

            String pingResult = context.connection.sync().ping();
            assertThat(pingResult).isEqualTo("PONG");
            log.info("✓ Connection still responsive after rebind: {}", pingResult);

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

            context.connection.sync().set("test-key", "test-value");
            String getValue = context.connection.sync().get("test-key");
            assertThat(getValue).isEqualTo("test-value");
            log.info("✓ Basic operations work after rebind");

            context.connection.sync().del("test-key");

            context.capture.setReconnectionTested(true);
            log.info("✓ Reconnection verification completed successfully for {}", testDescription);

        } catch (Exception e) {
            log.warn("Reconnection verification failed for {}: {}", testDescription, e.getMessage());
        }
    }

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

    private boolean verifyEndpointMatch(SocketAddress currentRemoteAddress, String expectedEndpoint) {
        if (!(currentRemoteAddress instanceof InetSocketAddress)) {
            return false;
        }

        InetSocketAddress inetAddress = (InetSocketAddress) currentRemoteAddress;
        String currentHost = inetAddress.getHostString();
        int currentPort = inetAddress.getPort();
        String currentEndpoint = currentHost + ":" + currentPort;

        // Direct match
        if (currentEndpoint.equals(expectedEndpoint)) {
            return true;
        }

        // Handle case where expectedEndpoint might have resolved hostname but current has IP
        // Extract port from expected endpoint for comparison
        String[] expectedParts = expectedEndpoint.split(":");
        if (expectedParts.length == 2) {

            int expectedPort = Integer.parseInt(expectedParts[1]);
            if (currentPort == expectedPort) {
                log.info("✓ Port match: current '{}' port {} matches expected '{}' port {}", currentEndpoint, currentPort,
                        expectedEndpoint, expectedPort);
                return true;
            }

        }

        return false;
    }

    private RedisClient createClientWithEndpointType(EndpointType endpointType) {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .maintNotificationsConfig(MaintNotificationsConfig.enabled(endpointType)).build();
        client.setOptions(options);

        return client;
    }

    private RedisClient createClientWithDisabledNotifications() {
        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .maintNotificationsConfig(MaintNotificationsConfig.disabled()).build();
        client.setOptions(options);

        return client;
    }

    private void executeEndpointTypeVerificationTest(EndpointType endpointType, String testDescription)
            throws InterruptedException {
        log.info("test {} started for endpoint type {}", testDescription, endpointType);

        TestContext context = setupTest(endpointType);

        performRebindOperation(context, testDescription);
        reconnectionVerification(context, testDescription);

        // End test phase to prevent capturing cleanup notifications
        context.capture.endTestPhase();

        log.info("test {} ended for endpoint type {}", testDescription, endpointType);
    }

    private void executeNotificationBehaviorTest(boolean notificationsEnabled) throws InterruptedException {
        String testDescription = notificationsEnabled ? "enabled notifications" : "disabled notifications";
        log.info("test {} started", testDescription);

        RedisClient client = notificationsEnabled ? createClientWithEndpointType(EndpointType.EXTERNAL_IP)
                : createClientWithDisabledNotifications();

        StatefulRedisConnection<String, String> connection = client.connect();

        AllNotificationTypesCapture capture = new AllNotificationTypesCapture();

        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture);

        String bdbId = String.valueOf(mStandard.getBdbId());

        log.info("=== Testing {} ===", testDescription);

        try {
            String endpointId = clusterConfig.getFirstEndpointId();
            String policy = "single";

            log.info("Starting comprehensive maintenance operations to trigger all notification types...");

            // This operation will trigger MIGRATING, MIGRATED, and MOVING notifications
            StepVerifier.create(faultClient.triggerMovingNotification(bdbId, endpointId, policy, clusterConfig))
                    .expectNext(true).expectComplete().verify(LONG_OPERATION_TIMEOUT);

            boolean migrationReceived = false;
            boolean failoverReceived = false;

            if (notificationsEnabled) {
                migrationReceived = capture.waitForMigrationNotifications(NOTIFICATION_WAIT_TIMEOUT);
            } else {
                // For disabled notifications, give a brief moment for any unexpected notifications
                // then proceed without waiting the full timeout
                Thread.sleep(Duration.ofSeconds(2).toMillis());
                log.info("Skipped waiting for migration notifications (disabled test)");
            }

            // Trigger additional failover operations to get FAILING_OVER and FAILED_OVER
            clusterConfig = RedisEnterpriseConfig.refreshClusterConfig(faultClient, String.valueOf(mStandard.getBdbId()));
            String nodeId = clusterConfig.getNodeWithMasterShards();

            log.info("Triggering failover operations to get FAILING_OVER and FAILED_OVER notifications...");
            StepVerifier.create(faultClient.triggerShardFailover(bdbId, nodeId, clusterConfig)).expectNext(true)
                    .expectComplete().verify(LONG_OPERATION_TIMEOUT);

            if (notificationsEnabled) {
                failoverReceived = capture.waitForFailoverNotifications(NOTIFICATION_WAIT_TIMEOUT);
            } else {
                // For disabled notifications, give a brief moment for any unexpected notifications
                // then proceed without waiting the full timeout
                Thread.sleep(Duration.ofSeconds(2).toMillis());
                log.info("Skipped waiting for failover notifications (disabled test)");
            }

            capture.endTestPhase();

            log.info("=== Notification Results ===");
            log.info("Total notifications received: {}", capture.getReceivedNotifications().size());
            log.info("MOVING notifications: {}", capture.getMovingCount());
            log.info("MIGRATING notifications: {}", capture.getMigratingCount());
            log.info("MIGRATED notifications: {}", capture.getMigratedCount());
            log.info("FAILING_OVER notifications: {}", capture.getFailingOverCount());
            log.info("FAILED_OVER notifications: {}", capture.getFailedOverCount());

            if (notificationsEnabled) {
                assertThat(capture.getReceivedNotifications())
                        .as("Should receive notifications when maintenance events are enabled").isNotEmpty();

                assertThat(migrationReceived).as("Should receive migration notifications").isTrue();
                assertThat(failoverReceived).as("Should receive failover notifications").isTrue();
                assertThat(capture.getMovingCount()).as("Should receive MOVING notifications").isGreaterThan(0);
                assertThat(capture.getMigratingCount()).as("Should receive MIGRATING notifications").isGreaterThan(0);
                assertThat(capture.getMigratedCount()).as("Should receive MIGRATED notifications").isGreaterThan(0);
                assertThat(capture.getFailingOverCount()).as("Should receive FAILING_OVER notifications").isGreaterThan(0);
                assertThat(capture.getFailedOverCount()).as("Should receive FAILED_OVER notifications").isGreaterThan(0);

                log.info("✓ All expected maintenance notifications received and validated successfully");
            } else {
                assertThat(capture.getReceivedNotifications())
                        .as("Should have no notifications when maintenance events are disabled").isEmpty();

                assertThat(capture.getMovingCount()).as("Should have no MOVING notifications").isZero();
                assertThat(capture.getMigratingCount()).as("Should have no MIGRATING notifications").isZero();
                assertThat(capture.getMigratedCount()).as("Should have no MIGRATED notifications").isZero();
                assertThat(capture.getFailingOverCount()).as("Should have no FAILING_OVER notifications").isZero();
                assertThat(capture.getFailedOverCount()).as("Should have no FAILED_OVER notifications").isZero();

                log.info("✓ Disabled maintenance events correctly prevent notifications");
            }

        } finally {
            // Cleanup operations (same for both enabled and disabled tests)

            clusterConfig = RedisEnterpriseConfig.refreshClusterConfig(faultClient, String.valueOf(mStandard.getBdbId()));
            String nodeId = clusterConfig.getNodeWithMasterShards();

            log.info("performing cluster cleanup operation for failover testing");
            StepVerifier.create(faultClient.triggerShardFailover(bdbId, nodeId, clusterConfig)).expectNext(true)
                    .expectComplete().verify(LONG_OPERATION_TIMEOUT);

            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            if (client != null) {
                client.shutdown();
            }

            log.info("test {} ended", testDescription);
        }
    }

    public static class DualConnectionCapture implements MaintenanceNotificationCapture {

        private final TestCapture firstCapture;

        private final RedisURI uri;

        private final StatefulRedisConnection<String, String> firstConnection;

        private final AtomicReference<TestCapture> secondCapture = new AtomicReference<>();

        private final AtomicReference<RedisClient> secondClient = new AtomicReference<>();

        private final AtomicReference<StatefulRedisConnection<String, String>> secondConnection = new AtomicReference<>();

        private final CountDownLatch secondConnectionMovingLatch = new CountDownLatch(1);

        private final AtomicBoolean testPhaseActive = new AtomicBoolean(true);

        public DualConnectionCapture(TestCapture firstCapture, RedisURI uri, String bdbId,
                StatefulRedisConnection<String, String> firstConnection) {
            this.firstCapture = firstCapture;
            this.uri = uri;
            this.firstConnection = firstConnection;
        }

        @Override
        public void captureNotification(String notification) {
            if (!testPhaseActive.get()) {
                log.debug("Ignoring notification during cleanup phase: {}", notification);
                return;
            }

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
                Channel firstChannel = ConnectionTestUtil.getChannel(firstConnection);
                String actualIpAddress = null;
                int actualPort = -1;

                if (firstChannel != null && firstChannel.remoteAddress() != null) {
                    String remoteAddress = firstChannel.remoteAddress().toString();
                    log.info("First connection remote address: {}", remoteAddress);

                    // Handle different address formats:
                    // Format 1: "/54.74.227.236:12000" (direct IP)
                    // Format 2: "redis-12000.ivo-somefdqn.com/54.74.227.236:12000" (FQDN with resolved
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
                        .maintNotificationsConfig(MaintNotificationsConfig.enabled(EndpointType.EXTERNAL_IP)).build();
                client.setOptions(options);

                StatefulRedisConnection<String, String> connection = client.connect();
                TestCapture capture = new TestCapture() {

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

                MaintenancePushNotificationMonitor.setupMonitoring(connection, capture);

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

        public TestCapture getFirstCapture() {
            return firstCapture;
        }

        public TestCapture getSecondCapture() {
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

    public static class AllNotificationTypesCapture implements MaintenanceNotificationCapture {

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

                if (notification.contains("MOVING")) {
                    try {
                        validateAndParseNotification(notification, NotificationType.MOVING);
                        movingCount.updateAndGet(count -> count + 1);
                        log.info("✓ MOVING notification validated successfully");
                    } catch (Exception e) {
                        log.warn("✗ MOVING notification validation failed: {}", e.getMessage());
                    }
                    if (movingReceived.compareAndSet(false, true)) {
                        migrationLatch.countDown();
                        log.info("First MOVING notification - migration latch count down");
                    }
                } else if (notification.contains("MIGRATING")) {
                    try {
                        validateAndParseNotification(notification, NotificationType.MIGRATING);
                        migratingCount.updateAndGet(count -> count + 1);
                        log.info("✓ MIGRATING notification validated successfully");
                    } catch (Exception e) {
                        log.warn("✗ MIGRATING notification validation failed: {}", e.getMessage());
                    }
                    if (migratingReceived.compareAndSet(false, true)) {
                        migrationLatch.countDown();
                        log.info("First MIGRATING notification - migration latch count down");
                    }
                } else if (notification.contains("MIGRATED")) {
                    try {
                        validateAndParseNotification(notification, NotificationType.MIGRATED);
                        migratedCount.updateAndGet(count -> count + 1);
                        log.info("✓ MIGRATED notification validated successfully");
                    } catch (Exception e) {
                        log.warn("✗ MIGRATED notification validation failed: {}", e.getMessage());
                    }
                    if (migratedReceived.compareAndSet(false, true)) {
                        migrationLatch.countDown();
                        log.info("First MIGRATED notification - migration latch count down");
                    }
                } else if (notification.contains("FAILING_OVER")) {
                    try {
                        validateAndParseNotification(notification, NotificationType.FAILING_OVER);
                        failingOverCount.updateAndGet(count -> count + 1);
                        log.info("✓ FAILING_OVER notification validated successfully");
                    } catch (Exception e) {
                        log.warn("✗ FAILING_OVER notification validation failed: {}", e.getMessage());
                    }
                    if (failingOverReceived.compareAndSet(false, true)) {
                        failoverLatch.countDown();
                        log.info("First FAILING_OVER notification - failover latch count down");
                    }
                } else if (notification.contains("FAILED_OVER")) {
                    try {
                        validateAndParseNotification(notification, NotificationType.FAILED_OVER);
                        failedOverCount.updateAndGet(count -> count + 1);
                        log.info("✓ FAILED_OVER notification validated successfully");
                    } catch (Exception e) {
                        log.warn("✗ FAILED_OVER notification validation failed: {}", e.getMessage());
                    }
                    if (failedOverReceived.compareAndSet(false, true)) {
                        failoverLatch.countDown();
                        log.info("First FAILED_OVER notification - failover latch count down");
                    }
                }
            }
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

    @ParameterizedTest
    @EnumSource(value = EndpointType.class, names = { "EXTERNAL_IP", "EXTERNAL_FQDN", "NONE" })
    @DisplayName("Connection rebind with supported endpoint types")
    public void connectionRebindWithEndpointTypesTest(EndpointType endpointType) throws InterruptedException {
        executeEndpointTypeVerificationTest(endpointType, endpointType + " Rebind Test");
    }

    @Test
    @DisplayName("Traffic resumes correctly after MOVING with async GET/SET operations")
    public void trafficResumesAfterMovingTest() throws InterruptedException {
        log.info("test trafficResumesAfterMovingTest started");
        TestContext context = setupTest(EndpointType.EXTERNAL_IP);

        RedisAsyncCommands<String, String> asyncCommands = context.connection.async();
        ContinuousTrafficGenerator trafficGenerator = new ContinuousTrafficGenerator(asyncCommands);

        log.info("=== Starting traffic before MOVING operation ===");
        trafficGenerator.startTraffic();

        await().atMost(TRAFFIC_AWAIT_TIMEOUT)
                .until(() -> trafficGenerator.getSuccessfulOperations() > TRAFFIC_OPERATION_THRESHOLD);
        long initialSuccessful = trafficGenerator.getSuccessfulOperations();
        long initialFailed = trafficGenerator.getFailedOperations();
        log.info("Initial traffic stats - Successful: {}, Failed: {}", initialSuccessful, initialFailed);

        log.info("=== Performing MOVING operation while traffic is active ===");
        performRebindOperation(context, "Traffic Resumption Test");

        log.info("=== Continuing traffic during maintenance ===");
        long midSuccessful = trafficGenerator.getSuccessfulOperations();
        await().atMost(TRAFFIC_AWAIT_TIMEOUT)
                .until(() -> trafficGenerator.getSuccessfulOperations() > midSuccessful + TRAFFIC_OPERATION_THRESHOLD);

        reconnectionVerification(context, "Traffic Resumption Test");

        log.info("=== Allowing traffic to continue after reconnection ===");
        long postReconnectSuccessful = trafficGenerator.getSuccessfulOperations();
        await().atMost(TRAFFIC_AWAIT_TIMEOUT).until(
                () -> trafficGenerator.getSuccessfulOperations() > postReconnectSuccessful + TRAFFIC_OPERATION_THRESHOLD);

        trafficGenerator.stopTraffic();

        long finalSuccessful = trafficGenerator.getSuccessfulOperations();
        long finalFailed = trafficGenerator.getFailedOperations();
        int totalCommands = trafficGenerator.getTotalCommands();

        log.info("=== Traffic Resumption Test Results ===");
        log.info("Total commands executed: {}", totalCommands);
        log.info("Successful operations: {}", finalSuccessful);
        log.info("Failed operations: {}", finalFailed);
        log.info("Success rate: {:.2f}%", (double) finalSuccessful / totalCommands * 100);

        assertThat(totalCommands).as("Should have executed traffic commands").isGreaterThan(0);
        assertThat(finalSuccessful).as("Should have successful operations after MOVING").isGreaterThan(initialSuccessful);

        double failureRate = (double) finalFailed / totalCommands;
        assertThat(failureRate).as("Failure rate should be zero").isZero();

        log.info("✓ Traffic resumed successfully after MOVING operation");

        context.capture.endTestPhase();

        log.info("test trafficResumesAfterMovingTest ended");
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @DisplayName("Maintenance notifications enabled/disabled behavior")
    public void maintenanceNotificationsBehaviorTest(boolean notificationsEnabled) throws InterruptedException {
        executeNotificationBehaviorTest(notificationsEnabled);
    }

    @Test
    @DisplayName("Connection handed off to new endpoint with External IP - Dual Connection Test")
    public void newConnectionDuringRebindAfterMovingTest() throws InterruptedException {
        log.info("test newConnectionDuringRebindAfterMovingTest started");

        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient firstClient = RedisClient.create(uri);
        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .maintNotificationsConfig(MaintNotificationsConfig.enabled(EndpointType.EXTERNAL_IP)).build();
        firstClient.setOptions(options);

        StatefulRedisConnection<String, String> firstConnection = firstClient.connect();
        TestCapture firstCapture = new TestCapture();
        String bdbId = String.valueOf(mStandard.getBdbId());

        DualConnectionCapture dualCapture = new DualConnectionCapture(firstCapture, uri, bdbId, firstConnection);

        MaintenancePushNotificationMonitor.setupMonitoring(firstConnection, dualCapture);

        try {
            performRebindOperation(new TestContext(firstClient, firstConnection, firstCapture, bdbId, EndpointType.EXTERNAL_IP),
                    "Dual Connection External IP Rebind Test");

            log.info("Waiting for second connection to receive MOVING notification...");
            boolean secondMovingReceived = dualCapture.waitForSecondConnectionMoving(NOTIFICATION_WAIT_TIMEOUT);
            assertThat(secondMovingReceived).as("Second connection should receive MOVING notification").isTrue();

            assertThat(dualCapture.getFirstCapture().getLastMovingNotification())
                    .as("First connection should have MOVING notification").isNotNull();
            assertThat(dualCapture.getSecondCapture().getLastMovingNotification())
                    .as("Second connection should have MOVING notification").isNotNull();

            log.info("Both connections received MOVING notifications successfully");

            reconnectionVerification(new TestContext(firstClient, firstConnection, dualCapture.getFirstCapture(), bdbId,
                    EndpointType.EXTERNAL_IP), "First Connection - Dual Connection External IP Rebind Test");

            if (dualCapture.getSecondConnection() != null) {
                reconnectionVerification(
                        new TestContext(dualCapture.getSecondClient(), dualCapture.getSecondConnection(),
                                dualCapture.getSecondCapture(), bdbId, EndpointType.EXTERNAL_IP),
                        "Second Connection - Dual Connection External IP Rebind Test");
            }

            dualCapture.endTestPhase();

            log.info("test newConnectionDuringRebindAfterMovingTest ended");

        } finally {
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
    @DisplayName("Combined BLPOP timeout unblock during MOVING with connection closure and EventBus monitoring")
    public void connectionHandoffDuringMovingWithEventBusMonitoringTest() throws InterruptedException {
        log.info("test connectionHandoffDuringMovingWithEventBusMonitoringTest started");

        ConnectionEventBusMonitoringUtil eventBusMonitor = new ConnectionEventBusMonitoringUtil();

        RedisURI uri = RedisURI.builder(RedisURI.create(mStandard.getEndpoints().get(0)))
                .withAuthentication(mStandard.getUsername(), mStandard.getPassword()).build();

        RedisClient client = RedisClient.create(uri);

        ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .maintNotificationsConfig(MaintNotificationsConfig.enabled(EndpointType.EXTERNAL_IP)).build();
        client.setOptions(options);

        eventBusMonitor.setupEventBusMonitoring(client);

        StatefulRedisConnection<String, String> connection = client.connect();

        RedisClient secondClient = RedisClient.create(uri);
        StatefulRedisConnection<String, String> secondConnection = secondClient.connect();

        log.info("Clearing BLPOP queue from previous test runs...");
        Long deletedKeys = connection.sync().del(CombinedBlpopAndEventBusCapture.BLPOP_QUEUE_KEY);
        log.info("Deleted {} keys from BLPOP queue", deletedKeys);

        CombinedBlpopAndEventBusCapture capture = new CombinedBlpopAndEventBusCapture(connection, secondConnection);

        MaintenancePushNotificationMonitor.setupMonitoring(connection, capture);

        try {

            String pingResult = connection.sync().ping();
            assertThat(pingResult).isEqualTo("PONG");
            String secondPingResult = secondConnection.sync().ping();
            assertThat(secondPingResult).isEqualTo("PONG");

            String initialChannelId = eventBusMonitor.getCurrentChannelId();
            Channel initialChannel = ConnectionTestUtil.getChannel(connection);

            log.info("Initial connection established - channelId: {}", initialChannelId);

            log.info("Initial channel state - active: {}, open: {}, registered: {}", initialChannel.isActive(),
                    initialChannel.isOpen(), initialChannel.isRegistered());

            eventBusMonitor.prepareForConnectionTransition();

            String bdbId = String.valueOf(mStandard.getBdbId());
            String endpointId = clusterConfig.getFirstEndpointId();
            String policy = "single";

            log.info("Starting migrate + moving operation with endpoint-aware node selection...");

            StepVerifier.create(faultClient.triggerMovingNotification(bdbId, endpointId, policy, clusterConfig))
                    .expectNext(true).expectComplete().verify(Duration.ofMinutes(3));

            log.info("Migrate + moving operation completed, waiting for connection events and BLPOP completion...");

            boolean blpopCompleted = capture.waitForBlpopCompletion(Duration.ofMinutes(2));
            assertThat(blpopCompleted).as("BLPOP should be unblocked by LPUSH during MOVING").isTrue();

            boolean eventsReceived = eventBusMonitor.waitForConnectionTransition(Duration.ofSeconds(30));
            assertThat(eventsReceived)
                    .as("Should receive connection transition events (DisconnectedEvent + ConnectionDeactivatedEvent)")
                    .isTrue();

            ConnectionEventBusMonitoringUtil.ConnectionAnalysisResult result = eventBusMonitor
                    .analyzeConnectionClosure(initialChannelId, initialChannel);

            // VALIDATIONS: BLPOP unblock functionality
            assertThat(capture.isBlpopCompleted()).as("BLPOP should have been unblocked during MOVING").isTrue();
            assertThat(capture.getBlpopResult()).as("BLPOP should have received the unblocking value").isNotNull();
            assertThat(capture.isStackVerified()).as("Command stack verification should have been performed").isTrue();

            // VALIDATIONS: Connection properly closed based on EventBus monitoring
            assertThat(result.wasDisconnected()).as("Old connection should have been disconnected (TCP level)").isTrue();
            assertThat(result.wasDeactivated())
                    .as("Old connection should have been deactivated (logical level) - this is the key signal").isTrue();
            assertThat(result.isEventBusCleanup())
                    .as("EventBus should indicate proper cleanup (both disconnected and deactivated)").isTrue();

            if (initialChannel != null) {
                assertThat(result.isNettyCleanup())
                        .as("Netty channel should be properly cleaned up (inactive, closed, unregistered)").isTrue();
            }

            assertThat(result.isConnectionHandedOff()).as("Connection should have been handed off to new channel").isTrue();
            assertThat(result.isFullyCleanedUpViaEventBus())
                    .as("Connection should be fully cleaned up based on EventBus monitoring").isTrue();

            Channel newChannel = ConnectionTestUtil.getChannel(connection);
            if (newChannel != null) {
                assertThat(newChannel.isActive()).as("New channel should be active after MOVING reconnection").isTrue();
                assertThat(newChannel.isRegistered()).as("New channel should be registered after MOVING reconnection").isTrue();
                log.info("✓ New channel state verified - active: {}, registered: {}", newChannel.isActive(),
                        newChannel.isRegistered());
            }

            String testKey = "combined-test-" + System.currentTimeMillis();
            String testValue = "test-value";

            connection.sync().set(testKey, testValue);
            String retrievedValue = connection.sync().get(testKey);

            assertThat(retrievedValue).isEqualTo(testValue);
            assertThat(connection.isOpen()).isTrue();

            log.info("✓ New connection is fully functional after handoff");
            log.info("✓ BLPOP unblock during MOVING test passed");
            log.info("✓ Connection closure validation passed - EventBus monitoring indicates proper cleanup");

        } finally {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            if (client != null) {
                client.shutdown();
            }
            if (secondConnection != null && secondConnection.isOpen()) {
                secondConnection.close();
            }
            if (secondClient != null) {
                secondClient.shutdown();
            }
            eventBusMonitor.stopMonitoring();
        }

        log.info("test connectionHandoffDuringMovingWithEventBusMonitoringTest ended");
    }

    public static class CombinedBlpopAndEventBusCapture implements MaintenanceNotificationCapture {

        private final StatefulRedisConnection<String, String> mainConnection;

        private final StatefulRedisConnection<String, String> secondConnection;

        private final AtomicReference<String> blpopResult = new AtomicReference<>();

        private final AtomicBoolean blpopCompleted = new AtomicBoolean(false);

        private final AtomicBoolean stackVerified = new AtomicBoolean(false);

        private final AtomicInteger stackSizeBeforeVerification = new AtomicInteger(-1);

        private final CountDownLatch blpopCompletionLatch = new CountDownLatch(1);

        private final AtomicBoolean testPhaseActive = new AtomicBoolean(true);

        public static final String BLPOP_QUEUE_KEY = "blpop-unblock-test-queue";

        private static final String UNBLOCK_VALUE = "unblock-value-" + System.currentTimeMillis();

        public CombinedBlpopAndEventBusCapture(StatefulRedisConnection<String, String> mainConnection,
                StatefulRedisConnection<String, String> secondConnection) {
            this.mainConnection = mainConnection;
            this.secondConnection = secondConnection;
        }

        @Override
        public void captureNotification(String notification) {
            if (!testPhaseActive.get()) {
                log.debug("Ignoring notification during cleanup phase: {}", notification);
                return;
            }

            log.info("Combined capture received notification: {}", notification);

            if (notification.contains("MIGRATED")) {
                log.info("MIGRATED notification received - starting BLPOP with 60-second timeout");
                startBlpopWithTimeout();
            } else if (notification.contains("MOVING")) {
                log.info("MOVING notification received - performing command stack verification and LPUSH unblock");
                verifyCommandStackDuringMoving();
                unblockBlpop();
            }
        }

        private void startBlpopWithTimeout() {
            CompletableFuture.runAsync(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    log.info("Starting BLPOP with 60-second timeout on key: {}", BLPOP_QUEUE_KEY);

                    RedisFuture<KeyValue<String, String>> future = mainConnection.async().blpop(60, BLPOP_QUEUE_KEY);
                    KeyValue<String, String> result = future.get();

                    long duration = System.currentTimeMillis() - startTime;

                    if (result != null) {
                        blpopResult.set(result.getValue());
                        log.info("BLPOP completed successfully in {}ms with value: {}", duration, result.getValue());
                    } else {
                        log.info("BLPOP completed in {}ms but returned null (timeout)", duration);
                    }

                    blpopCompleted.set(true);
                    blpopCompletionLatch.countDown();

                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("BLPOP failed after {}ms: {}", duration, e.getMessage());
                    blpopCompleted.set(true);
                    blpopCompletionLatch.countDown();
                }
            });
        }

        private void verifyCommandStackDuringMoving() {
            try {
                log.info("Verifying command stack during MOVING...");

                if (mainConnection != null && mainConnection.isOpen()) {
                    ConnectionTestUtil.StackVerificationResult result = ConnectionTestUtil
                            .verifyConnectionAndStackState(mainConnection, "MOVING phase");

                    stackSizeBeforeVerification.set(result.getStackSize());

                    assertThat(result.getStackSize()).as("Command stack should have pending commands during MOVING")
                            .isGreaterThan(0);

                    assertThat(result.isChannelActive()).as("Channel should be active during MOVING verification").isTrue();
                    assertThat(result.isChannelRegistered()).as("Channel should be registered during MOVING verification")
                            .isTrue();

                    stackVerified.set(true);
                }
            } catch (Exception e) {
                log.warn("Failed to verify command stack during MOVING: {}", e.getMessage());
                stackVerified.set(false);
            }
        }

        private void unblockBlpop() {
            try {
                log.info("Sending LPUSH via second connection to unblock BLPOP...");
                Long pushResult = secondConnection.sync().lpush(BLPOP_QUEUE_KEY, UNBLOCK_VALUE);
                log.info("LPUSH completed, result: {}", pushResult);
            } catch (Exception e) {
                log.warn("Failed to unblock BLPOP: {}", e.getMessage());
            }
        }

        public boolean waitForBlpopCompletion(Duration timeout) throws InterruptedException {
            return blpopCompletionLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public boolean isBlpopCompleted() {
            return blpopCompleted.get();
        }

        public String getBlpopResult() {
            return blpopResult.get();
        }

        public boolean isStackVerified() {
            return stackVerified.get();
        }

        public int getStackSizeBeforeVerification() {
            return stackSizeBeforeVerification.get();
        }

        public void endTestPhase() {
            testPhaseActive.set(false);
            log.info("Combined capture test phase ended - notifications will be ignored during cleanup");
        }

    }

}
