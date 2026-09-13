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
 * Duplicate sample handling policies used by the Redis <a href="https://redis.io/commands/ts.create/">TS.CREATE</a> and
 * <a href="https://redis.io/commands/ts.alter/">TS.ALTER</a> {@code DUPLICATE_POLICY} option.
 *
 * @author Gyumin Hwang
 * @since 7.8
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

    /**
     * Parses the wire-format value reported by {@code TS.INFO} (e.g. {@code "last"}) back into a {@link TsDuplicatePolicy}.
     *
     * @param wireValue the wire-format value, or {@code null}.
     * @return the matching {@link TsDuplicatePolicy}, or {@code null} if {@code wireValue} is {@code null} or does not match a
     *         known constant.
     */
    public static TsDuplicatePolicy fromWire(String wireValue) {
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
