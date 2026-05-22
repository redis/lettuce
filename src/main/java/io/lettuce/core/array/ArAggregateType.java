/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import io.lettuce.core.annotations.Experimental;

/**
 * Aggregate operations for the Redis {@code AROP} command that return a bulk string value ({@code V}).
 * <p>
 * These operations perform numeric aggregation over array elements in a given range.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see <a href="https://redis.io/docs/latest/commands/arop/">Redis Documentation: AROP</a>
 */
@Experimental
public enum ArAggregateType {

    /**
     * Returns the sum of all numeric values in the range.
     */
    SUM,

    /**
     * Returns the minimum numeric value in the range.
     */
    MIN,

    /**
     * Returns the maximum numeric value in the range.
     */
    MAX

}
