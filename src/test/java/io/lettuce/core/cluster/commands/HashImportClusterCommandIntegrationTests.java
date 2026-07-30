/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cluster.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.HashImport;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.commands.HashImportIntegrationTests;

/**
 * Runs the {@code HIMPORT} command-flow tests over Redis Cluster, and adds the cluster-specific command behavior that cannot be
 * expressed through the shared facade: {@code PREPARE} broadcast with a deterministic slot-routed {@code SET} to every master.
 * Node-connection session-state scenarios live in
 * {@link io.lettuce.core.cluster.commands.HashImportClusterConnectionStateIntegrationTests}.
 */
@Tag(INTEGRATION_TEST)
class HashImportClusterCommandIntegrationTests extends HashImportIntegrationTests {

    private final StatefulRedisClusterConnection<String, String> connection;

    @Inject
    HashImportClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
        this.connection = connection;
    }

    /**
     * {@code PREPARE} broadcasts to every upstream node, so a {@code SET} deterministically routed to each master succeeds
     * against an already-prepared node.
     */
    @Test
    void himportBroadcastsAndSlotRoutes() {

        HashImport<String> fieldset = HashImport.of("name", "email");
        assertThat(redis.himportPrepare(fieldset)).isEqualTo("OK");

        List<RedisClusterNode> masters = ClusterTestUtil.upstreamNodesWithSlots(connection);
        assertThat(masters).isNotEmpty();

        for (RedisClusterNode master : masters) {
            String key = ClusterTestUtil.keyForNode(master);
            assertThat(redis.himportSet(key, fieldset, "name-" + key, "mail-" + key)).isEqualTo("OK");
            assertThat(redis.hget(key, "name")).isEqualTo("name-" + key);
        }

        assertThat(redis.himportDiscard(fieldset)).isTrue();
    }

}
