/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TsFilter}.
 *
 * <p>
 * PLAN (Given/When/Then):
 * <ul>
 * <li>Given a label and a value, when {@code TsFilter.equal(label, value)}, then {@code toExpression()} returns
 * {@code "label=value"}.</li>
 * <li>Given a label and a value, when {@code TsFilter.notEqual(label, value)}, then {@code toExpression()} returns
 * {@code "label!=value"}.</li>
 * <li>Given a label, when {@code TsFilter.exists(label)}, then {@code toExpression()} returns {@code "label!="} (the server's
 * FILTER syntax uses {@code !=} to mean "label is present").</li>
 * <li>Given a label, when {@code TsFilter.notExists(label)}, then {@code toExpression()} returns {@code "label="} (the server's
 * FILTER syntax uses {@code =} to mean "label is absent").</li>
 * <li>Given a label and two or more values, when {@code TsFilter.in(label, values...)}, then {@code toExpression()} returns
 * {@code "label=(v1,v2)"}.</li>
 * <li>Given a label and two or more values, when {@code TsFilter.notIn(label, values...)}, then {@code toExpression()} returns
 * {@code "label!=(v1,v2)"}.</li>
 * <li>Given an arbitrary expression string, when {@code TsFilter.raw(expression)}, then {@code toExpression()} returns that
 * string unchanged.</li>
 * <li>Given two {@link TsFilter} instances that produce the same {@code toExpression()} result, when compared, then they are
 * {@code equal} and share the same {@code hashCode()}.</li>
 * <li>Given a {@code null} or empty label, when any factory method is called, then an {@link IllegalArgumentException} is
 * thrown.</li>
 * <li>Given a {@code null} or empty expression, when {@code TsFilter.raw(expression)} is called, then an
 * {@link IllegalArgumentException} is thrown.</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class TsFilterUnitTests {

    @Test
    void equalBuildsEqualityExpression() {
        assertThat(TsFilter.equal("region", "us").toExpression()).isEqualTo("region=us");
    }

    @Test
    void notEqualBuildsInequalityExpression() {
        assertThat(TsFilter.notEqual("region", "us").toExpression()).isEqualTo("region!=us");
    }

    @Test
    void existsBuildsNotEqualsEmptyExpression() {
        assertThat(TsFilter.exists("region").toExpression()).isEqualTo("region!=");
    }

    @Test
    void notExistsBuildsEqualsEmptyExpression() {
        assertThat(TsFilter.notExists("region").toExpression()).isEqualTo("region=");
    }

    @Test
    void inBuildsListMatchExpression() {
        assertThat(TsFilter.in("region", "us", "eu").toExpression()).isEqualTo("region=(us,eu)");
    }

    @Test
    void notInBuildsListNotMatchExpression() {
        assertThat(TsFilter.notIn("region", "us", "eu").toExpression()).isEqualTo("region!=(us,eu)");
    }

    @Test
    void rawReturnsExpressionUnchanged() {
        assertThat(TsFilter.raw("region=(us,eu)").toExpression()).isEqualTo("region=(us,eu)");
    }

    @Test
    void instancesWithSameExpressionAreEqual() {
        TsFilter first = TsFilter.equal("region", "us");
        TsFilter second = TsFilter.raw("region=us");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equalRejectsNullLabel() {
        assertThatThrownBy(() -> TsFilter.equal(null, "us")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalRejectsEmptyLabel() {
        assertThatThrownBy(() -> TsFilter.equal("", "us")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void existsRejectsEmptyLabel() {
        assertThatThrownBy(() -> TsFilter.exists("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inRejectsEmptyLabel() {
        assertThatThrownBy(() -> TsFilter.in("", "us", "eu")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rawRejectsNullExpression() {
        assertThatThrownBy(() -> TsFilter.raw(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rawRejectsEmptyExpression() {
        assertThatThrownBy(() -> TsFilter.raw("")).isInstanceOf(IllegalArgumentException.class);
    }

}
