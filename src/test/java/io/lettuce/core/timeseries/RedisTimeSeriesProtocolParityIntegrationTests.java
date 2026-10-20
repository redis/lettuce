/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.test.condition.EnabledOnCommand;
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

}
