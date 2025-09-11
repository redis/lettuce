package io.lettuce.scenario;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Utility class for setting up Redis Enterprise maintenance event push notification monitoring. Provides a reusable way to
 * monitor RESP3 push notifications for maintenance events like MOVING, MIGRATING, MIGRATED, FAILING_OVER, and FAILED_OVER.
 */
public class MaintenancePushNotificationMonitor {

    private static final Logger log = LoggerFactory.getLogger(MaintenancePushNotificationMonitor.class);

    // Default timeout constants
    private static final Duration DEFAULT_MONITORING_TIMEOUT = Duration.ofMinutes(2);

    private static final Duration DEFAULT_PING_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration DEFAULT_PING_INTERVAL = Duration.ofSeconds(5);

    /**
     * Sets up push notification monitoring with default timeouts
     * 
     * @param connection the Redis connection to monitor
     * @param capture the capture implementation to handle notifications
     * @param <T> the type of capture that implements MaintenanceNotificationCapture
     */
    public static <T extends MaintenanceNotificationCapture> void setupMonitoring(
            StatefulRedisConnection<String, String> connection, T capture) {
        setupMonitoring(connection, capture, DEFAULT_MONITORING_TIMEOUT, DEFAULT_PING_TIMEOUT, DEFAULT_PING_INTERVAL);
    }

    /**
     * Sets up push notification monitoring with custom timeouts
     * 
     * @param connection the Redis connection to monitor
     * @param capture the capture implementation to handle notifications
     * @param monitoringTimeout how long to run periodic ping monitoring
     * @param pingTimeout timeout for individual ping operations
     * @param pingInterval interval between ping operations
     * @param <T> the type of capture that implements MaintenanceNotificationCapture
     */
    public static <T extends MaintenanceNotificationCapture> void setupMonitoring(
            StatefulRedisConnection<String, String> connection, T capture, Duration monitoringTimeout, Duration pingTimeout,
            Duration pingInterval) {

        log.info("Setting up push notification monitoring for maintenance events...");

        // Create and register the push listener
        PushListener maintenanceListener = new MaintenanceEventPushListener<>(capture);
        connection.addListener(maintenanceListener);
        log.info("PushListener registered for maintenance event monitoring");

        // Start periodic ping monitoring to encourage push messages
        startPeriodicPingMonitoring(connection, monitoringTimeout, pingTimeout, pingInterval);

        log.info("Push notification monitoring active");
    }

    /**
     * Starts periodic ping monitoring to trigger push notifications
     */
    private static void startPeriodicPingMonitoring(StatefulRedisConnection<String, String> connection,
            Duration monitoringTimeout, Duration pingTimeout, Duration pingInterval) {

        RedisReactiveCommands<String, String> reactive = connection.reactive();

        // Calculate number of pings based on monitoring timeout and interval
        long totalPings = monitoringTimeout.toMillis() / pingInterval.toMillis();

        // Start monitoring - the Disposable is not stored as it runs asynchronously
        // Use Flux.interval(Duration.ZERO, pingInterval) to start immediately without initial delay
        Flux.interval(Duration.ZERO, pingInterval).take(totalPings)
                .doOnNext(i -> log.info("Ping #{} - Activity to trigger push messages", i))
                .flatMap(i -> reactive.ping().timeout(pingTimeout)
                        .doOnNext(response -> log.info("Ping #{} response: '{}'", i, response)).onErrorResume(e -> {
                            log.debug("Ping #{} failed, continuing: {}", i, e.getMessage());
                            return Mono.empty();
                        }))
                .doOnComplete(() -> log.info("Push notification monitoring completed")).subscribe();
    }

    /**
     * Internal PushListener implementation that handles all maintenance event types
     */
    private static class MaintenanceEventPushListener<T extends MaintenanceNotificationCapture> implements PushListener {

        private final T capture;

        public MaintenanceEventPushListener(T capture) {
            this.capture = capture;
        }

        @Override
        public void onPushMessage(PushMessage message) {
            String messageType = message.getType();
            log.info("Push message received: type='{}'", messageType);

            List<Object> content = message.getContent();
            log.info("Push message content: {}", content);

            try {
                switch (messageType) {
                    case "MOVING":
                        log.info("MOVING push message captured");
                        handleMovingMessage(content, capture);
                        break;
                    case "MIGRATING":
                        log.info("MIGRATING push message captured");
                        handleMigratingMessage(content, capture);
                        break;
                    case "MIGRATED":
                        log.info("MIGRATED push message captured");
                        handleMigratedMessage(content, capture);
                        break;
                    case "FAILING_OVER":
                        log.info("FAILING_OVER push message captured");
                        handleFailingOverMessage(content, capture);
                        break;
                    case "FAILED_OVER":
                        log.info("FAILED_OVER push message captured");
                        handleFailedOverMessage(content, capture);
                        break;
                    default:
                        log.info("Other push message: type={}, content={}", messageType, content);
                        break;
                }
            } catch (Exception e) {
                log.error("Error handling push message type '{}': {}", messageType, e.getMessage(), e);
            }
        }

        private void handleMovingMessage(List<Object> content, T capture) {
            String stateName = decodeByteBuffer(content.get(0));
            String seqNumber = decodeByteBuffer(content.get(1));
            String timeToLive = decodeByteBuffer(content.get(2));
            String newAddress = decodeByteBuffer(content.get(3));
            log.info("state name: {}, seq number: {}, time to live: {}, new address: {}", stateName, seqNumber, timeToLive,
                    newAddress);
            String resp3Format = String.format(">4\r\n%s\r\n:%s\r\n:%s\r\n%s\r\n", stateName, seqNumber, timeToLive,
                    newAddress != null ? newAddress : "");
            capture.captureNotification(resp3Format);

        }

        private void handleMigratingMessage(List<Object> content, T capture) {
            String stateName = decodeByteBuffer(content.get(0));
            String seqNumber = decodeByteBuffer(content.get(1));
            String timeToLive = decodeByteBuffer(content.get(2));
            String slotNumber = decodeByteBuffer(content.get(3));
            log.info("state name: {}, seq number: {}, time to live: {}, slot number: {}", stateName, seqNumber, timeToLive,
                    slotNumber);
            String resp3Format = String.format(">4\r\n%s\r\n:%s\r\n:%s\r\n:%s\r\n", stateName, seqNumber, timeToLive,
                    slotNumber);
            capture.captureNotification(resp3Format);
        }

        private void handleMigratedMessage(List<Object> content, T capture) {
            String stateName = decodeByteBuffer(content.get(0));
            String seqNumber = decodeByteBuffer(content.get(1));
            String slotNumber = decodeByteBuffer(content.get(2));
            log.info("state name: {}, seq number: {}, slot number: {}", stateName, seqNumber, slotNumber);
            String resp3Format = String.format(">3\r\n%s\r\n:%s\r\n:%s\r\n", stateName, seqNumber, slotNumber);
            capture.captureNotification(resp3Format);
        }

        private void handleFailingOverMessage(List<Object> content, T capture) {
            String stateName = decodeByteBuffer(content.get(0));
            String seqNumber = decodeByteBuffer(content.get(1));
            String timeToLive = decodeByteBuffer(content.get(2));
            String slotNumber = decodeByteBuffer(content.get(3));
            log.info("state name: {}, seq number: {}, time to live: {}, slot number: {}", stateName, seqNumber, timeToLive,
                    slotNumber);
            String resp3Format = String.format(">4\r\n%s\r\n:%s\r\n:%s\r\n:%s\r\n", stateName, seqNumber, timeToLive,
                    slotNumber);
            capture.captureNotification(resp3Format);
        }

        private void handleFailedOverMessage(List<Object> content, T capture) {
            String stateName = decodeByteBuffer(content.get(0));
            String seqNumber = decodeByteBuffer(content.get(1));
            String slotNumber = decodeByteBuffer(content.get(2));
            log.info("state name: {}, seq number: {}, slot number: {}", stateName, seqNumber, slotNumber);
            String resp3Format = String.format(">3\r\n%s\r\n:%s\r\n:%s\r\n", stateName, seqNumber, slotNumber);
            capture.captureNotification(resp3Format);
        }

        private String decodeByteBuffer(Object obj) {
            if (obj == null) {
                return null;
            } else if (obj instanceof ByteBuffer) {
                ByteBuffer buffer = (ByteBuffer) obj;
                return io.lettuce.core.codec.StringCodec.UTF8.decodeKey(buffer);
            } else {
                return obj.toString();
            }
        }

    }

}
