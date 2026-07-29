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
 * Duplicate sample handling policies used by the Redis <a href="https://redis.io/commands/ts.create/">TS.CREATE</a> and
 * <a href="https://redis.io/commands/ts.alter/">TS.ALTER</a> {@code DUPLICATE_POLICY} option.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public enum TsDuplicatePolicy implements ProtocolKeyword {

    BLOCK,

    FIRST,

    LAST,

    MIN,

    MAX,

    SUM;

    private final byte[] bytes;

    TsDuplicatePolicy() {
        this.bytes = name().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
