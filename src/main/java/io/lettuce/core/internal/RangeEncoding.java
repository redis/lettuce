/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.internal;

import io.lettuce.core.Range;

/**
 * Encodes the boundaries of a numeric {@link Range} as Redis score bounds: an unbounded side as {@code -inf} or {@code +inf}
 * and an exclusive boundary with a leading {@code (}. This class is part of the internal API and may change without further
 * notice.
 *
 * @since 8.0
 */
public final class RangeEncoding {

    private RangeEncoding() {
    }

    /**
     * Encode the lower boundary of {@code range}.
     *
     * @param range the range, must not be {@code null}.
     * @return the encoded lower boundary.
     */
    public static String min(Range<? extends Number> range) {

        Range.Boundary<? extends Number> lower = range.getLower();

        if (lower.getValue() == null) {
            return "-inf";
        }

        if (lower.getValue() instanceof Double && lower.getValue().doubleValue() == Double.NEGATIVE_INFINITY) {
            return lower.isIncluding() ? "-inf" : "(-inf";
        }

        if (!lower.isIncluding()) {
            return "(" + lower.getValue();
        }

        return lower.getValue().toString();
    }

    /**
     * Encode the upper boundary of {@code range}.
     *
     * @param range the range, must not be {@code null}.
     * @return the encoded upper boundary.
     */
    public static String max(Range<? extends Number> range) {

        Range.Boundary<? extends Number> upper = range.getUpper();

        if (upper.getValue() == null) {
            return "+inf";
        }

        if (upper.getValue() instanceof Double && upper.getValue().doubleValue() == Double.POSITIVE_INFINITY) {
            return upper.isIncluding() ? "+inf" : "(+inf";
        }

        if (!upper.isIncluding()) {
            return "(" + upper.getValue();
        }

        return upper.getValue().toString();
    }

}
