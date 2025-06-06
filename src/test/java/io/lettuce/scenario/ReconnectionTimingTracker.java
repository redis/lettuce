package io.lettuce.scenario;

import java.net.SocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionStateAdapter;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.event.connection.ConnectionActivatedEvent;
import io.lettuce.core.event.connection.ConnectionDeactivatedEvent;
import io.lettuce.core.event.connection.ReconnectAttemptEvent;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import reactor.core.Disposable;

/**
 * Utility class for tracking Redis connection reconnection timing. Provides comprehensive monitoring of connection lifecycle
 * events and timing metrics.
 */
public class ReconnectionTimingTracker {

    private static final Logger log = LoggerFactory.getLogger(ReconnectionTimingTracker.class);

    private final String trackerName;

    private final List<ReconnectionEvent> reconnectionEvents = new CopyOnWriteArrayList<>();

    private final List<Duration> reconnectionDurations = new CopyOnWriteArrayList<>();

    private final AtomicReference<Instant> lastDisconnectTime = new AtomicReference<>();

    // Event bus tracking
    private Disposable eventSubscription;

    // Connection state tracking
    private final AtomicReference<Instant> stateDisconnectTime = new AtomicReference<>();

    private final List<Duration> stateReconnectionDurations = new CopyOnWriteArrayList<>();

    /**
     * Creates a new reconnection timing tracker.
     * 
     * @param trackerName descriptive name for this tracker (e.g., "PubSub", "Reactive")
     */
    public ReconnectionTimingTracker(String trackerName) {
        this.trackerName = trackerName;
    }

    /**
     * Start tracking reconnection events using the EventBus approach.
     * 
     * @param client the Redis client to monitor
     * @return this tracker for method chaining
     */
    public ReconnectionTimingTracker trackWithEventBus(RedisClient client) {
        eventSubscription = client.getResources().eventBus().get().subscribe(event -> {
            Instant now = Instant.now();

            if (event instanceof ConnectionDeactivatedEvent) {
                ConnectionDeactivatedEvent deactivated = (ConnectionDeactivatedEvent) event;
                log.info("{} connection disconnected: {} at {}", trackerName, deactivated.remoteAddress(), now);
                lastDisconnectTime.set(now);
                reconnectionEvents
                        .add(new ReconnectionEvent(ReconnectionEvent.Type.DISCONNECTED, now, deactivated.remoteAddress()));

            } else if (event instanceof ConnectionActivatedEvent) {
                ConnectionActivatedEvent activated = (ConnectionActivatedEvent) event;
                log.info("{} connection reconnected: {} at {}", trackerName, activated.remoteAddress(), now);

                Instant disconnectTime = lastDisconnectTime.get();
                if (disconnectTime != null) {
                    Duration reconnectionDuration = Duration.between(disconnectTime, now);
                    reconnectionDurations.add(reconnectionDuration);
                    reconnectionEvents.add(new ReconnectionEvent(ReconnectionEvent.Type.RECONNECTED, now,
                            activated.remoteAddress(), reconnectionDuration));
                    log.info("{} reconnection completed in: {} ms", trackerName, reconnectionDuration.toMillis());
                }

            } else if (event instanceof ReconnectAttemptEvent) {
                ReconnectAttemptEvent attempt = (ReconnectAttemptEvent) event;
                log.info("{} reconnect attempt #{} with delay: {} ms", trackerName, attempt.getAttempt(),
                        attempt.getDelay().toMillis());
                reconnectionEvents.add(new ReconnectionEvent(ReconnectionEvent.Type.ATTEMPT, now, attempt.remoteAddress(),
                        attempt.getAttempt(), attempt.getDelay()));

            } else if (event instanceof ReconnectFailedEvent) {
                ReconnectFailedEvent failed = (ReconnectFailedEvent) event;
                log.warn("{} reconnect attempt #{} failed: {}", trackerName, failed.getAttempt(),
                        failed.getCause().getMessage());
                reconnectionEvents.add(new ReconnectionEvent(ReconnectionEvent.Type.FAILED, now, failed.remoteAddress(),
                        failed.getAttempt(), failed.getCause()));
            }
        });

        return this;
    }

    /**
     * Start tracking reconnection events using the ConnectionStateListener approach.
     *
     * @param connection the Redis connection to monitor
     * @return this tracker for method chaining
     */
    public <K, V> ReconnectionTimingTracker trackWithStateListener(StatefulRedisConnection<K, V> connection) {
        connection.addListener(new RedisConnectionStateAdapter() {

            @Override
            public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
                Instant now = Instant.now();
                stateDisconnectTime.set(now);
                log.info("{} connection state: DISCONNECTED at {}", trackerName, now);
            }

            @Override
            public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
                Instant now = Instant.now();
                log.info("{} connection state: CONNECTED to {} at {}", trackerName, socketAddress, now);

                Instant lastDisconnect = stateDisconnectTime.get();
                if (lastDisconnect != null) {
                    Duration duration = Duration.between(lastDisconnect, now);
                    stateReconnectionDurations.add(duration);
                    log.info("{} state listener reconnection duration: {} ms", trackerName, duration.toMillis());
                }
            }

        });

        return this;
    }

    /**
     * Get reconnection statistics and optionally execute a callback with the stats.
     * 
     * @param statsConsumer optional consumer to process the statistics
     * @return the reconnection statistics
     */
    public ReconnectionStats getStats(Consumer<ReconnectionStats> statsConsumer) {
        ReconnectionStats stats = new ReconnectionStats(trackerName, reconnectionDurations, stateReconnectionDurations,
                reconnectionEvents);

        if (statsConsumer != null) {
            statsConsumer.accept(stats);
        }

        return stats;
    }

    /**
     * Get reconnection statistics.
     * 
     * @return the reconnection statistics
     */
    public ReconnectionStats getStats() {
        return getStats(null);
    }

    /**
     * Log comprehensive reconnection statistics.
     */
    public void logStats() {
        ReconnectionStats stats = getStats();
        stats.logStatistics();
    }

    /**
     * Clean up resources and stop tracking.
     */
    public void dispose() {
        if (eventSubscription != null && !eventSubscription.isDisposed()) {
            eventSubscription.dispose();
        }
    }

    /**
     * Check if any reconnections were tracked.
     * 
     * @return true if reconnections were detected
     */
    public boolean hasReconnections() {
        return !reconnectionDurations.isEmpty() || !stateReconnectionDurations.isEmpty();
    }

    /**
     * Create a tracker with a custom name.
     * 
     * @param name the tracker name
     * @return new tracker with the specified name
     */
    public static ReconnectionTimingTracker withName(String name) {
        return new ReconnectionTimingTracker(name);
    }

}
