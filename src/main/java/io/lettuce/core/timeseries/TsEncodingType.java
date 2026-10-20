/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.charset.StandardCharsets;

/**
 * Chunk encodings known to this Lettuce version.
 *
 * @see TsEncoding#of(String)
 * @since 8.0
 */
public enum TsEncodingType implements TsEncoding {

    /**
     * Compressed chunks.
     */
    COMPRESSED,

    /**
     * Uncompressed chunks.
     */
    UNCOMPRESSED;

    private final String token;

    private final byte[] bytes;

    TsEncodingType() {
        this.token = name();
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
