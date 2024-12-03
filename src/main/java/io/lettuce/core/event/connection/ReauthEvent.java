package io.lettuce.core.event.connection;

import io.lettuce.core.event.Event;

/**
 * Event fired on failed authentication caused either by I/O issues or during connection reauthentication.
 *
 * @author Ivo Gaydajiev
 */
public class ReauthEvent implements Event {

    private final String epId;

    public ReauthEvent(String epId) {
        this.epId = epId;
    }

    public String getEpId() {
        return epId;
    }

}
