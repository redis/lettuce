/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.event.connection;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

/**
 * Flight recorder event variant of {@link ReauthenticationFailedEvent}.
 *
 * @author Ivo Gaydajiev
 * @since 6.6.0
 */
@Category({ "Lettuce", "Connection Events" })
@Label("Reauthenticate to a Redis server failed")
@StackTrace(value = false)
class JfrReauthFailedEvent extends Event {

    private final String epId;

    /**
     * Create a new {@link JfrReauthFailedEvent} given a {@link ReauthenticationFailedEvent}.
     *
     * @param event the {@link ReauthenticationFailedEvent}
     */
    public JfrReauthFailedEvent(ReauthenticationFailedEvent event) {
        this.epId = event.getEpId();
    }

    /**
     * @return the connection endpoint ID
     */
    public String getEpId() {
        return epId;
    }

}
