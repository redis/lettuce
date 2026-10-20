/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.charset.StandardCharsets;

/**
 * Duplicate policies known to this Lettuce version.
 *
 * @see TsDuplicatePolicy#of(String)
 * @since 8.0
 */
public enum TsDuplicatePolicyType implements TsDuplicatePolicy {

    /**
     * Reject the new sample with an error.
     */
    BLOCK,

    /**
     * Keep the existing value and ignore the new one.
     */
    FIRST,

    /**
     * Replace the existing value with the new one.
     */
    LAST,

    /**
     * Keep the lower of the existing and the new value.
     */
    MIN,

    /**
     * Keep the higher of the existing and the new value.
     */
    MAX,

    /**
     * Store the sum of the existing and the new value.
     */
    SUM;

    private final String token;

    private final byte[] bytes;

    TsDuplicatePolicyType() {
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
