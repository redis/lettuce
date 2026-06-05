/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.lettuce.core.output.ComplexData;

/**
 * Unit tests for {@link CfInfoValueParser}.
 *
 * @author HwangRock
 * @since 7.7
 */
class CfInfoValueParserUnitTests {

    private final CfInfoValueParser parser = CfInfoValueParser.INSTANCE;

    // ---------------------------------------------------------------------------
    // Helper: build a ComplexData backed by a flat key-value list (RESP2 array)
    // ---------------------------------------------------------------------------
    private static ComplexData buildMapData(Object... pairs) {
        // Use ArrayComplexData via package-accessible test stub instead.
        // We need to produce a ComplexData whose getDynamicMap() returns the pairs.
        // We'll use a simple anonymous subclass to avoid package-private visibility issues.
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
        assertThat(CfInfoValueParser.INSTANCE).isSameAs(CfInfoValueParser.INSTANCE);
    }

    // ---------------------------------------------------------------------------
    // null guard
    // ---------------------------------------------------------------------------

    @Test
    void parseNullThrows() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CF.INFO");
    }

    // ---------------------------------------------------------------------------
    // All 8 fields parsed correctly
    // ---------------------------------------------------------------------------

    @Test
    void parseAllEightFields() {
        ComplexData data = buildMapData(buf("Size"), 100L, buf("Number of buckets"), 200L, buf("Number of filters"), 1L,
                buf("Number of items inserted"), 50L, buf("Number of items deleted"), 5L, buf("Bucket size"), 2L,
                buf("Expansion rate"), 1L, buf("Max iterations"), 20L);

        CfInfoValue value = parser.parse(data);

        assertThat(value.getSize()).isEqualTo(100L);
        assertThat(value.getNumberOfBuckets()).isEqualTo(200L);
        assertThat(value.getNumberOfFilters()).isEqualTo(1L);
        assertThat(value.getNumberOfItemsInserted()).isEqualTo(50L);
        assertThat(value.getNumberOfItemsDeleted()).isEqualTo(5L);
        assertThat(value.getBucketSize()).isEqualTo(2L);
        assertThat(value.getExpansionRate()).isEqualTo(1L);
        assertThat(value.getMaxIterations()).isEqualTo(20L);
    }

    // ---------------------------------------------------------------------------
    // Plural field names — "Number of filters" and "Max iterations"
    // ---------------------------------------------------------------------------

    @Test
    void numberOfFiltersIsPlural() {
        ComplexData data = buildMapData(buf("Number of filters"), 3L);
        CfInfoValue value = parser.parse(data);
        assertThat(value.getNumberOfFilters()).isEqualTo(3L);
    }

    @Test
    void maxIterationsIsPlural() {
        ComplexData data = buildMapData(buf("Max iterations"), 15L);
        CfInfoValue value = parser.parse(data);
        assertThat(value.getMaxIterations()).isEqualTo(15L);
    }

    // ---------------------------------------------------------------------------
    // rawInfo preserved
    // ---------------------------------------------------------------------------

    @Test
    void rawInfoIsPreserved() {
        ComplexData data = buildMapData(buf("Size"), 42L);
        CfInfoValue value = parser.parse(data);
        assertThat(value.getRawInfo()).containsKey("Size");
        assertThat(value.getRawInfo().get("Size")).isEqualTo(42L);
    }

    // ---------------------------------------------------------------------------
    // Missing fields yield null (not NPE)
    // ---------------------------------------------------------------------------

    @Test
    void missingFieldsYieldNull() {
        ComplexData data = buildMapData(buf("Size"), 10L);
        CfInfoValue value = parser.parse(data);
        assertThat(value.getNumberOfBuckets()).isNull();
        assertThat(value.getNumberOfFilters()).isNull();
        assertThat(value.getNumberOfItemsInserted()).isNull();
        assertThat(value.getNumberOfItemsDeleted()).isNull();
        assertThat(value.getBucketSize()).isNull();
        assertThat(value.getExpansionRate()).isNull();
        assertThat(value.getMaxIterations()).isNull();
    }

    // ---------------------------------------------------------------------------
    // Empty map — all fields null, no exception
    // ---------------------------------------------------------------------------

    @Test
    void emptyMapDoesNotThrow() {
        ComplexData data = buildMapData();
        CfInfoValue value = parser.parse(data);
        assertThat(value).isNotNull();
        assertThat(value.getSize()).isNull();
        assertThat(value.getRawInfo()).isEmpty();
    }

}
