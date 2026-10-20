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
 * Unit tests for {@link TsDuplicatePolicy} and {@link TsDuplicatePolicyType}.
 */
@Tag(UNIT_TEST)
class TsDuplicatePolicyUnitTests {

    private static String wire(TsDuplicatePolicy policy) {
        return new String(policy.getBytes(), StandardCharsets.US_ASCII);
    }

    /**
     * Scenario U02 / U02c: known names in any case return the constant (TS.INFO reports them in lowercase).
     */
    @Test
    void lookupByWireToken() {

        assertThat(TsDuplicatePolicy.of("block")).isSameAs(TsDuplicatePolicyType.BLOCK);
        assertThat(TsDuplicatePolicy.of("BLOCK")).isSameAs(TsDuplicatePolicyType.BLOCK);
        assertThat(TsDuplicatePolicy.of("first")).isSameAs(TsDuplicatePolicyType.FIRST);
        assertThat(TsDuplicatePolicy.of("Last")).isSameAs(TsDuplicatePolicyType.LAST);
        assertThat(TsDuplicatePolicy.of("min")).isSameAs(TsDuplicatePolicyType.MIN);
        assertThat(TsDuplicatePolicy.of("max")).isSameAs(TsDuplicatePolicyType.MAX);
        assertThat(TsDuplicatePolicy.of("sum")).isSameAs(TsDuplicatePolicyType.SUM);
        assertThat(TsDuplicatePolicyType.values()).hasSize(6);
    }

    /**
     * Scenario U02: wire tokens.
     */
    @Test
    void wireTokens() {

        for (TsDuplicatePolicyType type : TsDuplicatePolicyType.values()) {
            assertThat(wire(type)).isEqualTo(type.name());
            assertThat(TsDuplicatePolicy.of(wire(type))).isSameAs(type);
        }
    }

    /**
     * Scenario U02: lookup under the Turkish locale.
     */
    @Test
    void lookupUnderTurkishLocale() {

        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));

            assertThat(TsDuplicatePolicy.of("min")).isSameAs(TsDuplicatePolicyType.MIN);
            assertThat(TsDuplicatePolicy.of("first")).isSameAs(TsDuplicatePolicyType.FIRST);
            assertThat(TsDuplicatePolicy.of("FIRST")).isSameAs(TsDuplicatePolicyType.FIRST);
            assertThat(wire(TsDuplicatePolicyType.MIN)).isEqualTo("MIN");
        } finally {
            Locale.setDefault(previous);
        }
    }

    /**
     * Scenario U02c: an unknown name becomes a raw value that keeps its name; equality by exact name.
     */
    @Test
    void unknownNameBecomesRawValue() {

        TsDuplicatePolicy keep = TsDuplicatePolicy.of("keep");

        assertThat(keep).isNotInstanceOf(TsDuplicatePolicyType.class);
        assertThat(keep.toString()).isEqualTo("keep");
        assertThat(wire(keep)).isEqualTo("keep");
        assertThat(keep).isEqualTo(TsDuplicatePolicy.of("keep"));
        assertThat(keep.hashCode()).isEqualTo(TsDuplicatePolicy.of("keep").hashCode());
        assertThat(keep).isNotEqualTo(TsDuplicatePolicy.of("KEEP"));
    }

    @Test
    void nullOrEmptyNameThrows() {

        assertThatThrownBy(() -> TsDuplicatePolicy.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsDuplicatePolicy.of("")).isInstanceOf(IllegalArgumentException.class);
    }

}
