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

}
