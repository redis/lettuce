package io.lettuce.core.event.connection;

/**
 * Event fired on failed authentication caused either by I/O issues or during connection re-authentication. see
 * {@link io.lettuce.core.StreamingCredentialsProvider}
 * 
 * @author Ivo Gaydajiev
 * @since 6.5.2
 */
public class ReauthenticateFailedEvent implements AuthenticateEvent {

    private final String epId;

    private final Throwable cause;

    public ReauthenticateFailedEvent(String epId, Throwable cause) {
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
