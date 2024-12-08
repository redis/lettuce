package io.lettuce.core.event.connection;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

/**
 * Flight recorder event variant of {@link ReauthenticateEvent}.
 *
 * @author Ivo Gaydajiev
 * @since 6.6.0
 */
@Category({ "Lettuce", "Connection Events" })
@Label("Reauthenticate to a Redis server")
@StackTrace(value = false)
class JfrReauthEvent extends Event {

    private final String epId;

    public JfrReauthEvent(ReauthenticateEvent event) {
        this.epId = event.getEpId();
    }

    public String getEpId() {
        return epId;
    }

}
