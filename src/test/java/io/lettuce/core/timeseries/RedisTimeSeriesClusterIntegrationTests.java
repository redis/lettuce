/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Function;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisTimeSeriesCommands;
import io.lettuce.core.cluster.ClusterTestSettings;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.test.condition.RedisConditions;
import io.lettuce.test.resource.TestClientResources;

/**
 * Runs {@link RedisTimeSeriesIntegrationTests} over Redis Cluster ({@code test-cluster}), with the hash-tagged keys of the base
 * suite.
 */
@Tag(INTEGRATION_TEST)
public class RedisTimeSeriesClusterIntegrationTests extends RedisTimeSeriesIntegrationTests {

    private static final ProtocolKeyword REFRESHCLUSTER = new ProtocolKeyword() {

        private final byte[] bytes = "TIMESERIES.REFRESHCLUSTER".getBytes(StandardCharsets.US_ASCII);

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public String toString() {
            return "TIMESERIES.REFRESHCLUSTER";
        }

    };

    private final StatefulRedisClusterConnection<String, String> connection;

    @Inject
    RedisTimeSeriesClusterIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
        this.connection = connection;
    }

    /**
     * Below 8.10, keyless commands gather across shards only after {@code TIMESERIES.REFRESHCLUSTER} ran on the answering node
     * (test plan §3).
     */
    @BeforeAll
    void refreshClusterBelow810() {

        if (RedisConditions.of(connection).hasVersionGreaterOrEqualsTo("8.10")) {
            return;
        }
        for (RedisClusterNode node : connection.getPartitions()) {
            if (node.is(RedisClusterNode.NodeFlag.UPSTREAM)) {
                String reply = connection.getConnection(node.getNodeId()).sync().dispatch(REFRESHCLUSTER,
                        new StatusOutput<>(StringCodec.UTF8));
                assertThat(reply).isEqualTo("OK");
            }
        }
    }

    /**
     * ACL users are per node, so create them on every node of the cluster. The cluster client also needs {@code CLUSTER} to
     * read the topology.
     */
    @Override
    protected void createAclUser(String user, String... rules) {

        String[] withCluster = Arrays.copyOf(rules, rules.length + 1);
        withCluster[rules.length] = "+cluster";
        for (RedisClusterNode node : connection.getPartitions()) {
            assertThat(aclSetuser(connection.getConnection(node.getNodeId()).sync(), user, withCluster)).isEqualTo("OK");
        }
    }

    @Override
    protected void deleteAclUser(String user) {
        for (RedisClusterNode node : connection.getPartitions()) {
            connection.getConnection(node.getNodeId()).sync().aclDeluser(user);
        }
    }

    /**
     * Run {@code action} on a dedicated cluster connection authenticated as {@code user}.
     */
    @Override
    protected void runAs(String user, Function<RedisTimeSeriesCommands<String, String>, Void> action) {

        RedisURI uri = RedisURI.Builder.redis(ClusterTestSettings.host, ClusterTestSettings.port1)
                .withAuthentication(user, "unused").build();
        RedisClusterClient client = RedisClusterClient.create(TestClientResources.get(), uri);
        try (StatefulRedisClusterConnection<String, String> clusterConnection = client.connect()) {
            action.apply(clusterConnection.sync());
        } finally {
            client.shutdown();
        }
    }

}
