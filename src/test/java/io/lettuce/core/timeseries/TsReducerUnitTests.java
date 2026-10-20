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

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TsReducer} and {@link TsReducerType} (design §3.1).
 */
@Tag(UNIT_TEST)
class TsReducerUnitTests {

    private static String wire(TsReducer reducer) {
        return new String(reducer.getBytes(), StandardCharsets.US_ASCII);
    }

    /**
     * Scenario U02c (reducer): the constants of design §3.1, and no FIRST, LAST or TWA.
     */
    @Test
    void constants() {

        assertThat(TsReducerType.values()).containsExactly(TsReducerType.SUM, TsReducerType.MIN, TsReducerType.MAX,
                TsReducerType.AVG, TsReducerType.RANGE, TsReducerType.COUNT, TsReducerType.COUNTNAN, TsReducerType.COUNTALL,
                TsReducerType.STD_P, TsReducerType.STD_S, TsReducerType.VAR_P, TsReducerType.VAR_S);
    }

    /**
     * Scenario U02c (reducer): the wire tokens; {@code STD_P} is sent as {@code STD.P}.
     */
    @Test
    void wireTokens() {

        assertThat(wire(TsReducerType.SUM)).isEqualTo("SUM");
        assertThat(wire(TsReducerType.AVG)).isEqualTo("AVG");
        assertThat(wire(TsReducerType.RANGE)).isEqualTo("RANGE");
        assertThat(wire(TsReducerType.COUNTNAN)).isEqualToIgnoringCase("countNaN");
        assertThat(wire(TsReducerType.COUNTALL)).isEqualToIgnoringCase("countAll");
        assertThat(wire(TsReducerType.STD_P)).isEqualTo("STD.P");
        assertThat(wire(TsReducerType.STD_S)).isEqualTo("STD.S");
        assertThat(wire(TsReducerType.VAR_P)).isEqualTo("VAR.P");
        assertThat(wire(TsReducerType.VAR_S)).isEqualTo("VAR.S");
    }

    /**
     * Scenario U02c (reducer): a known name, ignoring case, returns the constant itself; lookup by wire token, never by
     * {@code name()}.
     */
    @Test
    void knownNameReturnsConstant() {

        assertThat(TsReducer.of("sum")).isSameAs(TsReducerType.SUM);
        assertThat(TsReducer.of("SUM")).isSameAs(TsReducerType.SUM);
        assertThat(TsReducer.of("std.p")).isSameAs(TsReducerType.STD_P);
        assertThat(TsReducer.of("Var.S")).isSameAs(TsReducerType.VAR_S);
        assertThat(TsReducer.of("countNaN")).isSameAs(TsReducerType.COUNTNAN);
        assertThat(TsReducer.of("COUNTALL")).isSameAs(TsReducerType.COUNTALL);
        for (TsReducerType type : TsReducerType.values()) {
            assertThat(TsReducer.of(wire(type))).isSameAs(type);
            assertThat(TsReducer.of(wire(type).toLowerCase(Locale.ROOT))).isSameAs(type);
        }

        assertThat(TsReducer.of("std_p")).isNotInstanceOf(TsReducerType.class);
        assertThat(TsReducer.of("VAR_S")).isNotInstanceOf(TsReducerType.class);
    }

    /**
     * Scenario U02c (reducer): the lookup works under the Turkish locale (dotless i in {@code min}, {@code countNaN}).
     */
    @Test
    void lookupUnderTurkishLocale() {

        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));

            assertThat(TsReducer.of("min")).isSameAs(TsReducerType.MIN);
            assertThat(TsReducer.of("MIN")).isSameAs(TsReducerType.MIN);
            assertThat(TsReducer.of("countnan")).isSameAs(TsReducerType.COUNTNAN);
            assertThat(TsReducer.of("COUNTNAN")).isSameAs(TsReducerType.COUNTNAN);
            assertThat(wire(TsReducerType.MIN)).isEqualTo("MIN");
        } finally {
            Locale.setDefault(previous);
        }
    }

    /**
     * Scenario U02c (reducer): an unknown name becomes a raw value that keeps the name, is sent as given and compares by the
     * exact name; {@code first}, which the server rejects as a reducer, is such a raw value.
     */
    @Test
    void unknownNameBecomesRawValue() {

        TsReducer median = TsReducer.of("median");

        assertThat(median).isNotInstanceOf(TsReducerType.class);
        assertThat(median).hasToString("median");
        assertThat(wire(median)).isEqualTo("median");
        assertThat(median).isEqualTo(TsReducer.of("median"));
        assertThat(median.hashCode()).isEqualTo(TsReducer.of("median").hashCode());
        assertThat(median).isNotEqualTo(TsReducer.of("MEDIAN"));
        assertThat(TsReducer.of("first")).isNotInstanceOf(TsReducerType.class);
    }

    /**
     * Scenario U02c (reducer): {@code of(null)} and {@code of("")} → {@link IllegalArgumentException}.
     */
    @Test
    void nullOrEmptyNameThrows() {

        assertThatThrownBy(() -> TsReducer.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsReducer.of("")).isInstanceOf(IllegalArgumentException.class);
    }

}
