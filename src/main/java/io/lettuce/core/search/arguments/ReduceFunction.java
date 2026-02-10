/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import java.nio.charset.StandardCharsets;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Enumeration of REDUCE functions for GROUPBY operations in FT.HYBRID and FT.AGGREGATE commands.
 *
 * @author Aleksandar Todorov
 * @since 7.2
 * @see Reducer
 * @see GroupBy
 */
@Experimental
public enum ReduceFunction implements ProtocolKeyword {

    /** Count the number of records in the group. */
    COUNT,

    /** Count unique values of a field. */
    COUNT_DISTINCT,

    /** Approximate count of unique values using HyperLogLog algorithm. */
    COUNT_DISTINCTISH,

    /** Sum all numeric values of a field. */
    SUM,

    /** Calculate the average of numeric values. */
    AVG,

    /** Find the minimum value. */
    MIN,

    /** Find the maximum value. */
    MAX,

    /** Calculate standard deviation. */
    STDDEV,

    /** Calculate quantile/percentile (e.g., median at 0.5). */
    QUANTILE,

    /** Collect all values into a list. */
    TOLIST,

    /** Get the first value in the group. */
    FIRST_VALUE,

    /** Random sampling from the group. */
    RANDOM_SAMPLE;

    private final byte[] bytes;

    ReduceFunction() {
        this.bytes = name().getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

}
