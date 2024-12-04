package io.lettuce.core.event.connection;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

/**
 * Flight recorder event variant of {@link ReauthEvent}.
 *
 * @author Ivo Gaydajiev
 * @since 6.5.2
 */
@Category({ "Lettuce", "Connection Events" })
@Label("Reauthenticate to a Redis server failed")
@StackTrace(value = false)
class JfrReauthFailedEvent extends Event {

    private final String epId;

    public JfrReauthFailedEvent(ReauthenticateFailedEvent event) {
        this.epId = event.getEpId();
    }

    public String getEpId() {
        return epId;
    }

}
