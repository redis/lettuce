package io.lettuce.scenario;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Statistics and analysis for reconnection timing data.
 */
public class ReconnectionStats {

    private static final Logger log = LoggerFactory.getLogger(ReconnectionStats.class);

    private final String trackerName;

    private final List<Duration> eventBusReconnections;

    private final List<Duration> stateListenerReconnections;

    private final List<ReconnectionEvent> events;

    public ReconnectionStats(String trackerName, List<Duration> eventBusReconnections,
            List<Duration> stateListenerReconnections, List<ReconnectionEvent> events) {
        this.trackerName = trackerName;
        this.eventBusReconnections = Collections.unmodifiableList(new ArrayList<>(eventBusReconnections));
        this.stateListenerReconnections = Collections.unmodifiableList(new ArrayList<>(stateListenerReconnections));
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
    }

    /**
     * Get the number of reconnections detected by EventBus tracking.
     */
    public int getEventBusReconnectionCount() {
        return eventBusReconnections.size();
    }

    /**
     * Get the number of reconnections detected by state listener tracking.
     */
    public int getStateListenerReconnectionCount() {
        return stateListenerReconnections.size();
    }

    /**
     * Get total reconnection time from EventBus tracking.
     */
    public Duration getTotalEventBusReconnectionTime() {
        return eventBusReconnections.stream().reduce(Duration.ZERO, Duration::plus);
    }

    /**
     * Get average reconnection time from EventBus tracking.
     */
    public Duration getAverageEventBusReconnectionTime() {
        if (eventBusReconnections.isEmpty()) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(getTotalEventBusReconnectionTime().toMillis() / eventBusReconnections.size());
    }

    /**
     * Get total reconnection time from state listener tracking.
     */
    public Duration getTotalStateListenerReconnectionTime() {
        return stateListenerReconnections.stream().reduce(Duration.ZERO, Duration::plus);
    }

    /**
     * Get average reconnection time from state listener tracking.
     */
    public Duration getAverageStateListenerReconnectionTime() {
        if (stateListenerReconnections.isEmpty()) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(getTotalStateListenerReconnectionTime().toMillis() / stateListenerReconnections.size());
    }

    /**
     * Get the fastest reconnection time from EventBus tracking.
     */
    public Duration getFastestReconnection() {
        return eventBusReconnections.stream().min(Duration::compareTo).orElse(Duration.ZERO);
    }

    /**
     * Get the slowest reconnection time from EventBus tracking.
     */
    public Duration getSlowestReconnection() {
        return eventBusReconnections.stream().max(Duration::compareTo).orElse(Duration.ZERO);
    }

    /**
     * Get the number of failed reconnection attempts.
     */
    public long getFailedAttemptCount() {
        return events.stream().filter(event -> event.getType() == ReconnectionEvent.Type.FAILED).count();
    }

    /**
     * Get the total number of reconnection attempts (including successful ones).
     */
    public long getTotalAttemptCount() {
        return events.stream().filter(
                event -> event.getType() == ReconnectionEvent.Type.ATTEMPT || event.getType() == ReconnectionEvent.Type.FAILED)
                .count();
    }

    /**
     * Log comprehensive statistics.
     */
    public void logStatistics() {
        log.info("=== {} Reconnection Statistics ===", trackerName);

        if (!eventBusReconnections.isEmpty()) {
            log.info("EventBus Tracking:");
            log.info("  - Number of reconnections: {}", getEventBusReconnectionCount());
            log.info("  - Total reconnection time: {} ms", getTotalEventBusReconnectionTime().toMillis());
            log.info("  - Average reconnection time: {} ms", getAverageEventBusReconnectionTime().toMillis());
            log.info("  - Fastest reconnection: {} ms", getFastestReconnection().toMillis());
            log.info("  - Slowest reconnection: {} ms", getSlowestReconnection().toMillis());
            log.info("  - Individual times: {}", formatDurations(eventBusReconnections));
        }

        if (!stateListenerReconnections.isEmpty()) {
            log.info("State Listener Tracking:");
            log.info("  - Number of reconnections: {}", getStateListenerReconnectionCount());
            log.info("  - Total reconnection time: {} ms", getTotalStateListenerReconnectionTime().toMillis());
            log.info("  - Average reconnection time: {} ms", getAverageStateListenerReconnectionTime().toMillis());
            log.info("  - Individual times: {}", formatDurations(stateListenerReconnections));
        }

        if (getTotalAttemptCount() > 0) {
            log.info("Reconnection Attempts:");
            log.info("  - Total attempts: {}", getTotalAttemptCount());
            log.info("  - Failed attempts: {}", getFailedAttemptCount());
            log.info("  - Success rate: {}%", getSuccessRate());
        }

        if (eventBusReconnections.isEmpty() && stateListenerReconnections.isEmpty()) {
            log.info("No reconnections detected");
        }
    }

    /**
     * Get the success rate of reconnection attempts.
     */
    public double getSuccessRate() {
        long totalAttempts = getTotalAttemptCount();
        if (totalAttempts == 0) {
            return 100.0;
        }
        long failedAttempts = getFailedAttemptCount();
        return ((double) (totalAttempts - failedAttempts) / totalAttempts) * 100.0;
    }

    private String formatDurations(List<Duration> durations) {
        return durations.stream().map(d -> d.toMillis() + "ms").collect(Collectors.joining(", ", "[", "]"));
    }

}
