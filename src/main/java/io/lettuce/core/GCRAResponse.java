/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

/**
 * Represents the response from the Redis <a href="https://redis.io/commands/gcra">GCRA</a> command.
 * <p>
 * The GCRA (Generic Cell Rate Algorithm) command is used for rate limiting. The response contains information about whether the
 * request was rate-limited and the current state of the rate limiter.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see <a href="https://redis.io/commands/gcra">Redis Documentation: GCRA</a>
 */
public class GCRAResponse {

    private boolean limited;

    private long maxRequests;

    private long availableRequests;

    private long retryAfter;

    private long fullBurstAfter;

    /**
     * Creates a new empty {@link GCRAResponse} instance.
     */
    public GCRAResponse() {
    }

    /**
     * Creates a new {@link GCRAResponse} instance with all fields set.
     *
     * @param limited whether the request was rate-limited.
     * @param maxRequests the maximum number of requests allowed in the period.
     * @param availableRequests the number of requests still available in the current period.
     * @param retryAfter seconds until the request should be retried (-1 if not limited).
     * @param fullBurstAfter seconds until the full burst capacity is restored.
     */
    public GCRAResponse(boolean limited, long maxRequests, long availableRequests, long retryAfter, long fullBurstAfter) {
        this.limited = limited;
        this.maxRequests = maxRequests;
        this.availableRequests = availableRequests;
        this.retryAfter = retryAfter;
        this.fullBurstAfter = fullBurstAfter;
    }

    /**
     * @return {@code true} if the request was rate-limited, {@code false} otherwise.
     */
    public boolean isLimited() {
        return limited;
    }

    /**
     * @param limited whether the request was rate-limited.
     */
    public void setLimited(boolean limited) {
        this.limited = limited;
    }

    /**
     * @return the maximum number of requests allowed in the period.
     */
    public long getMaxRequests() {
        return maxRequests;
    }

    /**
     * @param maxRequests the maximum number of requests allowed in the period.
     */
    public void setMaxRequests(long maxRequests) {
        this.maxRequests = maxRequests;
    }

    /**
     * @return the number of requests still available in the current period.
     */
    public long getAvailableRequests() {
        return availableRequests;
    }

    /**
     * @param availableRequests the number of requests still available in the current period.
     */
    public void setAvailableRequests(long availableRequests) {
        this.availableRequests = availableRequests;
    }

    /**
     * @return seconds until the request should be retried. Returns -1 if the request was not limited.
     */
    public long getRetryAfter() {
        return retryAfter;
    }

    /**
     * @param retryAfter seconds until the request should be retried.
     */
    public void setRetryAfter(long retryAfter) {
        this.retryAfter = retryAfter;
    }

    /**
     * @return seconds until the full burst capacity is restored.
     */
    public long getFullBurstAfter() {
        return fullBurstAfter;
    }

    /**
     * @param fullBurstAfter seconds until the full burst capacity is restored.
     */
    public void setFullBurstAfter(long fullBurstAfter) {
        this.fullBurstAfter = fullBurstAfter;
    }

    @Override
    public String toString() {
        return "GCRAResponse{" + "limited=" + limited + ", maxRequests=" + maxRequests + ", availableRequests="
                + availableRequests + ", retryAfter=" + retryAfter + ", fullBurstAfter=" + fullBurstAfter + '}';
    }

}
