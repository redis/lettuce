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

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TsFilter}: the expression of every factory and argument validation.
 *
 * @author Aleksandar Todorov
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
        assertThat(TsFilter.raw("room=living room")).hasToString("room=living room");
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
     * Scenario A20: label names and values without filter syntax are accepted as given.
     */
    @Test
    void namesAndValuesWithoutSyntaxAreAccepted() {

        assertThat(TsFilter.equal("room_1", "north-2.5:x")).hasToString("room_1=north-2.5:x");
        assertThat(TsFilter.notEqual("room_1", "north-2.5:x")).hasToString("room_1!=north-2.5:x");
        assertThat(TsFilter.exists("room_1")).hasToString("room_1!=");
        assertThat(TsFilter.notExists("room_1")).hasToString("room_1=");
        assertThat(TsFilter.in("room_1", "north-2.5:x", "s")).hasToString("room_1=(north-2.5:x,s)");
        assertThat(TsFilter.notIn("room_1", "north-2.5:x", "s")).hasToString("room_1!=(north-2.5:x,s)");
    }

    /**
     * Scenario A20: every factory rejects a label name with whitespace, {@code =}, {@code !}, {@code (}, {@code )}, {@code ,}
     * or a quote, and points to {@code raw(String)}.
     */
    @Test
    void labelWithFilterSyntaxIsRejected() {

        for (String bad : SYNTAX) {
            String label = "a" + bad + "b";
            assertRejected(() -> TsFilter.equal(label, "1"));
            assertRejected(() -> TsFilter.notEqual(label, "1"));
            assertRejected(() -> TsFilter.exists(label));
            assertRejected(() -> TsFilter.notExists(label));
            assertRejected(() -> TsFilter.in(label, "1"));
            assertRejected(() -> TsFilter.notIn(label, "1"));
        }
    }

    /**
     * Scenario A20: {@code equal}, {@code notEqual}, {@code in} and {@code notIn} reject a value with whitespace, {@code =},
     * {@code !}, {@code (}, {@code )}, {@code ,} or a quote, and point to {@code raw(String)}.
     */
    @Test
    void valueWithFilterSyntaxIsRejected() {

        for (String bad : SYNTAX) {
            String value = "v" + bad + "w";
            assertRejected(() -> TsFilter.equal("a", value));
            assertRejected(() -> TsFilter.notEqual("a", value));
            assertRejected(() -> TsFilter.in("a", "1", value));
            assertRejected(() -> TsFilter.notIn("a", "1", value));
        }
    }

    /**
     * Scenario A20: {@code raw(String)} still accepts any expression, including quotes, spaces and lists.
     */
    @Test
    void rawAcceptsFilterSyntax() {

        assertThat(TsFilter.raw("room=\"living room\"")).hasToString("room=\"living room\"");
        assertThat(TsFilter.raw("a!=(x,'y z')")).hasToString("a!=(x,'y z')");
    }

    private static final String[] SYNTAX = { " ", "\t", "=", "!", "(", ")", ",", "\"", "'" };

    private static void assertRejected(ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("TsFilter.raw(String)");
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
