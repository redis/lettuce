/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import java.nio.charset.StandardCharsets;

import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Number of bytes per counter cell of a Count-Min Sketch, passed as the {@code CELL_SIZE} option of {@code CMS.INITBYDIM} and
 * {@code CMS.INITBYPROB}. Smaller cells reduce the memory footprint of the sketch but lower the maximum count a cell can hold.
 * The server default is {@link #FOUR_BYTES}. Requires Redis 8.12 or later.
 *
 * @since 7.8
 */
public enum CmsCellSize implements ProtocolKeyword {

    /**
     * 1-byte counter cells, holding counts up to {@code 255}.
     */
    ONE_BYTE(1),

    /**
     * 2-byte counter cells, holding counts up to {@code 65535}.
     */
    TWO_BYTES(2),

    /**
     * 4-byte counter cells, holding counts up to {@code 4294967295}. The server default.
     */
    FOUR_BYTES(4),

    /**
     * 8-byte counter cells, holding counts up to {@code 2^64 - 1}.
     */
    EIGHT_BYTES(8);

    private final byte[] bytes;

    CmsCellSize(int sizeInBytes) {
        this.bytes = Integer.toString(sizeInBytes).getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
