/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.timeseries.TsReplyTestSupport.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link TsSeriesParser} (design §4, shape P8). The replies are the bytes captured from Redis 8.10.0
 * (redislabs/client-libs-test:8.10.0, 2026-10-20) after {@code TS.CREATE {ts}:a LABELS t x sensor temp area north},
 * {@code TS.CREATE {ts}:b LABELS t x sensor hum}, {@code TS.CREATE {ts}:e LABELS t x}, {@code TS.ADD {ts}:a 1000 1},
 * {@code TS.ADD {ts}:a 2000 2.5}, {@code TS.ADD {ts}:b 1000 3}. The variants with unknown metadata are derived from them.
 */
@Tag(UNIT_TEST)
class TsSeriesParserUnitTests {

    /**
     * {@code TS.MRANGE - + FILTER t=x} on RESP2.
     */
    static final String RESP2_RAW = "*3\r\n*3\r\n$6\r\n{ts}:a\r\n*0\r\n*2\r\n*2\r\n:1000\r\n+1\r\n*2\r\n:2000\r\n+2.5\r\n"
            + "*3\r\n$6\r\n{ts}:b\r\n*0\r\n*1\r\n*2\r\n:1000\r\n+3\r\n*3\r\n$6\r\n{ts}:e\r\n*0\r\n*0\r\n";

    /**
     * {@code TS.MRANGE - + FILTER t=x} on RESP3: 8.10.0 sends an empty {@code aggregators} metadata map even without
     * AGGREGATION.
     */
    static final String RESP3_RAW = "%3\r\n$6\r\n{ts}:a\r\n*3\r\n%0\r\n%1\r\n$11\r\naggregators\r\n*0\r\n"
            + "*2\r\n*2\r\n:1000\r\n,1\r\n*2\r\n:2000\r\n,2.5\r\n$6\r\n{ts}:b\r\n*3\r\n%0\r\n%1\r\n$11\r\naggregators\r\n*0\r\n"
            + "*1\r\n*2\r\n:1000\r\n,3\r\n$6\r\n{ts}:e\r\n*3\r\n%0\r\n%1\r\n$11\r\naggregators\r\n*0\r\n*0\r\n";

    /**
     * {@code TS.MRANGE - + WITHLABELS AGGREGATION avg 10000 FILTER t=x} on RESP2.
     */
    static final String RESP2_AVG = "*3\r\n*3\r\n$6\r\n{ts}:a\r\n*3\r\n*2\r\n$1\r\nt\r\n$1\r\nx\r\n*2\r\n$6\r\nsensor\r\n"
            + "$4\r\ntemp\r\n*2\r\n$4\r\narea\r\n$5\r\nnorth\r\n*1\r\n*2\r\n:0\r\n+1.75\r\n*3\r\n$6\r\n{ts}:b\r\n*2\r\n"
            + "*2\r\n$1\r\nt\r\n$1\r\nx\r\n*2\r\n$6\r\nsensor\r\n$3\r\nhum\r\n*1\r\n*2\r\n:0\r\n+3\r\n*3\r\n$6\r\n{ts}:e\r\n"
            + "*1\r\n*2\r\n$1\r\nt\r\n$1\r\nx\r\n*0\r\n";

    /**
     * {@code TS.MRANGE - + WITHLABELS AGGREGATION avg 10000 FILTER t=x} on RESP3, with the {@code aggregators} metadata.
     */
    static final String RESP3_AVG = "%3\r\n$6\r\n{ts}:a\r\n*3\r\n%3\r\n$1\r\nt\r\n$1\r\nx\r\n$6\r\nsensor\r\n$4\r\ntemp\r\n"
            + "$4\r\narea\r\n$5\r\nnorth\r\n%1\r\n$11\r\naggregators\r\n*1\r\n$3\r\navg\r\n*1\r\n*2\r\n:0\r\n,1.75\r\n"
            + "$6\r\n{ts}:b\r\n*3\r\n%2\r\n$1\r\nt\r\n$1\r\nx\r\n$6\r\nsensor\r\n$3\r\nhum\r\n%1\r\n$11\r\naggregators\r\n"
            + "*1\r\n$3\r\navg\r\n*1\r\n*2\r\n:0\r\n,3\r\n$6\r\n{ts}:e\r\n*3\r\n%1\r\n$1\r\nt\r\n$1\r\nx\r\n"
            + "%1\r\n$11\r\naggregators\r\n*1\r\n$3\r\navg\r\n*0\r\n";

    /**
     * {@code TS.MRANGE - + AGGREGATION min,max 10000 FILTER t=x} on RESP2.
     */
    static final String RESP2_MIN_MAX = "*3\r\n*3\r\n$6\r\n{ts}:a\r\n*0\r\n*1\r\n*3\r\n:0\r\n+1\r\n+2.5\r\n"
            + "*3\r\n$6\r\n{ts}:b\r\n*0\r\n*1\r\n*3\r\n:0\r\n+3\r\n+3\r\n*3\r\n$6\r\n{ts}:e\r\n*0\r\n*0\r\n";

    /**
     * {@code TS.MRANGE - + AGGREGATION min,max 10000 FILTER t=x} on RESP3.
     */
    static final String RESP3_MIN_MAX = "%3\r\n$6\r\n{ts}:a\r\n*3\r\n%0\r\n%1\r\n$11\r\naggregators\r\n*2\r\n$3\r\nmin\r\n"
            + "$3\r\nmax\r\n*1\r\n*3\r\n:0\r\n,1\r\n,2.5\r\n$6\r\n{ts}:b\r\n*3\r\n%0\r\n%1\r\n$11\r\naggregators\r\n*2\r\n"
            + "$3\r\nmin\r\n$3\r\nmax\r\n*1\r\n*3\r\n:0\r\n,3\r\n,3\r\n$6\r\n{ts}:e\r\n*3\r\n%0\r\n%1\r\n$11\r\naggregators\r\n"
            + "*2\r\n$3\r\nmin\r\n$3\r\nmax\r\n*0\r\n";

    /**
     * {@code TS.MRANGE - + WITHLABELS FILTER t=x GROUPBY t REDUCE sum} on RESP2: {@code __reducer__} and {@code __source__}
     * come as labels.
     */
    static final String RESP2_GROUP_WITHLABELS = "*1\r\n*3\r\n$3\r\nt=x\r\n*3\r\n*2\r\n$1\r\nt\r\n$1\r\nx\r\n"
            + "*2\r\n$11\r\n__reducer__\r\n$3\r\nsum\r\n*2\r\n$10\r\n__source__\r\n$20\r\n{ts}:a,{ts}:b,{ts}:e\r\n"
            + "*2\r\n*2\r\n:1000\r\n+4\r\n*2\r\n:2000\r\n+2.5\r\n";

    /**
     * {@code TS.MRANGE - + WITHLABELS FILTER t=x GROUPBY t REDUCE sum} on RESP3: {@code reducers} and {@code sources} metadata.
     */
    static final String RESP3_GROUP_WITHLABELS = "%1\r\n$3\r\nt=x\r\n*4\r\n%1\r\n$1\r\nt\r\n$1\r\nx\r\n"
            + "%1\r\n$8\r\nreducers\r\n*1\r\n$3\r\nsum\r\n%1\r\n$7\r\nsources\r\n*3\r\n$6\r\n{ts}:a\r\n$6\r\n{ts}:b\r\n"
            + "$6\r\n{ts}:e\r\n*2\r\n*2\r\n:1000\r\n,4\r\n*2\r\n:2000\r\n,2.5\r\n";

    /**
     * {@code TS.MRANGE - + FILTER t=x GROUPBY t REDUCE sum} on RESP2: no labels, so no source information.
     */
    static final String RESP2_GROUP = "*1\r\n*3\r\n$3\r\nt=x\r\n*0\r\n*2\r\n*2\r\n:1000\r\n+4\r\n*2\r\n:2000\r\n+2.5\r\n";

    /**
     * {@code TS.MRANGE - + FILTER t=x GROUPBY t REDUCE sum} on RESP3.
     */
    static final String RESP3_GROUP = "%1\r\n$3\r\nt=x\r\n*4\r\n%0\r\n%1\r\n$8\r\nreducers\r\n*1\r\n$3\r\nsum\r\n"
            + "%1\r\n$7\r\nsources\r\n*3\r\n$6\r\n{ts}:a\r\n$6\r\n{ts}:b\r\n$6\r\n{ts}:e\r\n"
            + "*2\r\n*2\r\n:1000\r\n,4\r\n*2\r\n:2000\r\n,2.5\r\n";

    /**
     * {@code TS.MRANGE - + SELECTED_LABELS area FILTER t=x GROUPBY t REDUCE max} on RESP2: the selected label differs across
     * the group, so it is {@code null}.
     */
    static final String RESP2_GROUP_SELECTED = "*1\r\n*3\r\n$3\r\nt=x\r\n*1\r\n*2\r\n$4\r\narea\r\n$-1\r\n"
            + "*2\r\n*2\r\n:1000\r\n+3\r\n*2\r\n:2000\r\n+2.5\r\n";

    /**
     * Same as {@link #RESP2_GROUP_SELECTED} on RESP3.
     */
    static final String RESP3_GROUP_SELECTED = "%1\r\n$3\r\nt=x\r\n*4\r\n%1\r\n$4\r\narea\r\n_\r\n%1\r\n$8\r\nreducers\r\n"
            + "*1\r\n$3\r\nmax\r\n%1\r\n$7\r\nsources\r\n*3\r\n$6\r\n{ts}:a\r\n$6\r\n{ts}:b\r\n$6\r\n{ts}:e\r\n"
            + "*2\r\n*2\r\n:1000\r\n,3\r\n*2\r\n:2000\r\n,2.5\r\n";

    private static final List<TsAggregation> NONE = Collections.emptyList();

    private static List<TsSeries<String>> parseRaw(String resp) {
        return parse(resp, StringCodec.UTF8, new TsSeriesParser<>(StringCodec.UTF8, NONE, false, null));
    }

    private static List<TsSeries<String>> parseAggregated(String resp, TsAggregation... aggregations) {
        return parse(resp, StringCodec.UTF8, new TsSeriesParser<>(StringCodec.UTF8, Arrays.asList(aggregations), false, null));
    }

    private static List<TsSeries<String>> parseGrouped(String resp, TsReducer reducer) {
        return parse(resp, StringCodec.UTF8, new TsSeriesParser<>(StringCodec.UTF8, NONE, true, reducer));
    }

    // --- U09 ---------------------------------------------------------------------------------------------------------

    /**
     * Scenario U09: raw MRANGE, RESP2 and RESP3: one entry per series, in reply order; the RESP3 {@code aggregators} metadata
     * map is not turned into labels; the samples are the last element.
     */
    @Test
    void raw() {

        for (String resp : new String[] { RESP2_RAW, RESP3_RAW }) {
            List<TsSeries<String>> series = parseRaw(resp);
            assertThat(series).hasSize(3);

            TsSeries<String> a = series.get(0);
            assertThat(a.isGrouped()).isFalse();
            assertThat(a.getKey()).isEqualTo("{ts}:a");
            assertThat(a.getGroup()).isNull();
            assertThat(a.getLabels()).isNotNull().isEmpty();
            assertThat(a.getAggregations()).isEmpty();
            assertThat(a.getReducer()).isNull();
            assertThat(a.getSources()).isNull();
            assertThat(a.getSamples()).containsExactly(TsSample.of(1000, 1.0), TsSample.of(2000, 2.5));

            assertThat(series.get(1).getKey()).isEqualTo("{ts}:b");
            assertThat(series.get(1).getSamples()).containsExactly(TsSample.of(1000, 3.0));
            assertThat(series.get(2).getKey()).isEqualTo("{ts}:e");
            assertThat(series.get(2).getSamples()).isNotNull().isEmpty();
        }
    }

    /**
     * Scenario U09: one aggregation with WITHLABELS, RESP2 and RESP3: real labels only (no {@code aggregators} entry),
     * {@code getAggregations()} from the request.
     */
    @Test
    void aggregationWithLabels() {

        for (String resp : new String[] { RESP2_AVG, RESP3_AVG }) {
            List<TsSeries<String>> series = parseAggregated(resp, TsAggregationType.AVG);
            assertThat(series).hasSize(3);
            assertThat(series.get(0).getLabels()).containsExactly(entry("t", "x"), entry("sensor", "temp"),
                    entry("area", "north"));
            assertThat(series.get(0).getAggregations()).containsExactly(TsAggregationType.AVG);
            assertThat(series.get(0).getSamples()).containsExactly(TsSample.of(0, 1.75));
            assertThat(series.get(1).getLabels()).containsExactly(entry("t", "x"), entry("sensor", "hum"));
            assertThat(series.get(1).getSamples()).containsExactly(TsSample.of(0, 3.0));
            assertThat(series.get(2).getLabels()).containsExactly(entry("t", "x"));
            assertThat(series.get(2).getSamples()).isEmpty();
        }
    }

    /**
     * Scenario U09: several aggregations, RESP2 and RESP3: multi-value samples, {@code getAggregations()} = the request's list.
     */
    @Test
    void severalAggregations() {

        for (String resp : new String[] { RESP2_MIN_MAX, RESP3_MIN_MAX }) {
            List<TsSeries<String>> series = parseAggregated(resp, TsAggregationType.MIN, TsAggregationType.MAX);
            assertThat(series).hasSize(3);
            assertThat(series.get(0).getLabels()).isEmpty();
            assertThat(series.get(0).getAggregations()).containsExactly(TsAggregationType.MIN, TsAggregationType.MAX);
            assertThat(series.get(0).getSamples()).containsExactly(TsSample.of(0, Arrays.asList(1.0, 2.5)));
            assertThat(series.get(1).getSamples()).containsExactly(TsSample.of(0, Arrays.asList(3.0, 3.0)));
        }
    }

    /**
     * Scenario U09 / NF.9: an unknown aggregation name in the request is kept as given in {@code getAggregations()}; the
     * metadata the server sends does not change it.
     */
    @Test
    void aggregationsComeFromTheRequest() {

        TsAggregation median = TsAggregation.of("median");
        List<TsSeries<String>> series = parseAggregated(RESP3_AVG, median);

        assertThat(series.get(0).getAggregations()).containsExactly(median);
        assertThat(series.get(0).getLabels()).doesNotContainKey("aggregators");
    }

    // --- U10 ---------------------------------------------------------------------------------------------------------

    /**
     * Scenario U10: GROUPBY on RESP3: the group name {@code t=x}, the {@code sources} metadata as sources, the reducer from the
     * request; {@code reducers}/{@code sources} never in the labels.
     */
    @Test
    void groupResp3() {

        for (String resp : new String[] { RESP3_GROUP, RESP3_GROUP_WITHLABELS }) {
            List<TsSeries<String>> series = parseGrouped(resp, TsReducerType.SUM);
            assertThat(series).hasSize(1);

            TsSeries<String> group = series.get(0);
            assertThat(group.isGrouped()).isTrue();
            assertThat(group.getKey()).isNull();
            assertThat(group.getGroup()).isEqualTo("t=x");
            assertThat(group.getReducer()).isSameAs(TsReducerType.SUM);
            assertThat(group.getAggregations()).isEmpty();
            assertThat(group.getSources()).containsExactly("{ts}:a", "{ts}:b", "{ts}:e");
            assertThat(group.getLabels()).doesNotContainKeys("reducers", "sources", "__reducer__", "__source__");
            assertThat(group.getSamples()).containsExactly(TsSample.of(1000, 4.0), TsSample.of(2000, 2.5));
        }
        assertThat(parseGrouped(RESP3_GROUP, TsReducerType.SUM).get(0).getLabels()).isEmpty();
        assertThat(parseGrouped(RESP3_GROUP_WITHLABELS, TsReducerType.SUM).get(0).getLabels()).containsExactly(entry("t", "x"));
    }

    /**
     * Scenario U10: GROUPBY on RESP2 with WITHLABELS: sources from {@code __source__}; {@code __reducer__} and
     * {@code __source__} removed from the labels.
     */
    @Test
    void groupResp2WithLabels() {

        TsSeries<String> group = parseGrouped(RESP2_GROUP_WITHLABELS, TsReducerType.SUM).get(0);

        assertThat(group.isGrouped()).isTrue();
        assertThat(group.getKey()).isNull();
        assertThat(group.getGroup()).isEqualTo("t=x");
        assertThat(group.getReducer()).isSameAs(TsReducerType.SUM);
        assertThat(group.getSources()).containsExactly("{ts}:a", "{ts}:b", "{ts}:e");
        assertThat(group.getLabels()).containsExactly(entry("t", "x"));
        assertThat(group.getSamples()).containsExactly(TsSample.of(1000, 4.0), TsSample.of(2000, 2.5));
    }

    /**
     * Scenario U10: GROUPBY on RESP2 without WITHLABELS: no source information, so {@code getSources()} is {@code null}.
     */
    @Test
    void groupResp2WithoutLabels() {

        TsSeries<String> group = parseGrouped(RESP2_GROUP, TsReducerType.SUM).get(0);

        assertThat(group.isGrouped()).isTrue();
        assertThat(group.getGroup()).isEqualTo("t=x");
        assertThat(group.getSources()).isNull();
        assertThat(group.getLabels()).isEmpty();
        assertThat(group.getReducer()).isSameAs(TsReducerType.SUM);
        assertThat(group.getSamples()).containsExactly(TsSample.of(1000, 4.0), TsSample.of(2000, 2.5));
    }

    /**
     * Scenario U10: GROUPBY with SELECTED_LABELS: a label that differs across the group is {@code null}, on both protocols.
     */
    @Test
    void groupSelectedLabels() {

        for (String resp : new String[] { RESP2_GROUP_SELECTED, RESP3_GROUP_SELECTED }) {
            TsSeries<String> group = parseGrouped(resp, TsReducerType.MAX).get(0);
            assertThat(group.getLabels()).containsExactly(entry("area", null));
            assertThat(group.getSamples()).containsExactly(TsSample.of(1000, 3.0), TsSample.of(2000, 2.5));
        }
    }

    /**
     * Scenario U10 / NF.9: an unknown RESP3 metadata map between the labels and the samples is skipped, both without and with
     * GROUPBY; an unknown reducer in the request is reported as given.
     */
    @Test
    void unknownMetadataIsSkipped() {

        String raw = RESP3_RAW.replace("*3\r\n%0\r\n%1\r\n$11\r\naggregators\r\n*0\r\n",
                "*4\r\n%0\r\n%1\r\n$11\r\naggregators\r\n*0\r\n%1\r\n$6\r\nfuture\r\n:7\r\n");
        assertThat(parseRaw(raw)).isEqualTo(parseRaw(RESP3_RAW));

        String grouped = RESP3_GROUP.replace("*4\r\n%0\r\n", "*5\r\n%0\r\n%1\r\n$6\r\nfuture\r\n*1\r\n$1\r\nz\r\n");
        TsReducer median = TsReducer.of("median");
        TsSeries<String> group = parseGrouped(grouped, median).get(0);
        assertThat(group.getSources()).containsExactly("{ts}:a", "{ts}:b", "{ts}:e");
        assertThat(group.getLabels()).isEmpty();
        assertThat(group.getReducer()).isEqualTo(median);
        assertThat(group.getSamples()).containsExactly(TsSample.of(1000, 4.0), TsSample.of(2000, 2.5));
    }

    // --- U12 ---------------------------------------------------------------------------------------------------------

    /**
     * Scenario U12 (MRANGE): each RESP2/RESP3 pair gives equal models with equal hash codes (NF.1).
     */
    @Test
    void resp2AndResp3Equal() {

        assertEqual(parseRaw(RESP2_RAW), parseRaw(RESP3_RAW));
        assertEqual(parseAggregated(RESP2_AVG, TsAggregationType.AVG), parseAggregated(RESP3_AVG, TsAggregationType.AVG));
        assertEqual(parseAggregated(RESP2_MIN_MAX, TsAggregationType.MIN, TsAggregationType.MAX),
                parseAggregated(RESP3_MIN_MAX, TsAggregationType.MIN, TsAggregationType.MAX));
        assertEqual(parseGrouped(RESP2_GROUP_WITHLABELS, TsReducerType.SUM),
                parseGrouped(RESP3_GROUP_WITHLABELS, TsReducerType.SUM));
    }

    /**
     * Scenario U12 (the documented exception, H7): GROUPBY without WITHLABELS (no labels, or SELECTED_LABELS) differs between
     * RESP2 and RESP3 only in {@code getSources()}.
     */
    @Test
    void groupWithoutLabelsDiffersOnlyInSources() {

        assertDiffersOnlyInSources(parseGrouped(RESP2_GROUP, TsReducerType.SUM).get(0),
                parseGrouped(RESP3_GROUP, TsReducerType.SUM).get(0));
        assertDiffersOnlyInSources(parseGrouped(RESP2_GROUP_SELECTED, TsReducerType.MAX).get(0),
                parseGrouped(RESP3_GROUP_SELECTED, TsReducerType.MAX).get(0));
    }

    private static void assertDiffersOnlyInSources(TsSeries<String> fromResp2, TsSeries<String> fromResp3) {

        assertThat(fromResp2.getSources()).isNull();
        assertThat(fromResp3.getSources()).isNotNull();
        assertThat(fromResp2.isGrouped()).isEqualTo(fromResp3.isGrouped());
        assertThat(fromResp2.getKey()).isEqualTo(fromResp3.getKey());
        assertThat(fromResp2.getGroup()).isEqualTo(fromResp3.getGroup());
        assertThat(fromResp2.getLabels()).isEqualTo(fromResp3.getLabels());
        assertThat(fromResp2.getAggregations()).isEqualTo(fromResp3.getAggregations());
        assertThat(fromResp2.getReducer()).isEqualTo(fromResp3.getReducer());
        assertThat(fromResp2.getSamples()).isEqualTo(fromResp3.getSamples());
    }

    private static void assertEqual(List<TsSeries<String>> fromResp2, List<TsSeries<String>> fromResp3) {
        assertThat(fromResp2).isEqualTo(fromResp3);
        assertThat(fromResp2.hashCode()).isEqualTo(fromResp3.hashCode());
    }

    // --- codec and empty replies -------------------------------------------------------------------------------------

    /**
     * Scenario U09: keys and sources are decoded with the connection codec; the group name and labels as UTF-8 (design §3.5).
     */
    @Test
    void byteArrayCodec() {

        List<TsSeries<byte[]>> raw = parse(RESP3_RAW, ByteArrayCodec.INSTANCE,
                new TsSeriesParser<>(ByteArrayCodec.INSTANCE, NONE, false, null));
        assertThat(raw.get(0).getKey()).isEqualTo("{ts}:a".getBytes(StandardCharsets.UTF_8));

        for (String resp : new String[] { RESP2_GROUP_WITHLABELS, RESP3_GROUP_WITHLABELS }) {
            TsSeries<byte[]> group = parse(resp, ByteArrayCodec.INSTANCE,
                    new TsSeriesParser<>(ByteArrayCodec.INSTANCE, NONE, true, TsReducerType.SUM)).get(0);
            assertThat(group.getGroup()).isEqualTo("t=x");
            assertThat(group.getSources()).hasSize(3);
            assertThat(group.getSources().get(1)).isEqualTo("{ts}:b".getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Scenario U09: an empty reply → an empty list on both protocols.
     */
    @Test
    void emptyReply() {

        assertThat(parseRaw("*0\r\n")).isEmpty();
        assertThat(parseRaw("%0\r\n")).isEmpty();
    }

    /**
     * The parser rejects a {@code null} codec or aggregation list.
     */
    @Test
    void nullArgumentsThrow() {

        assertThatThrownBy(() -> new TsSeriesParser<>(null, NONE, false, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsSeriesParser<>(StringCodec.UTF8, null, false, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
