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
 * Reducer that combines the series of one group of a {@code TS.MRANGE} or {@code TS.MREVRANGE} with {@code GROUPBY}
 * ({@code REDUCE}).
 * <p>
 * Pass a constant of {@link TsReducerType}, or {@link #of(String)} for a reducer this Lettuce version does not know yet. Names
 * given to {@code of} must not contain {@code ,} or whitespace; the server rejects names it does not support.
 * <p>
 * This interface is implemented by Lettuce: by the constants of {@link TsReducerType} and by the values that
 * {@link #of(String)} returns. It is not meant to be implemented by applications.
 *
 * @author Aleksandar Todorov
 * @see TsReducerType
 * @since 8.0
 */
public interface TsReducer extends ProtocolKeyword {

    /**
     * Return the reducer for {@code name}. A known name, ignoring case, returns the {@link TsReducerType} constant (for example
     * {@code of("sum")} returns {@link TsReducerType#SUM} and {@code of("std.p")} returns {@link TsReducerType#STD_P}). Any
     * other name returns a value that is sent to the server as given. Lettuce only checks that such a name contains no
     * {@code ,} and no whitespace; the server rejects names it does not support.
     *
     * @param name the reducer name as the server spells it, must not be empty and must not contain {@code ,} or whitespace.
     * @return the reducer.
     * @throws IllegalArgumentException if {@code name} is {@code null} or empty, or contains {@code ,} or whitespace.
     * @since 8.0
     */
    static TsReducer of(String name) {

        LettuceAssert.notEmpty(name, "Name must not be empty");
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            LettuceAssert.isTrue(c != ',' && !Character.isWhitespace(c), "Name must not contain ',' or whitespace");
        }

        for (TsReducerType type : TsReducerType.values()) {
            if (type.getToken().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return new RawTsReducer(name);
    }

}
