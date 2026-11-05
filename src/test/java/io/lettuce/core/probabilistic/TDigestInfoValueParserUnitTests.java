/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.lettuce.core.output.ComplexData;

/**
 * Unit tests for {@link TDigestInfoValueParser}.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
class TDigestInfoValueParserUnitTests {

    private final TDigestInfoValueParser parser = TDigestInfoValueParser.INSTANCE;

    // ---------------------------------------------------------------------------
    // Helper: build a ComplexData backed by a flat key-value list (RESP2 array)
    // ---------------------------------------------------------------------------
    private static ComplexData buildMapData(Object... pairs) {
        return new ComplexData() {

            @Override
            public void storeObject(Object value) {
                // not needed
            }

            @Override
            public java.util.Map<Object, Object> getDynamicMap() {
                java.util.LinkedHashMap<Object, Object> map = new java.util.LinkedHashMap<>();
                for (int i = 0; i < pairs.length - 1; i += 2) {
                    map.put(pairs[i], pairs[i + 1]);
                }
                return map;
            }

        };
    }

    private static ByteBuffer buf(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------------------
    // INSTANCE singleton
    // ---------------------------------------------------------------------------

    @Test
    void instanceIsSingleton() {
        assertThat(TDigestInfoValueParser.INSTANCE).isSameAs(TDigestInfoValueParser.INSTANCE);
    }

    // ---------------------------------------------------------------------------
    // null guard
    // ---------------------------------------------------------------------------

    @Test
    void parseNullThrows() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TDIGEST.INFO");
    }

    // ---------------------------------------------------------------------------
    // All 9 fields parsed correctly
    // ---------------------------------------------------------------------------

    @Test
    void parseAllNineFields() {
        ComplexData data = buildMapData(buf("Compression"), 100L, buf("Capacity"), 610L, buf("Merged nodes"), 3L,
                buf("Unmerged nodes"), 2L, buf("Merged weight"), 300L, buf("Unmerged weight"), 200L, buf("Observations"), 500L,
                buf("Total compressions"), 1L, buf("Memory usage"), 9768L);

        TDigestInfoValue value = parser.parse(data);

        assertThat(value.getCompression()).isEqualTo(100L);
        assertThat(value.getCapacity()).isEqualTo(610L);
        assertThat(value.getMergedNodes()).isEqualTo(3L);
        assertThat(value.getUnmergedNodes()).isEqualTo(2L);
        assertThat(value.getMergedWeight()).isEqualTo(300L);
        assertThat(value.getUnmergedWeight()).isEqualTo(200L);
        assertThat(value.getObservations()).isEqualTo(500L);
        assertThat(value.getTotalCompressions()).isEqualTo(1L);
        assertThat(value.getMemoryUsage()).isEqualTo(9768L);
    }

    // ---------------------------------------------------------------------------
    // rawInfo preserved with decoded String keys
    // ---------------------------------------------------------------------------

    @Test
    void rawInfoIsPreserved() {
        ComplexData data = buildMapData(buf("Compression"), 42L);
        TDigestInfoValue value = parser.parse(data);
        assertThat(value.getRawInfo()).containsKey("Compression");
        assertThat(value.getRawInfo().get("Compression")).isEqualTo(42L);
    }

    // ---------------------------------------------------------------------------
    // Missing fields yield null (not NPE)
    // ---------------------------------------------------------------------------

    @Test
    void missingFieldsYieldNull() {
        ComplexData data = buildMapData(buf("Compression"), 100L);
        TDigestInfoValue value = parser.parse(data);
        assertThat(value.getCapacity()).isNull();
        assertThat(value.getMergedNodes()).isNull();
        assertThat(value.getUnmergedNodes()).isNull();
        assertThat(value.getMergedWeight()).isNull();
        assertThat(value.getUnmergedWeight()).isNull();
        assertThat(value.getObservations()).isNull();
        assertThat(value.getTotalCompressions()).isNull();
        assertThat(value.getMemoryUsage()).isNull();
    }

    // ---------------------------------------------------------------------------
    // Empty map — all fields null, no exception
    // ---------------------------------------------------------------------------

    @Test
    void emptyMapDoesNotThrow() {
        ComplexData data = buildMapData();
        TDigestInfoValue value = parser.parse(data);
        assertThat(value).isNotNull();
        assertThat(value.getCompression()).isNull();
        assertThat(value.getRawInfo()).isEmpty();
    }

}
