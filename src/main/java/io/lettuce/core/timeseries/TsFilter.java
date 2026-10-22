/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import io.lettuce.core.internal.LettuceAssert;

/**
 * One label filter expression of the TimeSeries label-index commands ({@code TS.MGET}, {@code TS.MRANGE}, {@code TS.MREVRANGE},
 * {@code TS.QUERYINDEX}, {@code TS.QUERYLABELS}). Several filters in one call are combined with AND.
 * <p>
 * The server requires at least one filter that matches a value ({@link #equal(String, String)} or
 * {@link #in(String, String...)}); Lettuce does not check this.
 * <p>
 * The factories do not quote or escape: label names and values must not contain whitespace, {@code =}, {@code !}, {@code (},
 * {@code )}, {@code ,} or a quote ({@code "} or {@code '}), because the server would read them as filter syntax. The factories
 * reject such names and values with {@link IllegalArgumentException}. For them, write the expression with {@link #raw(String)},
 * for example {@code TsFilter.raw("room=\"living room\"")}. The {@link #toString()} form is the expression sent to the server.
 * {@link TsFilter} is immutable.
 *
 * @since 8.0
 */
public final class TsFilter {

    private static final String SYNTAX = "=!(),\"'";

    private final String expression;

    private TsFilter(String expression) {
        this.expression = expression;
    }

    /**
     * Match series whose label {@code label} equals {@code value} ({@code label=value}).
     *
     * @param label the label name, must not be {@code null} or empty and must not contain filter syntax characters.
     * @param value the value, must not be {@code null} or empty and must not contain filter syntax characters.
     * @return the filter.
     * @throws IllegalArgumentException if {@code label} or {@code value} is {@code null} or empty, or contains whitespace,
     *         {@code =}, {@code !}, {@code (}, {@code )}, {@code ,} or a quote.
     * @since 8.0
     */
    public static TsFilter equal(String label, String value) {

        return new TsFilter(label(label) + "=" + value(value));
    }

    /**
     * Match series whose label {@code label} does not equal {@code value}, including series without the label
     * ({@code label!=value}).
     *
     * @param label the label name, must not be {@code null} or empty and must not contain filter syntax characters.
     * @param value the value, must not be {@code null} or empty and must not contain filter syntax characters.
     * @return the filter.
     * @throws IllegalArgumentException if {@code label} or {@code value} is {@code null} or empty, or contains whitespace,
     *         {@code =}, {@code !}, {@code (}, {@code )}, {@code ,} or a quote.
     * @since 8.0
     */
    public static TsFilter notEqual(String label, String value) {

        return new TsFilter(label(label) + "!=" + value(value));
    }

    /**
     * Match series that have the label {@code label} ({@code label!=}).
     *
     * @param label the label name, must not be {@code null} or empty and must not contain filter syntax characters.
     * @return the filter.
     * @throws IllegalArgumentException if {@code label} is {@code null} or empty, or contains whitespace, {@code =}, {@code !},
     *         {@code (}, {@code )}, {@code ,} or a quote.
     * @since 8.0
     */
    public static TsFilter exists(String label) {

        return new TsFilter(label(label) + "!=");
    }

    /**
     * Match series that do not have the label {@code label} ({@code label=}).
     *
     * @param label the label name, must not be {@code null} or empty and must not contain filter syntax characters.
     * @return the filter.
     * @throws IllegalArgumentException if {@code label} is {@code null} or empty, or contains whitespace, {@code =}, {@code !},
     *         {@code (}, {@code )}, {@code ,} or a quote.
     * @since 8.0
     */
    public static TsFilter notExists(String label) {

        return new TsFilter(label(label) + "=");
    }

    /**
     * Match series whose label {@code label} equals one of {@code values} ({@code label=(v1,v2)}).
     *
     * @param label the label name, must not be {@code null} or empty and must not contain filter syntax characters.
     * @param values the values, must not be {@code null} or empty, must not contain {@code null} elements, and no value may
     *        contain filter syntax characters.
     * @return the filter.
     * @throws IllegalArgumentException if {@code label} or {@code values} is {@code null} or empty, {@code values} contains
     *         {@code null} elements, or {@code label} or a value contains whitespace, {@code =}, {@code !}, {@code (},
     *         {@code )}, {@code ,} or a quote.
     * @since 8.0
     */
    public static TsFilter in(String label, String... values) {
        return new TsFilter(label(label) + "=(" + join(values) + ")");
    }

    /**
     * Match series whose label {@code label} equals none of {@code values}, including series without the label
     * ({@code label!=(v1,v2)}).
     *
     * @param label the label name, must not be {@code null} or empty and must not contain filter syntax characters.
     * @param values the values, must not be {@code null} or empty, must not contain {@code null} elements, and no value may
     *        contain filter syntax characters.
     * @return the filter.
     * @throws IllegalArgumentException if {@code label} or {@code values} is {@code null} or empty, {@code values} contains
     *         {@code null} elements, or {@code label} or a value contains whitespace, {@code =}, {@code !}, {@code (},
     *         {@code )}, {@code ,} or a quote.
     * @since 8.0
     */
    public static TsFilter notIn(String label, String... values) {
        return new TsFilter(label(label) + "!=(" + join(values) + ")");
    }

    /**
     * Use {@code expression} as given, for syntax the other factories do not cover (for example quoted values).
     *
     * @param expression the filter expression, must not be {@code null} or empty.
     * @return the filter.
     * @throws IllegalArgumentException if {@code expression} is {@code null} or empty.
     * @since 8.0
     */
    public static TsFilter raw(String expression) {

        LettuceAssert.notEmpty(expression, "Expression must not be empty");

        return new TsFilter(expression);
    }

    private static String label(String label) {

        LettuceAssert.notEmpty(label, "Label must not be empty");
        assertNoSyntax(label, "Label");

        return label;
    }

    private static String value(String value) {

        LettuceAssert.notEmpty(value, "Value must not be empty");
        assertNoSyntax(value, "Value");

        return value;
    }

    private static String join(String... values) {

        LettuceAssert.notEmpty(values, "Values must not be empty");
        LettuceAssert.noNullElements(values, "Values must not contain null elements");
        for (String value : values) {
            assertNoSyntax(value, "Value");
        }

        return String.join(",", values);
    }

    private static void assertNoSyntax(String text, String what) {

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            LettuceAssert.isTrue(!Character.isWhitespace(c) && SYNTAX.indexOf(c) < 0, () -> what + " '" + text
                    + "' must not contain whitespace, '=', '!', '(', ')', ',' or quotes; use TsFilter.raw(String) instead");
        }
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof TsFilter)) {
            return false;
        }

        return expression.equals(((TsFilter) o).expression);
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }

    /**
     * Return the expression sent to the server, for example {@code area=(north,south)}.
     *
     * @return the filter expression.
     */
    @Override
    public String toString() {
        return expression;
    }

}
