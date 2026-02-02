/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.failover.health;

import java.io.IOException;

/**
 * Exception thrown when a Redis REST API operation fails. This exception provides additional context about HTTP-specific errors
 * including status codes and response bodies.
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
public class RedisRestException extends RuntimeException {

    private final int statusCode;

    private final String responseBody;

    /**
     * Creates a new {@link RedisRestException} with the specified error message, HTTP status code, and response body.
     *
     * @param message the error message
     * @param statusCode the HTTP status code
     * @param responseBody the HTTP response body
     */
    public RedisRestException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Creates a new {@link RedisRestException} with the specified error message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public RedisRestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }

    /**
     * Creates a new {@link RedisRestException} with the specified error message.
     *
     * @param message the error message
     */
    public RedisRestException(String message) {
        super(message);
        this.statusCode = -1;
        this.responseBody = null;
    }

    /**
     * Returns the HTTP status code associated with this exception, or -1 if not applicable.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the HTTP response body associated with this exception, or null if not available.
     *
     * @return the HTTP response body
     */
    public String getResponseBody() {
        return responseBody;
    }

    @Override
    public String toString() {
        if (statusCode > 0) {
            return super.toString() + " [HTTP " + statusCode + "]";
        }
        return super.toString();
    }

}
