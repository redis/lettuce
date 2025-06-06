package io.lettuce.scenario;

import java.net.SocketAddress;
import java.time.Duration;
import java.time.Instant;

/**
 * Represents a reconnection-related event with timing information.
 */
public class ReconnectionEvent {

    public enum Type {
        DISCONNECTED, RECONNECTED, ATTEMPT, FAILED
    }

    private final Type type;

    private final Instant timestamp;

    private final SocketAddress remoteAddress;

    private final Duration duration;

    private final int attemptNumber;

    private final Duration delay;

    private final Throwable cause;

    // Constructor for DISCONNECTED and basic RECONNECTED events
    public ReconnectionEvent(Type type, Instant timestamp, SocketAddress remoteAddress) {
        this(type, timestamp, remoteAddress, null, 0, null, null);
    }

    // Constructor for RECONNECTED events with duration
    public ReconnectionEvent(Type type, Instant timestamp, SocketAddress remoteAddress, Duration duration) {
        this(type, timestamp, remoteAddress, duration, 0, null, null);
    }

    // Constructor for ATTEMPT events
    public ReconnectionEvent(Type type, Instant timestamp, SocketAddress remoteAddress, int attemptNumber, Duration delay) {
        this(type, timestamp, remoteAddress, null, attemptNumber, delay, null);
    }

    // Constructor for FAILED events
    public ReconnectionEvent(Type type, Instant timestamp, SocketAddress remoteAddress, int attemptNumber, Throwable cause) {
        this(type, timestamp, remoteAddress, null, attemptNumber, null, cause);
    }

    // Full constructor
    private ReconnectionEvent(Type type, Instant timestamp, SocketAddress remoteAddress, Duration duration, int attemptNumber,
            Duration delay, Throwable cause) {
        this.type = type;
        this.timestamp = timestamp;
        this.remoteAddress = remoteAddress;
        this.duration = duration;
        this.attemptNumber = attemptNumber;
        this.delay = delay;
        this.cause = cause;
    }

    public Type getType() {
        return type;
    }

    public Duration getDuration() {
        return duration;
    }

    public Duration getDelay() {
        return delay;
    }

    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ReconnectionEvent{").append("type=").append(type).append(", timestamp=").append(timestamp)
                .append(", remoteAddress=").append(remoteAddress);

        if (duration != null) {
            sb.append(", duration=").append(duration.toMillis()).append("ms");
        }
        if (attemptNumber > 0) {
            sb.append(", attempt=").append(attemptNumber);
        }
        if (delay != null) {
            sb.append(", delay=").append(delay.toMillis()).append("ms");
        }
        if (cause != null) {
            sb.append(", cause=").append(cause.getMessage());
        }

        sb.append('}');
        return sb.toString();
    }

}
