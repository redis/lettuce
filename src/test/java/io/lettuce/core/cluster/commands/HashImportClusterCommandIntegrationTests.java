/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cluster.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.commands.HashImportIntegrationTests;

/**
 * Runs the {@code HIMPORT} command-flow tests over Redis Cluster. By overriding {@link #keyFor(int)} to route each index to a
 * different master, the shared base flow imports the same fieldset to keys on several nodes — exercising slot routing and
 * per-node lazy prepare (each node's connection self-prepares on first use, no broadcast, no hash tags) with no
 * cluster-specific test bodies.
 */
@Tag(INTEGRATION_TEST)
class HashImportClusterCommandIntegrationTests extends HashImportIntegrationTests {

    private final StatefulRedisClusterConnection<String, String> connection;

    @Inject
    HashImportClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
        this.connection = connection;
    }

    @Override
    protected String keyFor(int index) {
        List<RedisClusterNode> masters = ClusterTestUtil.upstreamNodesWithSlots(connection);
        return ClusterTestUtil.keyForNode(masters.get(index % masters.size()));
    }

}
