/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.timeseries.TsReplyTestSupport.decode;
import static io.lettuce.core.timeseries.TsReplyTestSupport.parse;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.TsSampleListOutput;
import io.lettuce.core.output.TsSampleOutput;

/**
 * Unit tests for {@link TsSampleParser}, the sample decoder of the complex replies (MGET, MRANGE, MREVRANGE). Scenario U13: the
 * reply cases of the dedicated outputs (U03, U04; wire bytes observed on 8.10.0) are fed to both decoders, which must give
 * equal samples (design §4).
 */
@Tag(UNIT_TEST)
class TsSampleParserUnitTests {

    /**
     * The U03 cases: one sample each, RESP2 simple strings and RESP3 doubles, including NaN and ±Infinity, a large timestamp
     * and a multi-value row.
     */
    private static final String[] SAMPLES = { "*2\r\n:1000\r\n+1\r\n", "*2\r\n:3000\r\n+4.333333333333333\r\n",
            "*2\r\n:5\r\n+-2.5\r\n", "*2\r\n:2000\r\n+NaN\r\n", "*2\r\n:1000\r\n+Infinity\r\n", "*2\r\n:1000\r\n+-Infinity\r\n",
            "*2\r\n:1000\r\n,1\r\n", "*2\r\n:1000\r\n,1.5\r\n", "*2\r\n:2000\r\n,nan\r\n", "*2\r\n:1000\r\n,inf\r\n",
            "*2\r\n:1000\r\n,-inf\r\n", "*2\r\n:1700000000123\r\n+7\r\n", "*4\r\n:0\r\n+2\r\n+3\r\n+4\r\n",
            "*4\r\n:3000\r\n,4\r\n,4.333333333333333\r\n,5\r\n" };

    private static TsSample viaParser(String resp) {
        return parse(resp, StringCodec.UTF8, data -> TsSampleParser.sample(data));
    }

    private static TsSample viaOutput(String resp) {
        return decode(resp, new TsSampleOutput<>(StringCodec.UTF8));
    }

    private static List<TsSample> listViaParser(String resp) {
        return parse(resp, StringCodec.UTF8, data -> TsSampleParser.samples(data));
    }

    private static List<TsSample> listViaOutput(String resp) {
        return decode(resp, new TsSampleListOutput<>(StringCodec.UTF8));
    }

    /**
     * Scenario U13: every U03 case gives the same sample through {@link TsSampleParser} and {@link TsSampleOutput}.
     */
    @Test
    void sameSampleAsDedicatedOutput() {

        for (String resp : SAMPLES) {
            TsSample fromParser = viaParser(resp);
            assertThat(fromParser).as(resp).isNotNull().isEqualTo(viaOutput(resp));
        }
    }

    /**
     * Scenario U13: the expected values of the U03 cases, checked on the parser path.
     */
    @Test
    void expectedValues() {

        assertThat(viaParser("*2\r\n:1000\r\n+1\r\n")).isEqualTo(TsSample.of(1000, 1.0));
        assertThat(viaParser("*2\r\n:5\r\n+-2.5\r\n")).isEqualTo(TsSample.of(5, -2.5));
        assertThat(viaParser("*2\r\n:2000\r\n+NaN\r\n").getValue()).isNaN();
        assertThat(viaParser("*2\r\n:2000\r\n,nan\r\n").getValue()).isNaN();
        assertThat(viaParser("*2\r\n:1000\r\n+Infinity\r\n").getValue()).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(viaParser("*2\r\n:1000\r\n,-inf\r\n").getValue()).isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(viaParser("*2\r\n:1700000000123\r\n+7\r\n")).isEqualTo(TsSample.of(1700000000123L, 7.0));
        assertThat(viaParser("*4\r\n:0\r\n+2\r\n+3\r\n+4\r\n")).isEqualTo(TsSample.of(0, Arrays.asList(2.0, 3.0, 4.0)));
        assertThat(viaParser("*2\r\n:1000\r\n+2.5\r\n")).isEqualTo(viaParser("*2\r\n:1000\r\n,2.5\r\n"));
    }

    /**
     * Scenario U13: an empty array (the sample of an empty series in TS.MGET) → {@code null}, as with {@link TsSampleOutput}.
     */
    @Test
    void emptyArrayIsNull() {

        assertThat(viaParser("*0\r\n")).isNull();
        assertThat(viaOutput("*0\r\n")).isNull();
    }

    /**
     * Scenario U13 (lists): the multi-aggregation reply of U04 on both protocols gives the same rows through
     * {@link TsSampleParser} and {@link TsSampleListOutput}; an empty reply gives an empty list on both paths.
     */
    @Test
    void sameListAsDedicatedOutput() {

        String resp2 = "*3\r\n*4\r\n:0\r\n+2\r\n+3\r\n+4\r\n*4\r\n:3000\r\n+4\r\n+4.333333333333333\r\n+5\r\n"
                + "*4\r\n:6000\r\n+5\r\n+7\r\n+9\r\n";
        String resp3 = resp2.replace("+", ",");
        String special = "*3\r\n*2\r\n:1\r\n+NaN\r\n*2\r\n:2\r\n,inf\r\n*2\r\n:3\r\n+-Infinity\r\n";

        List<TsSample> expected = Arrays.asList(TsSample.of(0, Arrays.asList(2.0, 3.0, 4.0)),
                TsSample.of(3000, Arrays.asList(4.0, 4.333333333333333, 5.0)), TsSample.of(6000, Arrays.asList(5.0, 7.0, 9.0)));

        assertThat(listViaParser(resp2)).isEqualTo(listViaOutput(resp2)).isEqualTo(expected);
        assertThat(listViaParser(resp3)).isEqualTo(listViaOutput(resp3)).isEqualTo(expected);
        assertThat(listViaParser(special)).isEqualTo(listViaOutput(special)).containsExactly(TsSample.of(1, Double.NaN),
                TsSample.of(2, Double.POSITIVE_INFINITY), TsSample.of(3, Double.NEGATIVE_INFINITY));
        assertThat(listViaParser("*0\r\n")).isEqualTo(listViaOutput("*0\r\n")).isEmpty();
    }

}
