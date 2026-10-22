/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsMGetArgs;
import io.lettuce.core.timeseries.arguments.TsMRangeArgs;
import io.lettuce.core.timeseries.arguments.TsNRangeArgs;
import io.lettuce.core.timeseries.arguments.TsRangeArgs;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.condition.RedisConditions;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * Protocol parity (NF.1): the same series read on a RESP2 and a RESP3 connection produce equal result models.
 */
@Tag(INTEGRATION_TEST)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("TS.ADD")
public class RedisTimeSeriesProtocolParityIntegrationTests {

    private RedisClient resp2Client;

    private RedisClient resp3Client;

    private StatefulRedisConnection<String, String> resp2Connection;

    private StatefulRedisConnection<String, String> resp3Connection;

    private RedisCommands<String, String> resp2;

    private RedisCommands<String, String> resp3;

    @BeforeAll
    void connect() {

        RedisURI uri = RedisURI.create(TestSettings.host(), TestSettings.port());

        resp2Client = RedisClient.create(TestClientResources.get(), uri);
        resp2Client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        resp2Connection = resp2Client.connect();
        resp2 = resp2Connection.sync();

        resp3Client = RedisClient.create(TestClientResources.get(), uri);
        resp3Client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3).build());
        resp3Connection = resp3Client.connect();
        resp3 = resp3Connection.sync();
    }

    @AfterAll
    void close() {
        resp2Connection.close();
        resp3Connection.close();
        resp2Client.shutdown();
        resp3Client.shutdown();
    }

    @BeforeEach
    void flush() {
        resp3.flushall();
    }

    /**
     * Scenario I01 (INFO): INFO of a series with default options is equal on both protocols.
     */
    @Test
    void infoDefaults() {

        resp3.tsCreate("{ts}:p1");

        assertThat(resp2.tsInfo("{ts}:p1")).isEqualTo(resp3.tsInfo("{ts}:p1"));
    }

    /**
     * Scenario I01 (INFO): INFO of a series with every option and labels is equal on both protocols.
     */
    @Test
    void infoEveryOption() {

        resp3.tsCreate("{ts}:p2",
                new TsCreateArgs().retention(5000).encoding(TsEncodingType.UNCOMPRESSED).chunkSize(128)
                        .duplicatePolicy(TsDuplicatePolicyType.LAST).ignore(10, 0.5).label("t", "infoEveryOption")
                        .label("sensor", "temp").label("area", "north"));

        TsInfoValue<String> fromResp2 = resp2.tsInfo("{ts}:p2");
        TsInfoValue<String> fromResp3 = resp3.tsInfo("{ts}:p2");

        assertThat(fromResp2).isEqualTo(fromResp3);
        assertThat(fromResp2.hashCode()).isEqualTo(fromResp3.hashCode());
        assertThat(fromResp2.getLabels()).containsExactlyEntriesOf(fromResp3.getLabels());
        assertThat(fromResp2.getIgnoreMaxValDiff()).isEqualTo(0.5);
    }

    /**
     * Scenario I01 (INFO DEBUG): INFO DEBUG, including the chunks, is equal on both protocols.
     */
    @Test
    void infoDebug() {

        resp3.tsCreate("{ts}:p3", new TsCreateArgs().chunkSize(256).label("t", "infoDebug"));

        TsInfoValue<String> fromResp2 = resp2.tsInfo("{ts}:p3", true);
        TsInfoValue<String> fromResp3 = resp3.tsInfo("{ts}:p3", true);

        assertThat(fromResp2).isEqualTo(fromResp3);
        assertThat(fromResp2.getChunks()).isEqualTo(fromResp3.getChunks()).hasSize(1);
        assertThat(fromResp2.getKeySelfName()).isEqualTo(fromResp3.getKeySelfName()).isEqualTo("{ts}:p3");
    }

    /**
     * Scenario I01 (INFO with rules): the rules list (RESP2 list of 4-element arrays, RESP3 map destination → 3 elements) and
     * the destination's {@code sourceKey} are equal on both protocols.
     */
    @Test
    void infoWithRules() {

        resp3.tsCreate("{ts}:src", new TsCreateArgs().label("t", "infoWithRules"));
        resp3.tsCreate("{ts}:d1");
        resp3.tsCreate("{ts}:d2");
        resp3.tsCreateRule("{ts}:src", "{ts}:d1", TsAggregationType.AVG, 1000);
        resp3.tsCreateRule("{ts}:src", "{ts}:d2", TsAggregationType.STD_P, 60000, 500);

        TsInfoValue<String> fromResp2 = resp2.tsInfo("{ts}:src");
        TsInfoValue<String> fromResp3 = resp3.tsInfo("{ts}:src");

        assertThat(fromResp2).isEqualTo(fromResp3);
        assertThat(fromResp2.getRules()).hasSize(2).containsExactlyInAnyOrderElementsOf(fromResp3.getRules());
        assertThat(resp2.tsInfo("{ts}:d2")).isEqualTo(resp3.tsInfo("{ts}:d2"));
        assertThat(resp2.tsInfo("{ts}:d2").getSourceKey()).isEqualTo("{ts}:src");
    }

    /**
     * Write F2 ({@code 2, 4, 4, 4, 5, 5, 7, 9} at 1000 … 8000) to {@code key} on the RESP3 connection.
     */
    private void statsSeries(String key) {

        resp3.tsCreate(key, new TsCreateArgs().label("t", "parity"));
        double[] values = { 2, 4, 4, 4, 5, 5, 7, 9 };
        for (int i = 0; i < values.length; i++) {
            resp3.tsAdd(key, (i + 1) * 1000L, values[i]);
        }
    }

    /**
     * Scenario I01 (GET): GET of a series with samples, with and without LATEST, and of a series without samples is equal on
     * both protocols.
     */
    @Test
    void get() {

        statsSeries("{ts}:g1");
        resp3.tsCreate("{ts}:g2");

        assertThat(resp2.tsGet("{ts}:g1")).isEqualTo(resp3.tsGet("{ts}:g1")).isEqualTo(TsSample.of(8000, 9.0));
        assertThat(resp2.tsGet("{ts}:g1", true)).isEqualTo(resp3.tsGet("{ts}:g1", true));
        assertThat(resp2.tsGet("{ts}:g2")).isNull();
        assertThat(resp3.tsGet("{ts}:g2")).isNull();
    }

    /**
     * Scenario I01 (GET, compaction): GET with LATEST on a compaction destination is equal on both protocols, including a
     * fractional value.
     */
    @Test
    void getLatestOnCompaction() {

        resp3.tsCreate("{ts}:src");
        resp3.tsCreate("{ts}:dst");
        resp3.tsCreateRule("{ts}:src", "{ts}:dst", TsAggregationType.AVG, 1000);
        resp3.tsAdd("{ts}:src", 1000L, 1.0);
        resp3.tsAdd("{ts}:src", 1500L, 2.0);
        resp3.tsAdd("{ts}:src", 2000L, 0.1);

        assertThat(resp2.tsGet("{ts}:dst")).isEqualTo(resp3.tsGet("{ts}:dst")).isEqualTo(TsSample.of(1000, 1.5));
        assertThat(resp2.tsGet("{ts}:dst", true)).isEqualTo(resp3.tsGet("{ts}:dst", true)).isEqualTo(TsSample.of(2000, 0.1));
    }

    /**
     * Scenario I01 (RANGE raw): RANGE and REVRANGE without options are equal on both protocols.
     */
    @Test
    void rangeRaw() {

        statsSeries("{ts}:r1");

        List<TsSample> fromResp2 = resp2.tsRange("{ts}:r1", Range.unbounded());
        assertThat(fromResp2).isEqualTo(resp3.tsRange("{ts}:r1", Range.unbounded())).hasSize(8);
        assertThat(fromResp2.hashCode()).isEqualTo(resp3.tsRange("{ts}:r1", Range.unbounded()).hashCode());
        assertThat(resp2.tsRevRange("{ts}:r1", Range.create(2000L, 4000L)))
                .isEqualTo(resp3.tsRevRange("{ts}:r1", Range.create(2000L, 4000L))).hasSize(3);
    }

    /**
     * Scenario I01 (RANGE with an aggregation): fractional bucket values (avg 13/3, std.s, var.s, twa) are equal on both
     * protocols.
     */
    @Test
    void rangeAggregated() {

        statsSeries("{ts}:r2");

        for (TsAggregationType aggregation : new TsAggregationType[] { TsAggregationType.AVG, TsAggregationType.STD_S,
                TsAggregationType.VAR_S, TsAggregationType.TWA }) {
            TsRangeArgs args = new TsRangeArgs().aggregation(3000, aggregation);
            assertThat(resp2.tsRange("{ts}:r2", Range.unbounded(), args)).as(aggregation.toString())
                    .isEqualTo(resp3.tsRange("{ts}:r2", Range.unbounded(), new TsRangeArgs().aggregation(3000, aggregation)))
                    .hasSize(3);
        }
    }

    /**
     * Scenario I01 (RANGE with several aggregations, Redis 8.8+): multi-value rows are equal on both protocols.
     */
    @Test
    void rangeSeveralAggregations() {

        assumeTrue(RedisConditions.of(resp3).hasVersionGreaterOrEqualsTo("8.8"), "Requires Redis 8.8");
        statsSeries("{ts}:r3");

        List<TsSample> fromResp2 = resp2.tsRange("{ts}:r3", Range.unbounded(),
                new TsRangeArgs().aggregation(3000, TsAggregationType.MIN, TsAggregationType.AVG, TsAggregationType.MAX));
        List<TsSample> fromResp3 = resp3.tsRange("{ts}:r3", Range.unbounded(),
                new TsRangeArgs().aggregation(3000, TsAggregationType.MIN, TsAggregationType.AVG, TsAggregationType.MAX));

        assertThat(fromResp2).isEqualTo(fromResp3).hasSize(3);
        assertThat(fromResp2.get(1).getValues()).hasSize(3).isEqualTo(fromResp3.get(1).getValues());
    }

    /**
     * Scenario I01 (RANGE with EMPTY): the NaN of an empty avg bucket and the 0 of an empty count bucket are equal on both
     * protocols.
     */
    @Test
    void rangeEmpty() {

        resp3.tsCreate("{ts}:e");
        resp3.tsAdd("{ts}:e", 1000L, 1.0);
        resp3.tsAdd("{ts}:e", 7000L, 7.0);

        for (TsAggregationType aggregation : new TsAggregationType[] { TsAggregationType.AVG, TsAggregationType.COUNT,
                TsAggregationType.TWA }) {
            List<TsSample> fromResp2 = resp2.tsRange("{ts}:e", Range.unbounded(),
                    new TsRangeArgs().aggregation(3000, aggregation).empty());
            List<TsSample> fromResp3 = resp3.tsRange("{ts}:e", Range.unbounded(),
                    new TsRangeArgs().aggregation(3000, aggregation).empty());
            assertThat(fromResp2).as(aggregation.toString()).isEqualTo(fromResp3).hasSize(3);
        }
        assertThat(
                resp2.tsRange("{ts}:e", Range.unbounded(), new TsRangeArgs().aggregation(3000, TsAggregationType.AVG).empty())
                        .get(1).getValue()).isNaN();
    }

    /**
     * Scenario I01 (GET and RANGE with NaN, Redis 8.6+): stored NaN values are equal on both protocols.
     */
    @Test
    void nanValues() {

        assumeTrue(RedisConditions.of(resp3).hasVersionGreaterOrEqualsTo("8.6"), "Requires Redis 8.6");
        resp3.tsCreate("{ts}:nan");
        resp3.tsAdd("{ts}:nan", 1000L, 1.0);
        resp3.tsAdd("{ts}:nan", 2000L, Double.NaN);

        assertThat(resp2.tsGet("{ts}:nan")).isEqualTo(resp3.tsGet("{ts}:nan")).isEqualTo(TsSample.of(2000, Double.NaN));
        assertThat(resp2.tsRange("{ts}:nan", Range.unbounded())).isEqualTo(resp3.tsRange("{ts}:nan", Range.unbounded()))
                .isEqualTo(Arrays.asList(TsSample.of(1000, 1.0), TsSample.of(2000, Double.NaN)));
    }

    /**
     * Scenario I01 (READ, Redis 8.10+): TS.READ is equal on both protocols.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void read() {

        statsSeries("{ts}:rd");

        assertThat(resp2.tsRead("{ts}:rd", TsReadOffset.from(3000))).isEqualTo(resp3.tsRead("{ts}:rd", TsReadOffset.from(3000)))
                .hasSize(6);
    }

    // --- label-index reads -------------------------------------------------------------------------------------------

    private static final TsFilter PARITY = TsFilter.equal("t", "parity");

    /**
     * F1-like {@code {ts}:m1} (labels {@code t=parity, sensor=temp, area=north}, samples 1, 2.5, 3 at 1000 … 3000), F2 on
     * {@code {ts}:m2}, and {@code {ts}:m3} without samples.
     */
    private void labelIndexSeries() {

        resp3.tsCreate("{ts}:m1", new TsCreateArgs().label("t", "parity").label("sensor", "temp").label("area", "north"));
        resp3.tsAdd("{ts}:m1", 1000L, 1.0);
        resp3.tsAdd("{ts}:m1", 2000L, 2.5);
        resp3.tsAdd("{ts}:m1", 3000L, 3.0);
        statsSeries("{ts}:m2");
        resp3.tsCreate("{ts}:m3", new TsCreateArgs().label("t", "parity").label("sensor", "hum"));
    }

    /**
     * F3 on {@code {ts}:g1} / {@code {ts}:g2} with the label {@code t=parity}.
     */
    private void groupPair() {

        resp3.tsCreate("{ts}:g1", new TsCreateArgs().label("t", "parity").label("grp", "x").label("src", "a"));
        resp3.tsCreate("{ts}:g2", new TsCreateArgs().label("t", "parity").label("grp", "x").label("src", "b"));
        for (int i = 0; i < 3; i++) {
            resp3.tsAdd("{ts}:g1", (i + 1) * 1000L, 1.0 + i);
            resp3.tsAdd("{ts}:g2", (i + 1) * 1000L, 3.0 + i);
        }
    }

    /**
     * Scenario I01 (MGET): MGET without labels, with WITHLABELS, with SELECTED_LABELS (a missing label), and with LATEST is
     * equal on both protocols, order-insensitive.
     */
    @Test
    void mget() {

        labelIndexSeries();

        assertThat(resp2.tsMGet(PARITY)).containsExactlyInAnyOrderElementsOf(resp3.tsMGet(PARITY)).hasSize(3);
        assertThat(resp2.tsMGet(new TsMGetArgs().withLabels(), PARITY))
                .containsExactlyInAnyOrderElementsOf(resp3.tsMGet(new TsMGetArgs().withLabels(), PARITY)).hasSize(3);
        assertThat(resp2.tsMGet(new TsMGetArgs().selectedLabels("area", "missing"), PARITY))
                .containsExactlyInAnyOrderElementsOf(resp3.tsMGet(new TsMGetArgs().selectedLabels("area", "missing"), PARITY));
        assertThat(resp2.tsMGet(new TsMGetArgs().latest(), PARITY))
                .containsExactlyInAnyOrderElementsOf(resp3.tsMGet(new TsMGetArgs().latest(), PARITY));
    }

    /**
     * Scenario I01 (MRANGE raw): MRANGE and MREVRANGE with and without WITHLABELS are equal on both protocols,
     * order-insensitive.
     */
    @Test
    void mrangeRaw() {

        labelIndexSeries();

        assertThat(resp2.tsMRange(Range.unbounded(), PARITY))
                .containsExactlyInAnyOrderElementsOf(resp3.tsMRange(Range.unbounded(), PARITY)).hasSize(3);
        assertThat(resp2.tsMRevRange(Range.unbounded(), new TsMRangeArgs().withLabels(), PARITY))
                .containsExactlyInAnyOrderElementsOf(
                        resp3.tsMRevRange(Range.unbounded(), new TsMRangeArgs().withLabels(), PARITY))
                .hasSize(3);
        assertThat(resp2.tsMRange(Range.unbounded(), new TsMRangeArgs().aggregation(3000, TsAggregationType.AVG), PARITY))
                .containsExactlyInAnyOrderElementsOf(
                        resp3.tsMRange(Range.unbounded(), new TsMRangeArgs().aggregation(3000, TsAggregationType.AVG), PARITY));
    }

    /**
     * Scenario I01 (MRANGE with several aggregations, Redis 8.8+): equal on both protocols, order-insensitive.
     */
    @Test
    void mrangeSeveralAggregations() {

        assumeTrue(RedisConditions.of(resp3).hasVersionGreaterOrEqualsTo("8.8"), "Requires Redis 8.8");
        labelIndexSeries();

        List<TsSeries<String>> fromResp2 = resp2.tsMRange(Range.unbounded(), new TsMRangeArgs().withLabels().aggregation(3000,
                TsAggregationType.MIN, TsAggregationType.AVG, TsAggregationType.MAX), PARITY);
        List<TsSeries<String>> fromResp3 = resp3.tsMRange(Range.unbounded(), new TsMRangeArgs().withLabels().aggregation(3000,
                TsAggregationType.MIN, TsAggregationType.AVG, TsAggregationType.MAX), PARITY);

        assertThat(fromResp2).containsExactlyInAnyOrderElementsOf(fromResp3).hasSize(3);
    }

    /**
     * Scenario I01 (GROUPBY + WITHLABELS): the grouped series, including the sources, is equal on both protocols.
     */
    @Test
    void mrangeGroupByWithLabels() {

        groupPair();

        List<TsSeries<String>> fromResp2 = resp2.tsMRange(Range.unbounded(),
                new TsMRangeArgs().withLabels().groupBy("grp", TsReducerType.SUM), PARITY);
        List<TsSeries<String>> fromResp3 = resp3.tsMRange(Range.unbounded(),
                new TsMRangeArgs().withLabels().groupBy("grp", TsReducerType.SUM), PARITY);

        assertThat(fromResp2).isEqualTo(fromResp3).hasSize(1);
        assertThat(fromResp2.hashCode()).isEqualTo(fromResp3.hashCode());
        assertThat(fromResp2.get(0).getSources()).containsExactlyInAnyOrder("{ts}:g1", "{ts}:g2");
    }

    /**
     * Scenario I02: GROUPBY without WITHLABELS: everything is equal on both protocols except {@code getSources()}, which is
     * {@code null} on RESP2 (design §3.5, H7).
     */
    @Test
    void mrangeGroupByWithoutLabelsDiffersOnlyInSources() {

        groupPair();

        TsSeries<String> fromResp2 = resp2
                .tsMRevRange(Range.unbounded(), new TsMRangeArgs().groupBy("grp", TsReducerType.AVG), PARITY).get(0);
        TsSeries<String> fromResp3 = resp3
                .tsMRevRange(Range.unbounded(), new TsMRangeArgs().groupBy("grp", TsReducerType.AVG), PARITY).get(0);

        assertThat(fromResp2.getSources()).isNull();
        assertThat(fromResp3.getSources()).containsExactlyInAnyOrder("{ts}:g1", "{ts}:g2");
        assertThat(fromResp2).isNotEqualTo(fromResp3);
        assertThat(fromResp2.isGrouped()).isEqualTo(fromResp3.isGrouped()).isTrue();
        assertThat(fromResp2.getKey()).isEqualTo(fromResp3.getKey());
        assertThat(fromResp2.getGroup()).isEqualTo(fromResp3.getGroup()).isEqualTo("grp=x");
        assertThat(fromResp2.getLabels()).isEqualTo(fromResp3.getLabels());
        assertThat(fromResp2.getAggregations()).isEqualTo(fromResp3.getAggregations());
        assertThat(fromResp2.getReducer()).isEqualTo(fromResp3.getReducer());
        assertThat(fromResp2.getSamples()).isEqualTo(fromResp3.getSamples()).hasSize(3);
    }

    /**
     * Scenario I01 (QUERYINDEX): equal on both protocols (RESP2 array, RESP3 set), order-insensitive.
     */
    @Test
    void queryIndex() {

        labelIndexSeries();

        assertThat(resp2.tsQueryIndex(PARITY)).containsExactlyInAnyOrderElementsOf(resp3.tsQueryIndex(PARITY))
                .containsExactlyInAnyOrder("{ts}:m1", "{ts}:m2", "{ts}:m3");
    }

    /**
     * Scenario I01 (QUERYLABELS, Redis 8.10+): label names and values are equal on both protocols, order-insensitive.
     */
    @Test
    @EnabledOnCommand("TS.QUERYLABELS")
    void queryLabels() {

        labelIndexSeries();

        assertThat(resp2.tsQueryLabels(PARITY)).containsExactlyInAnyOrderElementsOf(resp3.tsQueryLabels(PARITY))
                .containsExactlyInAnyOrder("t", "sensor", "area");
        assertThat(resp2.tsQueryLabelValues("sensor")).containsExactlyInAnyOrderElementsOf(resp3.tsQueryLabelValues("sensor"))
                .containsExactlyInAnyOrder("temp", "hum");
    }

    // --- TS.NRANGE / TS.NREVRANGE ------------------------------------------------------------------------------------

    /**
     * Fixture F6 {@code nrangeFlow}: {@code {ts}:n1} (0, 1), (5, 2), (10, 100); {@code {ts}:n2} (0, 3), (5, 27), (12, 50).
     */
    private void nrangeFlow() {
        resp3.tsAdd("{ts}:n1", 0L, 1.0);
        resp3.tsAdd("{ts}:n1", 5L, 2.0);
        resp3.tsAdd("{ts}:n1", 10L, 100.0);
        resp3.tsAdd("{ts}:n2", 0L, 3.0);
        resp3.tsAdd("{ts}:n2", 5L, 27.0);
        resp3.tsAdd("{ts}:n2", 12L, 50.0);
    }

    /**
     * Scenario I01 (NRANGE raw, Redis 8.10+): the raw pivot, with NaN for a missing sample, is equal on both protocols, for
     * NRANGE and NREVRANGE.
     */
    @Test
    @EnabledOnCommand("TS.NRANGE")
    void nrangeRaw() {

        nrangeFlow();

        List<TsNRangeRow> fromResp2 = resp2.tsNRange("{ts}:n1", "{ts}:n2", Range.unbounded());
        List<TsNRangeRow> fromResp3 = resp3.tsNRange("{ts}:n1", "{ts}:n2", Range.unbounded());

        assertThat(fromResp2).isEqualTo(fromResp3).hasSize(4);
        assertThat(fromResp2.hashCode()).isEqualTo(fromResp3.hashCode());
        assertThat(fromResp2.get(2).getValues(1).get(0)).isNaN();
        assertThat(resp2.tsNRevRange("{ts}:n1", "{ts}:n2", Range.unbounded()))
                .isEqualTo(resp3.tsNRevRange("{ts}:n1", "{ts}:n2", Range.unbounded())).hasSize(4);
    }

    /**
     * Scenario I01 / E04 (NRANGE with several aggregations per key, Redis 8.10+): the F6 flow reply
     * {@code [[0,[3,30,2]],[10,[100,50,1]]]} is equal on both protocols, with the same per-key slices.
     */
    @Test
    @EnabledOnCommand("TS.NRANGE")
    void nrangeSeveralAggregations() {

        nrangeFlow();
        List<List<TsAggregation>> perKey = Arrays.asList(Arrays.asList(TsAggregationType.SUM),
                Arrays.asList(TsAggregationType.SUM, TsAggregationType.COUNT));

        List<TsNRangeRow> fromResp2 = resp2.tsNRange(Arrays.asList("{ts}:n1", "{ts}:n2"), Range.unbounded(),
                new TsNRangeArgs().aggregation(10, perKey));
        List<TsNRangeRow> fromResp3 = resp3.tsNRange(Arrays.asList("{ts}:n1", "{ts}:n2"), Range.unbounded(),
                new TsNRangeArgs().aggregation(10, perKey));

        assertThat(fromResp2).isEqualTo(fromResp3).hasSize(2);
        assertThat(fromResp2.get(1).getValues(1)).isEqualTo(fromResp3.get(1).getValues(1)).containsExactly(50.0, 1.0);
    }

    /**
     * Scenario I01 (NRANGE with EMPTY, Redis 8.10+): all-NaN empty buckets and fractional averages are equal on both protocols.
     */
    @Test
    @EnabledOnCommand("TS.NRANGE")
    void nrangeEmpty() {

        nrangeFlow();

        List<TsNRangeRow> fromResp2 = resp2.tsNRevRange("{ts}:n1", "{ts}:n2", Range.unbounded(),
                new TsNRangeArgs().aggregationForEachKey(3, TsAggregationType.AVG, TsAggregationType.MAX).empty());
        List<TsNRangeRow> fromResp3 = resp3.tsNRevRange("{ts}:n1", "{ts}:n2", Range.unbounded(),
                new TsNRangeArgs().aggregationForEachKey(3, TsAggregationType.AVG, TsAggregationType.MAX).empty());

        assertThat(fromResp2).isEqualTo(fromResp3).hasSize(5);
        assertThat(fromResp2.get(2).getValues()).hasSize(4).allMatch(v -> v.isNaN());

        List<TsNRangeRow> avgResp2 = resp2.tsNRange("{ts}:n1", "{ts}:n2", Range.unbounded(),
                new TsNRangeArgs().aggregationForEachKey(20, TsAggregationType.AVG, TsAggregationType.STD_S));
        assertThat(avgResp2)
                .isEqualTo(resp3.tsNRange("{ts}:n1", "{ts}:n2", Range.unbounded(),
                        new TsNRangeArgs().aggregationForEachKey(20, TsAggregationType.AVG, TsAggregationType.STD_S)))
                .hasSize(1);
    }

}
