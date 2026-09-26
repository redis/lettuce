/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Chunk encoding formats used by the Redis <a href="https://redis.io/commands/ts.create/">TS.CREATE</a> {@code ENCODING}
 * option.
 *
 * @author Gyumin Hwang
 * @since 7.8
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

    /**
     * Parses the wire-format value reported by {@code TS.INFO} (e.g. {@code "compressed"}) back into a
     * {@link TsEncodingFormat}.
     *
     * @param wireValue the wire-format value, or {@code null}.
     * @return the matching {@link TsEncodingFormat}, or {@code null} if {@code wireValue} is {@code null} or does not match a
     *         known constant.
     */
    public static TsEncodingFormat fromWire(String wireValue) {
        if (wireValue == null) {
            return null;
        }
        try {
            return valueOf(wireValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
