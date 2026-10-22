/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.charset.StandardCharsets;

/**
 * Aggregation functions known to this Lettuce version.
 *
 * @see TsAggregation#of(String)
 * @since 8.0
 */
public enum TsAggregationType implements TsAggregation {

    /**
     * Arithmetic mean of the values.
     */
    AVG,

    /**
     * Sum of the values.
     */
    SUM,

    /**
     * Minimum value.
     */
    MIN,

    /**
     * Maximum value.
     */
    MAX,

    /**
     * Difference between the maximum and the minimum value.
     */
    RANGE,

    /**
     * Number of values that are not NaN.
     */
    COUNT,

    /**
     * Value with the lowest timestamp in the bucket.
     */
    FIRST,

    /**
     * Value with the highest timestamp in the bucket.
     */
    LAST,

    /**
     * Population standard deviation of the values.
     */
    STD_P("STD.P"),

    /**
     * Sample standard deviation of the values.
     */
    STD_S("STD.S"),

    /**
     * Population variance of the values.
     */
    VAR_P("VAR.P"),

    /**
     * Sample variance of the values.
     */
    VAR_S("VAR.S"),

    /**
     * Time-weighted average over the bucket's timeframe.
     */
    TWA,

    /**
     * Number of NaN values. Requires Redis 8.6 or later.
     */
    COUNTNAN,

    /**
     * Number of values, including NaN values. Requires Redis 8.6 or later.
     */
    COUNTALL;

    private final String token;

    private final byte[] bytes;

    TsAggregationType() {
        this.token = name();
        this.bytes = token.getBytes(StandardCharsets.US_ASCII);
    }

    TsAggregationType(String token) {
        this.token = token;
        this.bytes = token.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Return the token sent to the server.
     *
     * @return the wire token.
     */
    String getToken() {
        return token;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Return the wire token, for example {@code STD.P} for {@link #STD_P}.
     *
     * @return the wire token.
     */
    @Override
    public String toString() {
        return token;
    }

}
