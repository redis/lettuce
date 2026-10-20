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
 * Timestamp reported for an aggregated bucket ({@code BUCKETTIMESTAMP}).
 *
 * @since 8.0
 */
public enum TsBucketTimestamp implements ProtocolKeyword {

    /**
     * The start of the bucket. This is the server's default.
     */
    START("-"),

    /**
     * The end of the bucket.
     */
    END("+"),

    /**
     * The middle of the bucket, rounded down.
     */
    MID("~");

    private final byte[] bytes;

    TsBucketTimestamp(String token) {
        this.bytes = token.getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
