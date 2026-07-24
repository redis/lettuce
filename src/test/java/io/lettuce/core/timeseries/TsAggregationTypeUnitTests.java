/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TsAggregationType}.
 * <p>
 * PLAN:
 * <ul>
 * <li>Given a plain aggregator (e.g. {@code AVG}), when {@link TsAggregationType#getBytes()}/{@code toString()} are read, then
 * the wire value equals the enum constant name.</li>
 * <li>Given a dotted aggregator (e.g. {@code STD_P}), when read, then the wire value is the dotted form ({@code STD.P}), not
 * the Java identifier.</li>
 * <li>All 15 values from the design contract must be present.</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class TsAggregationTypeUnitTests {

    @Test
    void shouldExposeFifteenValues() {
        assertThat(TsAggregationType.values()).hasSize(15);
    }

    @Test
    void shouldRenderPlainNameAsWireValue() {
        assertThat(TsAggregationType.AVG.toString()).isEqualTo("AVG");
        assertThat(new String(TsAggregationType.AVG.getBytes(), StandardCharsets.US_ASCII)).isEqualTo("AVG");
    }

    @Test
    void shouldRenderDottedWireValueForStdP() {
        assertThat(TsAggregationType.STD_P.toString()).isEqualTo("STD.P");
        assertThat(new String(TsAggregationType.STD_P.getBytes(), StandardCharsets.US_ASCII)).isEqualTo("STD.P");
    }

    @Test
    void shouldRenderDottedWireValueForStdS() {
        assertThat(TsAggregationType.STD_S.toString()).isEqualTo("STD.S");
    }

    @Test
    void shouldRenderDottedWireValueForVarP() {
        assertThat(TsAggregationType.VAR_P.toString()).isEqualTo("VAR.P");
    }

    @Test
    void shouldRenderDottedWireValueForVarS() {
        assertThat(TsAggregationType.VAR_S.toString()).isEqualTo("VAR.S");
    }

    @Test
    void shouldRenderRemainingPlainValues() {
        assertThat(TsAggregationType.SUM.toString()).isEqualTo("SUM");
        assertThat(TsAggregationType.MIN.toString()).isEqualTo("MIN");
        assertThat(TsAggregationType.MAX.toString()).isEqualTo("MAX");
        assertThat(TsAggregationType.RANGE.toString()).isEqualTo("RANGE");
        assertThat(TsAggregationType.COUNT.toString()).isEqualTo("COUNT");
        assertThat(TsAggregationType.FIRST.toString()).isEqualTo("FIRST");
        assertThat(TsAggregationType.LAST.toString()).isEqualTo("LAST");
        assertThat(TsAggregationType.TWA.toString()).isEqualTo("TWA");
        assertThat(TsAggregationType.COUNTNAN.toString()).isEqualTo("COUNTNAN");
        assertThat(TsAggregationType.COUNTALL.toString()).isEqualTo("COUNTALL");
    }

}
