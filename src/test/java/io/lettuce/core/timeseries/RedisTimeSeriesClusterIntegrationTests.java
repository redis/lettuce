/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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

    private static final String FILTER_ACROSS_SLOTS_REASON = "TS.MGET/TS.QUERYINDEX FILTER matches series by label across "
            + "the whole keyspace; on Redis Cluster a single-node connection can only see the series hashed to that node's "
            + "slots, so a filter that matches keys spread across multiple slots cannot be fully resolved";

    @Inject
    RedisTimeSeriesClusterIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
    }

    @Disabled(FILTER_ACROSS_SLOTS_REASON)
    @Test
    @Override
    void tsMGetWithoutLabelsOptionReturnsEmptyLabelMap() {
    }

    @Disabled(FILTER_ACROSS_SLOTS_REASON)
    @Test
    @Override
    void tsMGetWithLabelsIncludesAllLabels() {
    }

    @Disabled(FILTER_ACROSS_SLOTS_REASON)
    @Test
    @Override
    void tsMGetIncludesSeriesWithNoSamples() {
    }

    @Disabled(FILTER_ACROSS_SLOTS_REASON)
    @Test
    @Override
    void tsQueryIndexReturnsMatchingKeys() {
    }

    @Disabled(FILTER_ACROSS_SLOTS_REASON)
    @Test
    @Override
    void tsQueryIndexWithMultipleFiltersNarrowsResults() {
    }

}
