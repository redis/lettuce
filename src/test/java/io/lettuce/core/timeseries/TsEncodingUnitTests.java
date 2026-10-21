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
 * Unit tests for {@link TsEncoding} and {@link TsEncodingType}.
 */
@Tag(UNIT_TEST)
class TsEncodingUnitTests {

    private static String wire(TsEncoding encoding) {
        return new String(encoding.getBytes(), StandardCharsets.US_ASCII);
    }

    /**
     * Scenario U02 / U02c: known names in any case return the constant (TS.INFO reports {@code compressed} in lowercase).
     */
    @Test
    void lookupByWireToken() {

        assertThat(TsEncoding.of("compressed")).isSameAs(TsEncodingType.COMPRESSED);
        assertThat(TsEncoding.of("COMPRESSED")).isSameAs(TsEncodingType.COMPRESSED);
        assertThat(TsEncoding.of("uncompressed")).isSameAs(TsEncodingType.UNCOMPRESSED);
        assertThat(TsEncoding.of("UnCompressed")).isSameAs(TsEncodingType.UNCOMPRESSED);
        assertThat(TsEncodingType.values()).hasSize(2);
        assertThat(wire(TsEncodingType.COMPRESSED)).isEqualTo("COMPRESSED");
        assertThat(wire(TsEncodingType.UNCOMPRESSED)).isEqualTo("UNCOMPRESSED");
    }

    /**
     * Scenario U02: {@code toString()} of every constant is its wire token.
     */
    @Test
    void toStringIsWireToken() {

        assertThat(TsEncodingType.COMPRESSED.toString()).isEqualTo("COMPRESSED");
        assertThat(TsEncodingType.UNCOMPRESSED.toString()).isEqualTo("UNCOMPRESSED");
        for (TsEncodingType type : TsEncodingType.values()) {
            assertThat(type.toString()).isEqualTo(wire(type));
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

            assertThat(TsEncoding.of("compressed")).isSameAs(TsEncodingType.COMPRESSED);
            assertThat(TsEncoding.of("uncompressed")).isSameAs(TsEncodingType.UNCOMPRESSED);
        } finally {
            Locale.setDefault(previous);
        }
    }

    /**
     * Scenario U02c: an unknown name becomes a raw value that keeps its name; equality by exact name.
     */
    @Test
    void unknownNameBecomesRawValue() {

        TsEncoding zstd = TsEncoding.of("zstd");

        assertThat(zstd).isNotInstanceOf(TsEncodingType.class);
        assertThat(zstd.toString()).isEqualTo("zstd");
        assertThat(wire(zstd)).isEqualTo("zstd");
        assertThat(zstd).isEqualTo(TsEncoding.of("zstd"));
        assertThat(zstd.hashCode()).isEqualTo(TsEncoding.of("zstd").hashCode());
        assertThat(zstd).isNotEqualTo(TsEncoding.of("ZSTD"));
    }

    @Test
    void nullOrEmptyNameThrows() {

        assertThatThrownBy(() -> TsEncoding.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsEncoding.of("")).isInstanceOf(IllegalArgumentException.class);
    }

}
