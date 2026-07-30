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
 * Runs the {@code HIMPORT} command-flow tests over Redis Cluster, and adds the cluster-specific behavior that cannot be
 * expressed through the shared facade: a {@code SET} deterministically slot-routed to each master self-prepares on that node's
 * connection, with no broadcast and no hash tags. Node-connection session-state scenarios live in
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
     * A {@code SET} deterministically slot-routed to each master self-prepares on that node's connection — no broadcast, no
     * hash tags — so imports to keys on different nodes all succeed. The fieldset is closed at the end.
     */
    @Test
    void himportSetSelfPreparesPerNode() {

        HashImport<String> fieldset = HashImport.of("name", "email");

        List<RedisClusterNode> masters = ClusterTestUtil.upstreamNodesWithSlots(connection);
        assertThat(masters).isNotEmpty();

        for (RedisClusterNode master : masters) {
            String key = ClusterTestUtil.keyForNode(master);
            // No explicit prepare: the SET routes to this master's connection, whose handler injects PREPARE first.
            assertThat(redis.himportSet(key, fieldset, "name-" + key, "mail-" + key)).isEqualTo("OK");
            assertThat(redis.hget(key, "name")).isEqualTo("name-" + key);
        }

        fieldset.close();
    }

}
