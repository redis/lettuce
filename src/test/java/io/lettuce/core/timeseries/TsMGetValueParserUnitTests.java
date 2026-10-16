/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

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
 * Unit tests for {@link TsMGetValueParser}.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
class TsMGetValueParserUnitTests {

    private final TsMGetValueParser<String, String> parser = new TsMGetValueParser<>(StringCodec.UTF8);

    // ---------------------------------------------------------------------------
    // Test data builders
    // ---------------------------------------------------------------------------

    private static ByteBuffer buf(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

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

    private static ComplexData mapData(Object... pairs) {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length - 1; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return new ComplexData() {

            @Override
            public void storeObject(Object value) {
            }

            @Override
            public Map<Object, Object> getDynamicMap() {
                return map;
            }

            @Override
            public boolean isMap() {
                return true;
            }

        };
    }

    // ---------------------------------------------------------------------------
    // Given: null data, When: parse, Then: reject
    // ---------------------------------------------------------------------------

    @Test
    void parseNullThrows() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TS.MGET");
    }

    // ---------------------------------------------------------------------------
    // Given: RESP2 array-of-triples [key, labels, sample], When: parse, Then: same result as RESP3 map
    // ---------------------------------------------------------------------------

    @Test
    void parsesResp2ArrayOfTriples() {
        ComplexData entry = listData(buf("key1"), listData(listData(buf("region"), buf("us"))), listData(1000L, buf("1.5")));
        ComplexData data = listData(entry);

        List<TsMGetValue<String>> result = parser.parse(data);

        assertThat(result).hasSize(1);
        TsMGetValue<String> value = result.get(0);
        assertThat(value.getKey()).isEqualTo("key1");
        assertThat(value.getLabels()).containsEntry("region", "us");
        assertThat(value.getSample()).isNotNull();
        assertThat(value.getSample().getTimestamp()).isEqualTo(1000L);
        assertThat(value.getSample().getValue()).isEqualTo(1.5);
    }

    // ---------------------------------------------------------------------------
    // labels: RESP2 nested array of [key, value] pairs vs RESP3 native map
    // ---------------------------------------------------------------------------

    @Test
    void labelsSingleResp2NestedPair() {
        ComplexData entry = listData(buf("key1"), listData(listData(buf("region"), buf("us"))), listData());
        ComplexData data = listData(entry);

        List<TsMGetValue<String>> result = parser.parse(data);

        assertThat(result.get(0).getLabels()).containsExactly(entry("region", "us"));
    }

    @Test
    void labelsMultipleResp2NestedPairs() {
        ComplexData entry = listData(buf("key1"),
                listData(listData(buf("region"), buf("us")), listData(buf("type"), buf("temp"))), listData());
        ComplexData data = listData(entry);

        List<TsMGetValue<String>> result = parser.parse(data);

        assertThat(result.get(0).getLabels()).containsExactly(entry("region", "us"), entry("type", "temp"));
    }

    @Test
    void labelsResp3NativeMap() {
        ComplexData value = listData(mapData(buf("region"), buf("us"), buf("type"), buf("temp")), listData());
        ComplexData data = mapData(buf("key1"), value);

        List<TsMGetValue<String>> result = parser.parse(data);

        assertThat(result.get(0).getLabels()).containsExactly(entry("region", "us"), entry("type", "temp"));
    }

    // ---------------------------------------------------------------------------
    // Given: RESP3 map key -> [labels, sample], When: parse, Then: same result as RESP2
    // ---------------------------------------------------------------------------

    @Test
    void parsesResp3Map() {
        ComplexData value = listData(mapData(buf("region"), buf("us")), listData(1000L, 1.5d));
        ComplexData data = mapData(buf("key1"), value);

        List<TsMGetValue<String>> result = parser.parse(data);

        assertThat(result).hasSize(1);
        TsMGetValue<String> mGetValue = result.get(0);
        assertThat(mGetValue.getKey()).isEqualTo("key1");
        assertThat(mGetValue.getLabels()).containsEntry("region", "us");
        assertThat(mGetValue.getSample().getTimestamp()).isEqualTo(1000L);
        assertThat(mGetValue.getSample().getValue()).isEqualTo(1.5);
    }

    // ---------------------------------------------------------------------------
    // Given: empty top-level container, Then: empty result list (no matching keys)
    // ---------------------------------------------------------------------------

    @Test
    void emptyResp2ListYieldsEmptyResult() {
        ComplexData data = listData();

        assertThat(parser.parse(data)).isEmpty();
    }

    @Test
    void emptyResp3MapYieldsEmptyResult() {
        ComplexData data = mapData();

        assertThat(parser.parse(data)).isEmpty();
    }

    // ---------------------------------------------------------------------------
    // Given: a series with no samples (TS.GET-style empty array, not nil), Then: sample is null
    // ---------------------------------------------------------------------------

    @Test
    void emptySampleYieldsNullSample() {
        ComplexData entry = listData(buf("key1"), listData(), listData());
        ComplexData data = listData(entry);

        List<TsMGetValue<String>> result = parser.parse(data);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSample()).isNull();
    }

    // ---------------------------------------------------------------------------
    // Given: a sample with multiple aggregator values (N-tuple), Then: TsSample carries all values
    // ---------------------------------------------------------------------------

    @Test
    void multiAggregatorSampleIsPreserved() {
        ComplexData entry = listData(buf("key1"), listData(), listData(1000L, 10.0d, 1.0d, 20.0d));
        ComplexData data = listData(entry);

        List<TsMGetValue<String>> result = parser.parse(data);

        assertThat(result.get(0).getSample().getValues()).containsExactly(10.0, 1.0, 20.0);
    }

}
