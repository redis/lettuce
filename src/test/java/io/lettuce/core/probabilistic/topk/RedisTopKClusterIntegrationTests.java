/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic.topk;

import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import org.junit.jupiter.api.Tag;

import javax.inject.Inject;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Integration tests for Redis Top-K commands using Redis Cluster.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
public class RedisTopKClusterIntegrationTests extends RedisTopKIntegrationTests {

    @Inject
    RedisTopKClusterIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
    }

}
