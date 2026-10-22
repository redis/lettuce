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
 * Policy for handling a sample whose timestamp already exists in a TimeSeries ({@code DUPLICATE_POLICY}, {@code ON_DUPLICATE}).
 * <p>
 * Pass a constant of {@link TsDuplicatePolicyType}, or {@link #of(String)} for a policy this Lettuce version does not know yet.
 * TS.INFO returns the duplicate policy as a {@link TsDuplicatePolicyType} constant when it is known, and as a value that keeps
 * the server's name otherwise.
 * <p>
 * This interface is implemented by Lettuce: by the constants of {@link TsDuplicatePolicyType} and by the values that
 * {@link #of(String)} returns. It is not meant to be implemented by applications.
 *
 * @author Aleksandar Todorov
 * @see TsDuplicatePolicyType
 * @since 8.0
 */
public interface TsDuplicatePolicy extends ProtocolKeyword {

    /**
     * Return the duplicate policy for {@code name}. A known name, ignoring case, returns the {@link TsDuplicatePolicyType}
     * constant (for example {@code of("last")} returns {@link TsDuplicatePolicyType#LAST}). Any other name returns a value that
     * is sent to the server as given. Lettuce only checks that such a name contains no {@code ,} and no whitespace; the server
     * rejects names it does not support.
     *
     * @param name the policy name as the server spells it, must not be empty and must not contain {@code ,} or whitespace.
     * @return the duplicate policy.
     * @throws IllegalArgumentException if {@code name} is {@code null} or empty, or contains {@code ,} or whitespace.
     * @since 8.0
     */
    static TsDuplicatePolicy of(String name) {

        LettuceAssert.notEmpty(name, "Name must not be empty");
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            LettuceAssert.isTrue(c != ',' && !Character.isWhitespace(c), "Name must not contain ',' or whitespace");
        }

        for (TsDuplicatePolicyType type : TsDuplicatePolicyType.values()) {
            if (type.getToken().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return new RawTsDuplicatePolicy(name);
    }

}
