/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.event.connection;

/**
 * Event fired on successful connection re-authentication
 *
 * @author Ivo Gaydajiev
 * @since 6.6.0
 * @see io.lettuce.core.StreamingCredentialsProvider
 */
public class ReauthenticationEvent implements AuthenticationEvent {

    private final String epId;

    /**
     * Create a new {@link ReauthenticationEvent} given a connection endpoint ID
     *
     * @param epId the connection endpoint ID
     */
    public ReauthenticationEvent(String epId) {
        this.epId = epId;
    }

    @Override
    public String getEpId() {
        return epId;
    }

}
