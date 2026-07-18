/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Integration tests for Redis TimeSeries commands using Redis Cluster.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
public class RedisTimeSeriesClusterIntegrationTests extends RedisTimeSeriesIntegrationTests {

    @Inject
    RedisTimeSeriesClusterIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
    }

}
