/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.event.connection;

/**
 * Event fired on failed authentication caused either by I/O issues or during connection re-authentication.
 * 
 * @author Ivo Gaydajiev
 * @since 6.6.0
 * @see io.lettuce.core.RedisCredentialsProvider
 */
public class ReauthenticationFailedEvent implements AuthenticationEvent {

    private final String epId;

    private final Throwable cause;

    /**
     * Create a new {@link ReauthenticationFailedEvent} given a {@link Throwable} that describes the re-authentication failure
     * cause.
     *
     * @param cause the {@link Throwable} that describes the re-authentication failure cause.
     */
    public ReauthenticationFailedEvent(Throwable cause) {
        this(null, cause);
    }

    /**
     * Create a new {@link ReauthenticationFailedEvent} given a connection endpoint ID and a {@link Throwable} that describes
     * the re-authentication failure cause.
     *
     * @param epId the connection endpoint ID
     * @param cause the {@link Throwable} that describes the re-authentication failure cause.
     */
    public ReauthenticationFailedEvent(String epId, Throwable cause) {
        this.epId = epId;
        this.cause = cause;
    }

    @Override
    public String getEpId() {
        return epId;
    }

    /**
     * @return the {@link Throwable} that describes the re-authentication failure cause.
     */
    public Throwable getCause() {
        return cause;
    }

}
