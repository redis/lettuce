package io.lettuce.core.event.connection;

import java.net.SocketAddress;
import java.time.Duration;

/**
 * Event fired on reconnect attempts.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class ReconnectAttemptEvent extends ConnectionEventSupport {

    private final int attempt;

    private final Duration delay;

    public ReconnectAttemptEvent(String redisUri, String epId, SocketAddress local, SocketAddress remote, int attempt,
            Duration delay) {
        super(redisUri, epId, null, local, remote);
        this.attempt = attempt;
        this.delay = delay;
    }

    public ReconnectAttemptEvent(SocketAddress local, SocketAddress remote, int attempt) {
        super(local, remote);
        this.attempt = attempt;
        this.delay = Duration.ZERO;
    }

    /**
     * Returns the reconnect attempt counter for the connection. Zero-based counter, {@code 0} represents the first attempt. The
     * counter is reset upon successful reconnect.
     *
     * @return the reconnect attempt counter for the connection. Zero-based counter, {@code 0} represents the first attempt.
     */
    public int getAttempt() {
        return attempt;
    }

    public Duration getDelay() {
        return delay;
    }

}
