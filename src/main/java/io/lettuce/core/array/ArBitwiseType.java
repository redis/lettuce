/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

/**
 * Bitwise operations for the Redis {@code AROP} command that return an integer ({@code Long}).
 * <p>
 * These operations perform bitwise operations over array elements in a given range.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 * @see <a href="https://redis.io/docs/latest/commands/arop/">Redis Documentation: AROP</a>
 */
public enum ArBitwiseType {

    /**
     * Returns the bitwise AND of all integer values in the range.
     */
    AND,

    /**
     * Returns the bitwise OR of all integer values in the range.
     */
    OR,

    /**
     * Returns the bitwise XOR of all integer values in the range.
     */
    XOR

}
