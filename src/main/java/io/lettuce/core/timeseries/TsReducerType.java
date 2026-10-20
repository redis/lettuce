/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.charset.StandardCharsets;

/**
 * Reducers known to this Lettuce version. {@code FIRST}, {@code LAST} and {@code TWA} are not reducers; the server rejects
 * them.
 *
 * @see TsReducer#of(String)
 * @since 8.0
 */
public enum TsReducerType implements TsReducer {

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
     * Arithmetic mean of the values.
     */
    AVG,

    /**
     * Difference between the maximum and the minimum value.
     */
    RANGE,

    /**
     * Number of values that are not NaN.
     */
    COUNT,

    /**
     * Number of NaN values. Requires Redis 8.6 or later.
     */
    COUNTNAN,

    /**
     * Number of values, including NaN values. Requires Redis 8.6 or later.
     */
    COUNTALL,

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
    VAR_S("VAR.S");

    private final String token;

    private final byte[] bytes;

    TsReducerType() {
        this.token = name();
        this.bytes = token.getBytes(StandardCharsets.US_ASCII);
    }

    TsReducerType(String token) {
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

}
