/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

/**
 * Lightweight index range for Redis Array commands that require a start/end pair.
 * <p>
 * Uses primitive {@code long} fields so that {@code null} bounds are impossible at compile time. Unlike
 * {@link io.lettuce.core.Range}, this type has no inclusive/exclusive semantics — array indices are always inclusive.
 * <p>
 * Primarily used with {@code ARDELRANGE} to express one or more index ranges to delete.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
public class ArrayIndexRange {

    private final long lower;

    private final long upper;

    private ArrayIndexRange(long lower, long upper) {
        this.lower = lower;
        this.upper = upper;
    }

    /**
     * Create a bounded index range.
     *
     * @param lower the lower bound (inclusive).
     * @param upper the upper bound (inclusive).
     * @return new {@link ArrayIndexRange}.
     */
    public static ArrayIndexRange of(long lower, long upper) {
        return new ArrayIndexRange(lower, upper);
    }

    /**
     * @return the lower bound (inclusive).
     */
    public long getLower() {
        return lower;
    }

    /**
     * @return the upper bound (inclusive).
     */
    public long getUpper() {
        return upper;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ArrayIndexRange))
            return false;
        ArrayIndexRange that = (ArrayIndexRange) o;
        return lower == that.lower && upper == that.upper;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(lower) * 31 + Long.hashCode(upper);
    }

    @Override
    public String toString() {
        return "ArrayIndexRange [" + lower + " to " + upper + "]";
    }

}
