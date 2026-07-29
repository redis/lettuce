/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ComplexData;

/**
 * Unit tests that close the defensive-branch coverage gaps identified for {@link TsInfoValueParser} and {@link TsSampleParser}:
 * branches that are logically correct but were never exercised by the existing parser unit tests because those tests only ever
 * fed {@code Double}/{@code String} encoded values.
 * <p>
 * PLAN (Given/When/Then):
 * <ul>
 * <li>Given {@code ignoreMaxValDiff} arrives as a {@code Long} (not {@code Double}, not a bulk string), when
 * {@code TsInfoValueParser} parses it, then the {@code Number && !Double} branch of {@code toNullableDouble} converts it via
 * {@code doubleValue()}.</li>
 * <li>Given an N-tuple value arrives as {@code Long} (not {@code Double}, not a bulk string), when {@code TsSampleParser}
 * parses it, then the {@code Number && !Double} branch of {@code toDouble} converts it via {@code doubleValue()}.</li>
 * <li>Given a {@code rules} aggregation type string that does not match any {@link TsAggregationType} constant, when
 * {@code TsInfoValueParser} parses it, then {@code decodeAggregationType} silently returns {@code null} instead of
 * throwing.</li>
 * </ul>
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
class TsParserBranchCoverageUnitTests {

    private final TsInfoValueParser<String, String> infoParser = new TsInfoValueParser<>(StringCodec.UTF8);

    private final TsSampleParser sampleParser = TsSampleParser.INSTANCE;

    private static ByteBuffer buf(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Simulates a RESP2 flat key-value array: {@code isList()==true}, but {@code getDynamicMap()} is still available via the
     * odd/even heuristic (matching {@code ArrayComplexData}). Copied from {@code TsInfoValueParserUnitTests} since the builder
     * is {@code private static} there.
     */
    private static ComplexData flatMapData(Object... pairs) {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length - 1; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        List<Object> list = Arrays.asList(pairs);
        return new ComplexData() {

            @Override
            public void storeObject(Object value) {
            }

            @Override
            public List<Object> getDynamicList() {
                return list;
            }

            @Override
            public Map<Object, Object> getDynamicMap() {
                return map;
            }

            @Override
            public boolean isList() {
                return true;
            }

        };
    }

    /**
     * Simulates a nested array (used for the {@code rules} tuple and for {@code TS.GET}-shaped N-tuples): {@code
     * isList()==true}. Copied from {@code TsInfoValueParserUnitTests}/{@code TsSampleParserUnitTests} since the builder is
     * {@code private static} there.
     */
    private static ComplexData listData(Object... items) {
        List<Object> list = Arrays.asList(items);
        return new ComplexData() {

            @Override
            public void storeObject(Object value) {
            }

            @Override
            public List<Object> getDynamicList() {
                return list;
            }

            @Override
            public boolean isList() {
                return true;
            }

        };
    }

    // ---------------------------------------------------------------------------
    // A-1: TsInfoValueParser#toNullableDouble, Number-but-not-Double (Long) branch
    // ---------------------------------------------------------------------------

    @Test
    void ignoreMaxValDiffAsLongHitsNumberNotDoubleBranch() {
        ComplexData data = flatMapData(buf("ignoreMaxValDiff"), 0L);

        TsInfoValue<String> value = infoParser.parse(data);

        assertThat(value.getIgnoreMaxValDiff()).isEqualTo(0.0);
    }

    // ---------------------------------------------------------------------------
    // A-2: TsSampleParser#toDouble, Number-but-not-Double (Long) branch
    // ---------------------------------------------------------------------------

    @Test
    void multiAggregatorValuesAsLongHitNumberNotDoubleBranch() {
        TsSample sample = sampleParser.parse(listData(1000L, 10L, 20L));

        assertThat(sample.getValues()).containsExactly(10.0, 20.0);
    }

    // ---------------------------------------------------------------------------
    // A-3: TsInfoValueParser#decodeAggregationType, unknown enum string -> silent null
    // ---------------------------------------------------------------------------

    @Test
    void unknownAggregationTypeDecodesToNullInsteadOfThrowing() {
        ComplexData rules = listData(listData(buf("dest-key"), 60000L, buf("UNKNOWN_AGG"), 0L));
        ComplexData data = flatMapData(buf("rules"), rules);

        TsInfoValue<String> value = infoParser.parse(data);

        assertThat(value.getRules()).hasSize(1);
        TsInfoValue.Rule<String> rule = value.getRules().get(0);
        assertThat(rule.getDestKey()).isEqualTo("dest-key");
        assertThat(rule.getAggregationType()).isNull();
    }

}
