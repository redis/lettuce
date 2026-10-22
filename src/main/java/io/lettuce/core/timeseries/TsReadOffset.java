/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Starting point of a <a href="https://redis.io/commands/ts.read/">TS.READ</a> call: the inclusive lower bound of the returned
 * samples. The server resolves {@link #earliest()}, {@link #latest()} and {@link #newSamples()} once, when it receives the
 * command.
 * <p>
 * Use {@link #latest()} or {@link #newSamples()} only on the first call. On later calls, continue with
 * {@code from(lastTimestamp + 1)}, where {@code lastTimestamp} is the timestamp of the last sample returned. The
 * {@link #toString()} form is the argument sent to the server. {@link TsReadOffset} is immutable.
 *
 * @since 8.0
 */
public final class TsReadOffset {

    private static final TsReadOffset EARLIEST = new TsReadOffset("-");

    private static final TsReadOffset LATEST = new TsReadOffset("+");

    private static final TsReadOffset NEW_SAMPLES = new TsReadOffset("$");

    private final String token;

    private TsReadOffset(String token) {
        this.token = token;
    }

    /**
     * Start at the earliest sample of the series ({@code -}).
     *
     * @return the offset.
     * @since 8.0
     */
    public static TsReadOffset earliest() {
        return EARLIEST;
    }

    /**
     * Start at the latest sample of the series, inclusive ({@code +}). Use it only on the first call.
     *
     * @return the offset.
     * @since 8.0
     */
    public static TsReadOffset latest() {
        return LATEST;
    }

    /**
     * Start after the latest sample of the series, so that only samples added later are returned ({@code $}). Use it only on
     * the first call, together with {@code BLOCK}: without {@code BLOCK} the call always returns an empty list.
     *
     * @return the offset.
     * @since 8.0
     */
    public static TsReadOffset newSamples() {
        return NEW_SAMPLES;
    }

    /**
     * Start at {@code timestamp}, inclusive.
     *
     * @param timestamp the timestamp in milliseconds, must not be negative.
     * @return the offset.
     * @throws IllegalArgumentException if {@code timestamp} is negative.
     * @since 8.0
     */
    public static TsReadOffset from(long timestamp) {

        LettuceAssert.isTrue(timestamp >= 0, "Timestamp must not be negative");

        return new TsReadOffset(Long.toString(timestamp));
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof TsReadOffset)) {
            return false;
        }

        return token.equals(((TsReadOffset) o).token);
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

    /**
     * Return the argument sent to the server: {@code -}, {@code +}, {@code $} or the timestamp.
     *
     * @return the wire form.
     */
    @Override
    public String toString() {
        return token;
    }

}
