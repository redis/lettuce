/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cf;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Integration tests for Redis Cuckoo Filter commands using Redis Cluster.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
public class RedisCuckooFilterClusterIntegrationTests extends RedisCuckooFilterIntegrationTests {

    @Inject
    RedisCuckooFilterClusterIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
    }

}
