/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.charset.StandardCharsets;

import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Aggregation types used by the Redis <a href="https://redis.io/commands/ts.createrule/">TS.CREATERULE</a> command.
 * <p>
 * Some constants (e.g. {@link #STD_P}) cannot be represented as a plain Java identifier because their wire value contains a
 * dot. These constants use the {@link #TsAggregationType(String)} constructor to override the wire value.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public enum TsAggregationType implements ProtocolKeyword {

    AVG,

    SUM,

    MIN,

    MAX,

    RANGE,

    COUNT,

    FIRST,

    LAST,

    STD_P("STD.P"),

    STD_S("STD.S"),

    VAR_P("VAR.P"),

    VAR_S("VAR.S"),

    TWA,

    COUNTNAN,

    COUNTALL;

    private final byte[] bytes;

    private final String value;

    TsAggregationType() {
        this.value = name();
        this.bytes = value.getBytes(StandardCharsets.US_ASCII);
    }

    TsAggregationType(String value) {
        this.value = value;
        this.bytes = value.getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return value;
    }

}
