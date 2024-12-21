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
 * Flight recorder event variant of {@link ReauthenticationEvent}.
 *
 * @author Ivo Gaydajiev
 * @since 6.6.0
 */
@Category({ "Lettuce", "Connection Events" })
@Label("Reauthenticate to a Redis server")
@StackTrace(value = false)
class JfrReauthEvent extends Event {

    private final String epId;

    /**
     * Create a new {@link JfrReauthEvent} given a {@link ReauthenticationEvent}.
     *
     * @param event the {@link ReauthenticationEvent}
     */
    public JfrReauthEvent(ReauthenticationEvent event) {
        this.epId = event.getEpId();
    }

    /**
     * @return the connection endpoint ID
     */
    public String getEpId() {
        return epId;
    }

}
