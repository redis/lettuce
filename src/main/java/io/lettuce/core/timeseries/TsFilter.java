/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import io.lettuce.core.internal.LettuceAssert;

/**
 * A single label filter expression for time-series index and multi-key lookup commands, such as
 * <a href="https://redis.io/commands/ts.queryindex/">TS.QUERYINDEX</a> and <a href="https://redis.io/commands/ts.mget/">
 * TS.MGET</a>.
 * <p>
 * {@link TsFilter} is a thin, immutable wrapper around a single FILTER expression string; it does not escape or quote label or
 * value contents, and it does not enforce that at least one filter in a given call is an equality filter, both of which are
 * validated by the server.
 *
 * @since 7.8
 */
public class TsFilter {

    private final String expression;

    private TsFilter(String expression) {
        this.expression = expression;
    }

    /**
     * Creates a filter that matches series where {@code label} is set to exactly {@code value}.
     *
     * @param label the label name. Must not be {@code null} or empty.
     * @param value the label value.
     * @return the {@link TsFilter}.
     */
    public static TsFilter equal(String label, String value) {

        LettuceAssert.notEmpty(label, "Label must not be empty");
        LettuceAssert.notNull(value, "Value must not be null");

        return new TsFilter(label + "=" + value);
    }

    /**
     * Creates a filter that matches series where {@code label} is set to a value other than {@code value}.
     *
     * @param label the label name. Must not be {@code null} or empty.
     * @param value the label value.
     * @return the {@link TsFilter}.
     */
    public static TsFilter notEqual(String label, String value) {

        LettuceAssert.notEmpty(label, "Label must not be empty");
        LettuceAssert.notNull(value, "Value must not be null");

        return new TsFilter(label + "!=" + value);
    }

    /**
     * Creates a filter that matches series where {@code label} is set, regardless of its value.
     * <p>
     * Per the <a href="https://redis.io/commands/ts.queryindex/">TS.QUERYINDEX</a> FILTER syntax, "label is present" is
     * expressed as {@code label!=} (an inequality against the empty string), not {@code label=}.
     *
     * @param label the label name. Must not be {@code null} or empty.
     * @return the {@link TsFilter}.
     */
    public static TsFilter exists(String label) {

        LettuceAssert.notEmpty(label, "Label must not be empty");

        return new TsFilter(label + "!=");
    }

    /**
     * Creates a filter that matches series where {@code label} is not set.
     * <p>
     * Per the <a href="https://redis.io/commands/ts.queryindex/">TS.QUERYINDEX</a> FILTER syntax, "label is absent" is
     * expressed as {@code label=} (an equality against the empty string), not {@code label!=}.
     *
     * @param label the label name. Must not be {@code null} or empty.
     * @return the {@link TsFilter}.
     */
    public static TsFilter notExists(String label) {

        LettuceAssert.notEmpty(label, "Label must not be empty");

        return new TsFilter(label + "=");
    }

    /**
     * Creates a filter that matches series where {@code label} is set to one of the given {@code values}.
     *
     * @param label the label name. Must not be {@code null} or empty.
     * @param values the candidate label values. Must not be {@code null} or empty.
     * @return the {@link TsFilter}.
     */
    public static TsFilter in(String label, String... values) {

        LettuceAssert.notEmpty(label, "Label must not be empty");
        LettuceAssert.notEmpty(values, "Values must not be empty");

        return new TsFilter(label + "=(" + String.join(",", values) + ")");
    }

    /**
     * Creates a filter that matches series where {@code label} is not set to any of the given {@code values}.
     *
     * @param label the label name. Must not be {@code null} or empty.
     * @param values the excluded label values. Must not be {@code null} or empty.
     * @return the {@link TsFilter}.
     */
    public static TsFilter notIn(String label, String... values) {

        LettuceAssert.notEmpty(label, "Label must not be empty");
        LettuceAssert.notEmpty(values, "Values must not be empty");

        return new TsFilter(label + "!=(" + String.join(",", values) + ")");
    }

    /**
     * Creates a filter from a raw FILTER expression, unmodified. Use this as an escape hatch for syntax not covered by the
     * other factory methods.
     *
     * @param expression the raw filter expression. Must not be {@code null} or empty.
     * @return the {@link TsFilter}.
     */
    public static TsFilter raw(String expression) {

        LettuceAssert.notEmpty(expression, "Expression must not be empty");

        return new TsFilter(expression);
    }

    /**
     * Returns the FILTER expression this {@link TsFilter} represents, exactly as sent on the wire.
     *
     * @return the FILTER expression.
     */
    public String toExpression() {
        return expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TsFilter))
            return false;

        TsFilter that = (TsFilter) o;

        return expression != null ? expression.equals(that.expression) : that.expression == null;
    }

    @Override
    public int hashCode() {
        return expression != null ? expression.hashCode() : 0;
    }

    @Override
    public String toString() {
        return String.format("TsFilter[%s]", expression);
    }

}
