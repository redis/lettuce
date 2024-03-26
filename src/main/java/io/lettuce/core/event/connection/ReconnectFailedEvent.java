package io.lettuce.core.event.connection;

import java.net.SocketAddress;

/**
 * Event fired on failed reconnect caused either by I/O issues or during connection initialization.
 *
 * @author Mark Paluch
 * @since 5.2
 */
public class ReconnectFailedEvent extends ConnectionEventSupport {

    private final Throwable cause;

    private final int attempt;

    public ReconnectFailedEvent(String redisUri, String epId, SocketAddress local, SocketAddress remote, Throwable cause,
            int attempt) {
        super(redisUri, epId, null, local, remote);
        this.cause = cause;
        this.attempt = attempt;
    }

    public ReconnectFailedEvent(SocketAddress local, SocketAddress remote, Throwable cause, int attempt) {
        super(local, remote);
        this.cause = cause;
        this.attempt = attempt;
    }

    /**
     * Returns the {@link Throwable} that describes the reconnect cause.
     *
     * @return the {@link Throwable} that describes the reconnect cause.
     */
    public Throwable getCause() {
        return cause;
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

}
