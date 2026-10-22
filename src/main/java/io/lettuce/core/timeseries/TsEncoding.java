/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Chunk encoding of a TimeSeries ({@code ENCODING}).
 * <p>
 * Pass a constant of {@link TsEncodingType}, or {@link #of(String)} for an encoding this Lettuce version does not know yet.
 * TS.INFO returns the chunk type as a {@link TsEncodingType} constant when it is known, and as a value that keeps the server's
 * name otherwise.
 *
 * @see TsEncodingType
 * @since 8.0
 */
public interface TsEncoding extends ProtocolKeyword {

    /**
     * Return the encoding for {@code name}. A known name, ignoring case, returns the {@link TsEncodingType} constant (for
     * example {@code of("compressed")} returns {@link TsEncodingType#COMPRESSED}). Any other name returns a value that is sent
     * to the server as given. Lettuce does not check such names; the server rejects names it does not support.
     *
     * @param name the encoding name as the server spells it, must not be empty.
     * @return the encoding.
     * @throws IllegalArgumentException if {@code name} is {@code null} or empty.
     */
    static TsEncoding of(String name) {

        LettuceAssert.notEmpty(name, "Name must not be empty");

        for (TsEncodingType type : TsEncodingType.values()) {
            if (type.getToken().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return new RawTsEncoding(name);
    }

}
