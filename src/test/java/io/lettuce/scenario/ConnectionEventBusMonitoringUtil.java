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
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.connection.ConnectedEvent;
import io.lettuce.core.event.connection.ConnectionActivatedEvent;
import io.lettuce.core.event.connection.ConnectionDeactivatedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class ConnectionEventBusMonitoringUtil {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(ConnectionEventBusMonitoringUtil.class);

    private final Set<String> connectedChannels = ConcurrentHashMap.newKeySet();

    private final Set<String> disconnectedChannels = ConcurrentHashMap.newKeySet();

    private final Set<String> activatedChannels = ConcurrentHashMap.newKeySet();

    private final Set<String> deactivatedChannels = ConcurrentHashMap.newKeySet();

    private final AtomicReference<String> currentChannelId = new AtomicReference<>();

    private final AtomicBoolean monitoringActive = new AtomicBoolean(true);

    private CountDownLatch connectionTransitionLatch;

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

    private String getChannelIdFromEvent(Object event) {
        try {
            Method getChannelIdMethod = event.getClass().getSuperclass().getDeclaredMethod("getChannelId");
            getChannelIdMethod.setAccessible(true);
            String channelId = (String) getChannelIdMethod.invoke(event);
            return channelId != null ? channelId : event.toString();
        } catch (Exception e) {
            if (event instanceof ConnectedEvent) {
                return "connected-" + ((ConnectedEvent) event).remoteAddress().toString();
            } else if (event instanceof DisconnectedEvent) {
                return "disconnected-" + ((DisconnectedEvent) event).remoteAddress().toString();
            } else {
                return event.getClass().getSimpleName() + "-" + System.currentTimeMillis();
            }
        }
    }

    public void prepareForConnectionTransition() {
        connectionTransitionLatch = new CountDownLatch(2); // Disconnect + Deactivate
    }

    public boolean waitForConnectionTransition(Duration timeout) throws InterruptedException {
        if (connectionTransitionLatch == null) {
            throw new IllegalStateException("Must call prepareForConnectionTransition() first");
        }
        return connectionTransitionLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public String getCurrentChannelId() {
        return currentChannelId.get();
    }

    public boolean wasChannelDisconnected(String channelId) {
        return disconnectedChannels.contains(channelId);
    }

    public boolean wasChannelDeactivated(String channelId) {
        return deactivatedChannels.contains(channelId);
    }

    public boolean isConnectionProperlyClosedViaEventBus(String channelId) {
        return wasChannelDisconnected(channelId) && wasChannelDeactivated(channelId);
    }

    public boolean isNettyChannelCleanedUp(Channel channel) {
        if (channel == null)
            return true;

        boolean isCleanedUp = !channel.isActive() && !channel.isOpen() && !channel.isRegistered();

        log.info("Netty channel cleanup status - Active: {}, Open: {}, Registered: {}, CleanedUp: {}", channel.isActive(),
                channel.isOpen(), channel.isRegistered(), isCleanedUp);

        return isCleanedUp;
    }

    public ConnectionAnalysisResult analyzeConnectionClosure(String initialChannelId, Channel initialChannel) {
        log.info("=== Connection Closure Analysis ===");

        boolean wasDisconnected = wasChannelDisconnected(initialChannelId);
        boolean wasDeactivated = wasChannelDeactivated(initialChannelId);
        boolean eventBusCleanup = isConnectionProperlyClosedViaEventBus(initialChannelId);

        boolean nettyCleanup = isNettyChannelCleanedUp(initialChannel);

        String currentChannelId = getCurrentChannelId();
        boolean connectionHandedOff = !initialChannelId.equals(currentChannelId);

        log.info("EventBus indicators - Disconnected: {}, Deactivated: {}, Cleanup: {}", wasDisconnected, wasDeactivated,
                eventBusCleanup);
        log.info("Netty cleanup: {}", nettyCleanup);
        log.info("Connection handoff - Initial: {}, Current: {}, Handed off: {}", initialChannelId, currentChannelId,
                connectionHandedOff);

        ConnectionAnalysisResult result = new ConnectionAnalysisResult(wasDisconnected, wasDeactivated, eventBusCleanup,
                nettyCleanup, connectionHandedOff, initialChannelId, currentChannelId);

        if (result.isFullyCleanedUpViaEventBus()) {
            log.info("✓ Connection closure validation passed - EventBus events indicate proper cleanup");
        } else {
            log.warn("⚠ Potential connection cleanup issue detected - EventBus events indicate incomplete cleanup");
        }

        return result;
    }

    public void stopMonitoring() {
        monitoringActive.set(false);
    }

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
         * Primary indicator: connection is fully cleaned up based on EventBus monitoring.
         */
        public boolean isFullyCleanedUpViaEventBus() {
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

}
