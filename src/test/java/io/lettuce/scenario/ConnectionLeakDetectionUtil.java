package io.lettuce.scenario;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ConnectedEvent;
import io.lettuce.core.event.connection.ConnectionActivatedEvent;
import io.lettuce.core.event.connection.ConnectionDeactivatedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Utility for detecting connection closure and memory leaks using EventBus monitoring and Netty channel state. This provides a
 * practical way to verify connections are properly cleaned up without relying on internal APIs.
 */
public class ConnectionLeakDetectionUtil {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(ConnectionLeakDetectionUtil.class);

    private final Set<String> connectedChannels = ConcurrentHashMap.newKeySet();

    private final Set<String> disconnectedChannels = ConcurrentHashMap.newKeySet();

    private final Set<String> activatedChannels = ConcurrentHashMap.newKeySet();

    private final Set<String> deactivatedChannels = ConcurrentHashMap.newKeySet();

    private final AtomicReference<String> currentChannelId = new AtomicReference<>();

    private final AtomicBoolean monitoringActive = new AtomicBoolean(true);

    private CountDownLatch connectionTransitionLatch;

    /**
     * Setup EventBus monitoring for connection events. Call this BEFORE creating connections.
     */
    public void setupEventBusMonitoring(RedisClient client) {
        EventBus eventBus = client.getResources().eventBus();

        eventBus.get().subscribe(event -> {
            if (!monitoringActive.get())
                return;

            if (event instanceof ConnectedEvent) {
                ConnectedEvent connected = (ConnectedEvent) event;
                String channelId = getChannelIdFromEvent(connected);
                connectedChannels.add(channelId);
                log.info("EventBus: Channel connected - {}", channelId);
            }

            if (event instanceof ConnectionActivatedEvent) {
                ConnectionActivatedEvent activated = (ConnectionActivatedEvent) event;
                String channelId = getChannelIdFromEvent(activated);
                activatedChannels.add(channelId);
                currentChannelId.set(channelId);
                log.info("EventBus: Connection activated - {}", channelId);
            }

            if (event instanceof DisconnectedEvent) {
                DisconnectedEvent disconnected = (DisconnectedEvent) event;
                String channelId = getChannelIdFromEvent(disconnected);
                disconnectedChannels.add(channelId);
                if (connectionTransitionLatch != null) {
                    connectionTransitionLatch.countDown();
                }
                log.info("EventBus: Channel disconnected - {}", channelId);
            }

            if (event instanceof ConnectionDeactivatedEvent) {
                ConnectionDeactivatedEvent deactivated = (ConnectionDeactivatedEvent) event;
                String channelId = getChannelIdFromEvent(deactivated);
                deactivatedChannels.add(channelId);
                if (connectionTransitionLatch != null) {
                    connectionTransitionLatch.countDown();
                }
                log.info("EventBus: Connection deactivated - {}", channelId);
            }
        });

        log.info("EventBus monitoring setup completed");
    }

    /**
     * Extract channel ID from connection event using reflection (since getChannelId() is package-private).
     */
    private String getChannelIdFromEvent(Object event) {
        try {
            Method getChannelIdMethod = event.getClass().getSuperclass().getDeclaredMethod("getChannelId");
            getChannelIdMethod.setAccessible(true);
            String channelId = (String) getChannelIdMethod.invoke(event);
            return channelId != null ? channelId : event.toString();
        } catch (Exception e) {
            // Fallback to using socket address as identifier
            if (event instanceof ConnectedEvent) {
                return "connected-" + ((ConnectedEvent) event).remoteAddress().toString();
            } else if (event instanceof DisconnectedEvent) {
                return "disconnected-" + ((DisconnectedEvent) event).remoteAddress().toString();
            } else {
                return event.getClass().getSimpleName() + "-" + System.currentTimeMillis();
            }
        }
    }

    /**
     * Prepare to wait for connection transition events (disconnect + deactivate). Call this before performing operations that
     * will cause connection handoff.
     */
    public void prepareForConnectionTransition() {
        connectionTransitionLatch = new CountDownLatch(2); // Disconnect + Deactivate
    }

    /**
     * Wait for connection transition events to complete.
     */
    public boolean waitForConnectionTransition(Duration timeout) throws InterruptedException {
        if (connectionTransitionLatch == null) {
            throw new IllegalStateException("Must call prepareForConnectionTransition() first");
        }
        return connectionTransitionLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Get the current active channel ID.
     */
    public String getCurrentChannelId() {
        return currentChannelId.get();
    }

    /**
     * Check if a channel was properly disconnected (TCP level).
     */
    public boolean wasChannelDisconnected(String channelId) {
        return disconnectedChannels.contains(channelId);
    }

    /**
     * Check if a connection was properly deactivated (logical level).
     */
    public boolean wasChannelDeactivated(String channelId) {
        return deactivatedChannels.contains(channelId);
    }

    /**
     * Check if connection is properly closed and not leaking memory. This is the primary method to verify no memory leaks.
     */
    public boolean isConnectionProperlyClosedAndNotLeaking(String channelId) {
        return wasChannelDisconnected(channelId) && wasChannelDeactivated(channelId);
    }

    /**
     * Verify Netty channel is properly cleaned up.
     */
    public boolean isNettyChannelCleanedUp(Channel channel) {
        if (channel == null)
            return true;

        boolean isCleanedUp = !channel.isActive() && !channel.isOpen() && !channel.isRegistered();

        log.info("Netty channel cleanup status - Active: {}, Open: {}, Registered: {}, CleanedUp: {}", channel.isActive(),
                channel.isOpen(), channel.isRegistered(), isCleanedUp);

        return isCleanedUp;
    }

    /**
     * Complete connection closure and memory leak analysis.
     */
    public ConnectionAnalysisResult analyzeConnectionClosure(String initialChannelId, Channel initialChannel) {
        log.info("=== Connection Closure Analysis ===");

        // EventBus level indicators
        boolean wasDisconnected = wasChannelDisconnected(initialChannelId);
        boolean wasDeactivated = wasChannelDeactivated(initialChannelId);
        boolean eventBusCleanup = isConnectionProperlyClosedAndNotLeaking(initialChannelId);

        // Netty channel level indicators
        boolean nettyCleanup = isNettyChannelCleanedUp(initialChannel);

        // Connection handoff verification
        String currentChannelId = getCurrentChannelId();
        boolean connectionHandedOff = !initialChannelId.equals(currentChannelId);

        log.info("EventBus indicators - Disconnected: {}, Deactivated: {}, Cleanup: {}", wasDisconnected, wasDeactivated,
                eventBusCleanup);
        log.info("Netty cleanup: {}", nettyCleanup);
        log.info("Connection handoff - Initial: {}, Current: {}, Handed off: {}", initialChannelId, currentChannelId,
                connectionHandedOff);

        ConnectionAnalysisResult result = new ConnectionAnalysisResult(wasDisconnected, wasDeactivated, eventBusCleanup,
                nettyCleanup, connectionHandedOff, initialChannelId, currentChannelId);

        if (result.isFullyCleanedUpWithoutLeaks()) {
            log.info("✓ Connection closure validation passed - no memory leaks detected");
        } else {
            log.warn("⚠ Potential memory leak detected - connection not fully cleaned up");
        }

        return result;
    }

    /**
     * Stop monitoring events.
     */
    public void stopMonitoring() {
        monitoringActive.set(false);
    }

    /**
     * Results of connection closure analysis.
     */
    public static class ConnectionAnalysisResult {

        private final boolean wasDisconnected;

        private final boolean wasDeactivated;

        private final boolean eventBusCleanup;

        private final boolean nettyCleanup;

        private final boolean connectionHandedOff;

        private final String initialChannelId;

        private final String currentChannelId;

        public ConnectionAnalysisResult(boolean wasDisconnected, boolean wasDeactivated, boolean eventBusCleanup,
                boolean nettyCleanup, boolean connectionHandedOff, String initialChannelId, String currentChannelId) {
            this.wasDisconnected = wasDisconnected;
            this.wasDeactivated = wasDeactivated;
            this.eventBusCleanup = eventBusCleanup;
            this.nettyCleanup = nettyCleanup;
            this.connectionHandedOff = connectionHandedOff;
            this.initialChannelId = initialChannelId;
            this.currentChannelId = currentChannelId;
        }

        /**
         * Primary indicator: connection is fully cleaned up without memory leaks.
         */
        public boolean isFullyCleanedUpWithoutLeaks() {
            return eventBusCleanup && nettyCleanup && connectionHandedOff;
        }

        public boolean wasDisconnected() {
            return wasDisconnected;
        }

        public boolean wasDeactivated() {
            return wasDeactivated;
        }

        public boolean isEventBusCleanup() {
            return eventBusCleanup;
        }

        public boolean isNettyCleanup() {
            return nettyCleanup;
        }

        public boolean isConnectionHandedOff() {
            return connectionHandedOff;
        }

        public String getInitialChannelId() {
            return initialChannelId;
        }

        public String getCurrentChannelId() {
            return currentChannelId;
        }

    }

    /**
     * Helper method to extract channel from connection using reflection. This is needed because the channel is not directly
     * accessible via public APIs.
     */
    public static Channel getChannelFromConnection(StatefulRedisConnection<String, String> connection) {
        try {
            return io.lettuce.test.ConnectionTestUtil.getChannel(connection);
        } catch (Exception e) {
            log.warn("Could not extract channel from connection: {}", e.getMessage());
            return null;
        }
    }

}
