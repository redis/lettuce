package io.lettuce.core.event.connection;

import io.lettuce.core.event.Event;

import java.net.SocketAddress;

/**
 * Event fired on failed authentication caused either by I/O issues or during connection reauthentication.
 *
 * @author Ivo Gaydajiev
 */
public class ReauthFailedEvent implements Event {

    private final String epId;

    private final Throwable cause;

    public ReauthFailedEvent(String epId, Throwable cause) {
        this.epId = epId;
        this.cause = cause;
    }

    public String getEpId() {
        return epId;
    }

    /**
     * Returns the {@link Throwable} that describes the reauth failure cause.
     *
     * @return the {@link Throwable} that describes the reauth failure cause.
     */
    public Throwable getCause() {
        return cause;
    }

}
