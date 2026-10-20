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
 *
 * @see TsDuplicatePolicyType
 * @since 8.0
 */
public interface TsDuplicatePolicy extends ProtocolKeyword {

    /**
     * Return the duplicate policy for {@code name}. A known name, ignoring case, returns the {@link TsDuplicatePolicyType}
     * constant (for example {@code of("last")} returns {@link TsDuplicatePolicyType#LAST}). Any other name returns a value that
     * is sent to the server as given. Lettuce does not check such names; the server rejects names it does not support.
     *
     * @param name the policy name as the server spells it, must not be empty.
     * @return the duplicate policy.
     * @throws IllegalArgumentException if {@code name} is {@code null} or empty.
     */
    static TsDuplicatePolicy of(String name) {

        LettuceAssert.notEmpty(name, "Name must not be empty");

        for (TsDuplicatePolicyType type : TsDuplicatePolicyType.values()) {
            if (type.getToken().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return new RawTsDuplicatePolicy(name);
    }

}
