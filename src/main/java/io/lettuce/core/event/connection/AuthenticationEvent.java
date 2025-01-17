/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.event.connection;

import io.lettuce.core.event.Event;

/**
 * Interface for Connection authentication events
 *
 * @author Ivo Gaydajiev
 * @since 6.6.0
 */
public interface AuthenticationEvent extends Event {

    /**
     * @return the endpoint ID associated with this event
     */
    String getEpId();

}
