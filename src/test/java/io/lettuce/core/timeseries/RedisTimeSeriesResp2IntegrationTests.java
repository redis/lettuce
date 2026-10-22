/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.timeseries.arguments.TsMRangeArgs;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * Runs {@link RedisTimeSeriesIntegrationTests} on a RESP2 connection.
 */
@Tag(INTEGRATION_TEST)
public class RedisTimeSeriesResp2IntegrationTests extends RedisTimeSeriesIntegrationTests {

    @Inject
    RedisTimeSeriesResp2IntegrationTests(RedisClient client) {
        super(connectWithResp2(client));
    }

    private static RedisCommands<String, String> connectWithResp2(RedisClient client) {
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        return client.connect().sync();
    }

    /**
     * The pipelining test (L01) runs on a dedicated RESP2 connection.
     */
    @Override
    protected Pipeline openPipeline() {

        RedisClient client = RedisClient.create(TestClientResources.get(),
                RedisURI.create(TestSettings.host(), TestSettings.port()));
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        StatefulRedisConnection<String, String> connection = client.connect();
        return new Pipeline(connection, connection.async(), client::shutdown);
    }

    /**
     * Scenario D08 (RESP2): without WITHLABELS a RESP2 GROUPBY reply has no source information, so {@code getSources()} is
     * {@code null} (design §3.5, H7); with WITHLABELS the sources come from {@code __source__}: {g1, g2}.
     */
    @Test
    @Override
    void groupBySources() {

        groupPair();

        TsSeries<String> withoutLabels = onlyGroup(
                redis.tsMRange(Range.unbounded(), new TsMRangeArgs().groupBy("grp", TsReducerType.SUM), isolation()));
        assertThat(withoutLabels.getSources()).isNull();
        assertThat(withoutLabels.getLabels()).isEmpty();
        assertThat(withoutLabels.getSamples()).containsExactly(TsSample.of(1000, 4.0), TsSample.of(2000, 6.0),
                TsSample.of(3000, 8.0));

        assertGroupSourcesWithLabels();
    }

}
