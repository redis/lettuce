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
 * @author Aleksandar Todorov
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
     * Return the name sent to the server.
     *
     * @return the name sent to the server.
     */
    String getToken() {
        return token;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Return the name sent to the server, for example {@code COMPRESSED}.
     *
     * @return the name sent to the server.
     */
    @Override
    public String toString() {
        return token;
    }

}
