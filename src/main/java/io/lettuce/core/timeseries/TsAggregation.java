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
 * Aggregation function of a TimeSeries compaction rule or of a range query ({@code AGGREGATION}).
 * <p>
 * Pass a constant of {@link TsAggregationType}, or {@link #of(String)} for an aggregation this Lettuce version does not know
 * yet. TS.INFO returns the aggregation of a compaction rule as a {@link TsAggregationType} constant when it is known, and as a
 * value that keeps the server's name otherwise.
 *
 * @see TsAggregationType
 * @since 8.0
 */
public interface TsAggregation extends ProtocolKeyword {

    /**
     * Return the aggregation for {@code name}. A known name, ignoring case, returns the {@link TsAggregationType} constant (for
     * example {@code of("avg")} returns {@link TsAggregationType#AVG} and {@code of("std.p")} returns
     * {@link TsAggregationType#STD_P}). Any other name returns a value that is sent to the server as given. Lettuce does not
     * check such names; the server rejects names it does not support.
     *
     * @param name the aggregation name as the server spells it, must not be empty.
     * @return the aggregation.
     * @throws IllegalArgumentException if {@code name} is {@code null} or empty.
     */
    static TsAggregation of(String name) {

        LettuceAssert.notEmpty(name, "Name must not be empty");

        for (TsAggregationType type : TsAggregationType.values()) {
            if (type.getToken().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return new RawTsAggregation(name);
    }

}
