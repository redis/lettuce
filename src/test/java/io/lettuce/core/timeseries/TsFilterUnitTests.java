/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TsFilter}: the expression of every factory (TIMESERIES-CANONICAL.md §2.6, design §3.3) and argument
 * validation.
 */
@Tag(UNIT_TEST)
class TsFilterUnitTests {

    /**
     * Scenario A20: {@code equal("a","1")} → {@code a=1}.
     */
    @Test
    void equal() {
        assertThat(TsFilter.equal("a", "1")).hasToString("a=1");
    }

    /**
     * Scenario A20: {@code notEqual("a","1")} → {@code a!=1}.
     */
    @Test
    void notEqual() {
        assertThat(TsFilter.notEqual("a", "1")).hasToString("a!=1");
    }

    /**
     * Scenario A20: {@code exists("a")} → {@code a!=}.
     */
    @Test
    void exists() {
        assertThat(TsFilter.exists("a")).hasToString("a!=");
    }

    /**
     * Scenario A20: {@code notExists("a")} → {@code a=}.
     */
    @Test
    void notExists() {
        assertThat(TsFilter.notExists("a")).hasToString("a=");
    }

    /**
     * Scenario A20: {@code in("a","1","2")} → {@code a=(1,2)}; a single value → {@code a=(1)}.
     */
    @Test
    void in() {
        assertThat(TsFilter.in("a", "1", "2")).hasToString("a=(1,2)");
        assertThat(TsFilter.in("a", "1")).hasToString("a=(1)");
    }

    /**
     * Scenario A20: {@code notIn("a","1","2")} → {@code a!=(1,2)}.
     */
    @Test
    void notIn() {
        assertThat(TsFilter.notIn("a", "1", "2")).hasToString("a!=(1,2)");
    }

    /**
     * Scenario A20: {@code raw("x=y")} → {@code x=y}, sent exactly as given (quoted values included).
     */
    @Test
    void raw() {
        assertThat(TsFilter.raw("x=y")).hasToString("x=y");
        assertThat(TsFilter.raw("room=\"living room\"")).hasToString("room=\"living room\"");
        assertThat(TsFilter.raw("sensor=(temp,hum)")).hasToString("sensor=(temp,hum)");
    }

    /**
     * Scenario A20: empty {@code in} / {@code notIn} values → {@link IllegalArgumentException}.
     */
    @Test
    void emptyValuesThrow() {

        assertThatThrownBy(() -> TsFilter.in("a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.notIn("a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.in("a", (String[]) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.notIn("a", (String[]) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.in("a", "1", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.notIn("a", null, "2")).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A13 (filters): a {@code null} or empty label, value or expression → {@link IllegalArgumentException}, per the
     * Javadoc of each factory.
     */
    @Test
    void nullOrEmptyArgumentsThrow() {

        assertThatThrownBy(() -> TsFilter.equal(null, "1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.equal("", "1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.equal("a", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.equal("a", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.notEqual(null, "1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.notEqual("a", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.exists(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.exists("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.notExists(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.notExists("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.in(null, "1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.notIn("", "1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.raw(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsFilter.raw("")).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A20: filters are values: equal expressions are equal, whichever factory built them.
     */
    @Test
    void equality() {

        assertThat(TsFilter.equal("a", "1")).isEqualTo(TsFilter.raw("a=1")).hasSameHashCodeAs(TsFilter.raw("a=1"));
        assertThat(TsFilter.in("a", "1", "2")).isEqualTo(TsFilter.raw("a=(1,2)"));
        assertThat(TsFilter.equal("a", "1")).isNotEqualTo(TsFilter.notEqual("a", "1"));
        assertThat(TsFilter.exists("a")).isNotEqualTo(TsFilter.notExists("a"));
    }

}
