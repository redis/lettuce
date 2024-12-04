package io.lettuce.core.event.connection;

/**
 * Event fired on successfull connection re-authentication. see {@link io.lettuce.core.StreamingCredentialsProvider}
 *
 * @author Ivo Gaydajiev
 * @since 6.5.2
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
