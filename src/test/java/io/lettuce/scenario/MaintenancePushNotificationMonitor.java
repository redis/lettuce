package io.lettuce.scenario;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;

/**
 * Utility class for setting up Redis Enterprise maintenance event push notification monitoring. Provides a reusable way to
 * monitor RESP3 push notifications for maintenance events like MOVING, MIGRATING, MIGRATED, FAILING_OVER, and FAILED_OVER.
 */
public class MaintenancePushNotificationMonitor {

    private static final Logger log = LoggerFactory.getLogger(MaintenancePushNotificationMonitor.class);

    /**
     * Sets up push notification monitoring. Lettuce automatically observes the input buffer and parses PUSH notifications.
     * 
     * @param connection the Redis connection to monitor
     * @param capture the capture implementation to handle notifications
     * @param <T> the type of capture that implements MaintenanceNotificationCapture
     */
    public static <T extends MaintenanceNotificationCapture> void setupMonitoring(
            StatefulRedisConnection<String, String> connection, T capture) {

        log.info("Setting up push notification monitoring for maintenance events...");

        // Create and register the push listener
        PushListener maintenanceListener = new MaintenanceEventPushListener<>(capture);
        connection.addListener(maintenanceListener);
        log.info("PushListener registered for maintenance event monitoring");

        // No periodic ping monitoring needed - Lettuce automatically handles PUSH notifications
        log.info("Push notification monitoring active - Lettuce will automatically parse PUSH notifications");
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
