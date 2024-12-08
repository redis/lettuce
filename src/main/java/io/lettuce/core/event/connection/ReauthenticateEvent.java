package io.lettuce.core.event.connection;

/**
 * Event fired on successfull connection re-authentication. see {@link io.lettuce.core.StreamingCredentialsProvider}
 *
 * @author Ivo Gaydajiev
 * @since 6.6.0
 */
public class ReauthenticateEvent implements AuthenticateEvent {

    private final String epId;

    public ReauthenticateEvent(String epId) {
        this.epId = epId;
    }

    public String getEpId() {
        return epId;
    }

}
