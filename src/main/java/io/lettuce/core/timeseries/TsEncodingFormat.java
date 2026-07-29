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
 * Chunk encoding formats used by the Redis <a href="https://redis.io/commands/ts.create/">TS.CREATE</a> {@code ENCODING}
 * option.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public enum TsEncodingFormat implements ProtocolKeyword {

    COMPRESSED,

    UNCOMPRESSED;

    private final byte[] bytes;

    TsEncodingFormat() {
        this.bytes = name().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
