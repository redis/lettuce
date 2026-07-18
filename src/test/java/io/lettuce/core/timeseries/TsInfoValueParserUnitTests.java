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
 * Unit tests for {@link TsInfoValueParser}.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
class TsInfoValueParserUnitTests {

    private final TsInfoValueParser<String, String> parser = new TsInfoValueParser<>(StringCodec.UTF8);

    // ---------------------------------------------------------------------------
    // Test data builders
    // ---------------------------------------------------------------------------

    private static ByteBuffer buf(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Simulates a RESP2 flat key-value array: {@code isList()==true}, but {@code getDynamicMap()} is still available via the
     * odd/even heuristic (matching {@code ArrayComplexData}).
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
     * Simulates a RESP3 native map: {@code isMap()==true}.
     */
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

    /**
     * Simulates a nested array (used for RESP2 rule tuples and RESP3 rule value tuples): {@code isList()==true}.
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
    // Given: null data, When: parse, Then: reject
    // ---------------------------------------------------------------------------

    @Test
    void parseNullThrows() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TS.INFO");
    }

    // ---------------------------------------------------------------------------
    // Given: RESP2 flat array, When: parse, Then: same TsInfoValue as RESP3 map
    // ---------------------------------------------------------------------------

    @Test
    void parsesResp2FlatArray() {
        ComplexData data = flatMapData(buf("totalSamples"), 100L, buf("memoryUsage"), 4096L, buf("firstTimestamp"), 1000L,
                buf("lastTimestamp"), 2000L, buf("retentionTime"), 60000L, buf("chunkCount"), 2L, buf("chunkSize"), 4096L,
                buf("chunkType"), buf("compressed"), buf("duplicatePolicy"), buf("last"), buf("labels"),
                listData(listData(buf("region"), buf("us"))), buf("sourceKey"), buf("src-key"), buf("rules"), listData(),
                buf("ignoreMaxTimeDiff"), 10L, buf("ignoreMaxValDiff"), buf("0.5"));

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getTotalSamples()).isEqualTo(100L);
        assertThat(value.getMemoryUsage()).isEqualTo(4096L);
        assertThat(value.getFirstTimestamp()).isEqualTo(1000L);
        assertThat(value.getLastTimestamp()).isEqualTo(2000L);
        assertThat(value.getRetentionTime()).isEqualTo(60000L);
        assertThat(value.getChunkCount()).isEqualTo(2L);
        assertThat(value.getChunkSize()).isEqualTo(4096L);
        assertThat(value.getChunkType()).isEqualTo("compressed");
        assertThat(value.getDuplicatePolicy()).isEqualTo("last");
        assertThat(value.getLabels()).containsEntry("region", "us");
        assertThat(value.getSourceKey()).isEqualTo("src-key");
        assertThat(value.getRules()).isEmpty();
        assertThat(value.getIgnoreMaxTimeDiff()).isEqualTo(10L);
        assertThat(value.getIgnoreMaxValDiff()).isEqualTo(0.5);
    }

    @Test
    void parsesResp3NativeMap() {
        ComplexData data = mapData(buf("totalSamples"), 100L, buf("memoryUsage"), 4096L, buf("firstTimestamp"), 1000L,
                buf("lastTimestamp"), 2000L, buf("retentionTime"), 60000L, buf("chunkCount"), 2L, buf("chunkSize"), 4096L,
                buf("chunkType"), buf("compressed"), buf("duplicatePolicy"), buf("last"), buf("labels"),
                mapData(buf("region"), buf("us")), buf("sourceKey"), buf("src-key"), buf("rules"), mapData(),
                buf("ignoreMaxTimeDiff"), 10L, buf("ignoreMaxValDiff"), 0.5d);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getTotalSamples()).isEqualTo(100L);
        assertThat(value.getChunkType()).isEqualTo("compressed");
        assertThat(value.getDuplicatePolicy()).isEqualTo("last");
        assertThat(value.getLabels()).containsEntry("region", "us");
        assertThat(value.getSourceKey()).isEqualTo("src-key");
        assertThat(value.getRules()).isEmpty();
        assertThat(value.getIgnoreMaxValDiff()).isEqualTo(0.5);
    }

    // ---------------------------------------------------------------------------
    // Given: duplicatePolicy DP_NONE (server sends real null), Then: getDuplicatePolicy() is null
    // ---------------------------------------------------------------------------

    @Test
    void duplicatePolicyNullWhenDpNone() {
        ComplexData data = flatMapData(buf("duplicatePolicy"), null);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getDuplicatePolicy()).isNull();
    }

    // ---------------------------------------------------------------------------
    // Given: sourceKey absent (series is not a compaction destination), Then: getSourceKey() is null
    // ---------------------------------------------------------------------------

    @Test
    void sourceKeyNullWhenNotACompactionDestination() {
        ComplexData data = flatMapData(buf("sourceKey"), null);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getSourceKey()).isNull();
    }

    // ---------------------------------------------------------------------------
    // labels: RESP2 nested array of [key, value] pairs vs RESP3 native map
    // ---------------------------------------------------------------------------

    @Test
    void labelsSingleResp2NestedPair() {
        ComplexData labels = listData(listData(buf("region"), buf("us")));
        ComplexData data = flatMapData(buf("labels"), labels);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getLabels()).containsExactly(entry("region", "us"));
    }

    @Test
    void labelsMultipleResp2NestedPairs() {
        ComplexData labels = listData(listData(buf("region"), buf("us")), listData(buf("type"), buf("temp")));
        ComplexData data = flatMapData(buf("labels"), labels);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getLabels()).containsExactly(entry("region", "us"), entry("type", "temp"));
    }

    @Test
    void labelsResp3NativeMap() {
        ComplexData labels = mapData(buf("region"), buf("us"), buf("type"), buf("temp"));
        ComplexData data = flatMapData(buf("labels"), labels);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getLabels()).containsExactly(entry("region", "us"), entry("type", "temp"));
    }

    @Test
    void labelsEmptyResp2() {
        ComplexData labels = listData();
        ComplexData data = flatMapData(buf("labels"), labels);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getLabels()).isEmpty();
    }

    @Test
    void labelsEmptyResp3() {
        ComplexData labels = mapData();
        ComplexData data = flatMapData(buf("labels"), labels);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getLabels()).isEmpty();
    }

    @Test
    void labelsAbsentYieldsEmptyMap() {
        ComplexData data = flatMapData();

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getLabels()).isEmpty();
    }

    // ---------------------------------------------------------------------------
    // rules: empty (no compaction rules) on both protocols
    // ---------------------------------------------------------------------------

    @Test
    void rulesEmptyResp2() {
        ComplexData data = flatMapData(buf("rules"), listData());

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getRules()).isNotNull().isEmpty();
    }

    @Test
    void rulesEmptyResp3() {
        ComplexData data = flatMapData(buf("rules"), mapData());

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getRules()).isNotNull().isEmpty();
    }

    // ---------------------------------------------------------------------------
    // rules: present, RESP2 4-element tuple [destKey, bucketDuration, aggType, timestampAlignment]
    // ---------------------------------------------------------------------------

    @Test
    void rulesPresentResp2FourElementTuple() {
        ComplexData rules = listData(listData(buf("dest-key"), 60000L, buf("avg"), 0L));
        ComplexData data = flatMapData(buf("rules"), rules);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getRules()).hasSize(1);
        TsInfoValue.Rule<String> rule = value.getRules().get(0);
        assertThat(rule.getDestKey()).isEqualTo("dest-key");
        assertThat(rule.getBucketDuration()).isEqualTo(60000L);
        assertThat(rule.getAggregationType()).isEqualTo(TsAggregationType.AVG);
        assertThat(rule.getTimestampAlignment()).isEqualTo(0L);
    }

    // ---------------------------------------------------------------------------
    // rules: present, RESP3 map with 3-element value tuple [bucketDuration, aggType, timestampAlignment]
    // ---------------------------------------------------------------------------

    @Test
    void rulesPresentResp3ThreeElementTuple() {
        ComplexData rules = mapData(buf("dest-key"), listData(60000L, buf("std.p"), 5L));
        ComplexData data = flatMapData(buf("rules"), rules);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getRules()).hasSize(1);
        TsInfoValue.Rule<String> rule = value.getRules().get(0);
        assertThat(rule.getDestKey()).isEqualTo("dest-key");
        assertThat(rule.getBucketDuration()).isEqualTo(60000L);
        assertThat(rule.getAggregationType()).isEqualTo(TsAggregationType.STD_P);
        assertThat(rule.getTimestampAlignment()).isEqualTo(5L);
    }

    // ---------------------------------------------------------------------------
    // ignoreMaxValDiff: RESP2 (bulk/simple string) vs RESP3 (native double)
    // ---------------------------------------------------------------------------

    @Test
    void ignoreMaxValDiffAsString() {
        ComplexData data = flatMapData(buf("ignoreMaxValDiff"), buf("1.5"));

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getIgnoreMaxValDiff()).isEqualTo(1.5);
    }

    @Test
    void ignoreMaxValDiffAsDouble() {
        ComplexData data = flatMapData(buf("ignoreMaxValDiff"), 1.5d);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getIgnoreMaxValDiff()).isEqualTo(1.5);
    }

    // ---------------------------------------------------------------------------
    // DEBUG fields: keySelfName and Chunks
    // ---------------------------------------------------------------------------

    @Test
    void debugFieldsAbsentByDefault() {
        ComplexData data = flatMapData(buf("totalSamples"), 1L);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getKeySelfName()).isNull();
        assertThat(value.getChunks()).isNull();
    }

    @Test
    void debugFieldsParsedWhenPresent() {
        ComplexData chunks = listData(flatMapData(buf("startTimestamp"), 0L, buf("endTimestamp"), 1000L, buf("samples"), 10L,
                buf("size"), 256L, buf("bytesPerSample"), buf("25.6")));
        ComplexData data = flatMapData(buf("keySelfName"), buf("self-key"), buf("Chunks"), chunks);

        TsInfoValue<String> value = parser.parse(data);

        assertThat(value.getKeySelfName()).isEqualTo("self-key");
        assertThat(value.getChunks()).hasSize(1);
        TsInfoValue.Chunk chunk = value.getChunks().get(0);
        assertThat(chunk.getStartTimestamp()).isEqualTo(0L);
        assertThat(chunk.getEndTimestamp()).isEqualTo(1000L);
        assertThat(chunk.getSamples()).isEqualTo(10L);
        assertThat(chunk.getSize()).isEqualTo(256L);
        assertThat(chunk.getBytesPerSample()).isEqualTo(25.6);
    }

}
