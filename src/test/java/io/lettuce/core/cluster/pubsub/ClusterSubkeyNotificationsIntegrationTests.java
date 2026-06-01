/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster.pubsub;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.TestSupport;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.api.sync.NodeSelectionPubSubCommands;
import io.lettuce.core.cluster.pubsub.api.sync.PubSubNodeSelection;
import io.lettuce.core.support.CapturingPubSubListener;
import io.lettuce.core.support.CapturingPubSubListener.Notification;
import io.lettuce.test.LettuceExtension;

/**
 * Cluster integration tests for Redis 8.8 Subkey Notifications. Subkey notifications are published locally on the slot owner,
 * so the test enables node message propagation and subscribes on every upstream via {@link PubSubNodeSelection}. Server gating
 * mirrors {@link io.lettuce.core.pubsub.SubkeyNotificationsIntegrationTests}, applied to every upstream.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class ClusterSubkeyNotificationsIntegrationTests extends TestSupport {

    private final RedisClusterClient clusterClient;

    private StatefulRedisClusterConnection<String, String> connection;

    private StatefulRedisClusterPubSubConnection<String, String> pubSubConnection;

    private CapturingPubSubListener<String, String> listener;

    @Inject
    ClusterSubkeyNotificationsIntegrationTests(RedisClusterClient clusterClient) {
        this.clusterClient = clusterClient;
    }

    @BeforeEach
    void openPubSubConnection() {
        connection = clusterClient.connect();
        pubSubConnection = clusterClient.connectPubSub();
        pubSubConnection.setNodeMessagePropagation(true);
        listener = new CapturingPubSubListener<>();
        pubSubConnection.addListener(listener);

        try {
            for (RedisClusterNode node : connection.getPartitions()) {
                if (node.is(RedisClusterNode.NodeFlag.UPSTREAM)) {
                    connection.getConnection(node.getNodeId()).sync().configSet("notify-keyspace-events", "AKEhSTIV");
                }
            }
        } catch (RedisCommandExecutionException e) {
            Assumptions.abort("Cluster does not support subkey notification flags (STIV): " + e.getMessage());
        }
    }

    @AfterEach
    void closePubSubConnection() {
        if (connection != null) {
            for (RedisClusterNode node : connection.getPartitions()) {
                try {
                    connection.getConnection(node.getNodeId()).sync().configSet("notify-keyspace-events", "");
                } catch (Exception ignore) {
                    // best-effort restore
                }
            }
            ClusterTestUtil.flushDatabaseOfAllNodes(connection);
            connection.close();
        }
        if (pubSubConnection != null) {
            pubSubConnection.close();
        }
    }

    @Test
    void subkeyspace_clusterWide() {
        String hashKey = "cl-subkeyspace-" + System.nanoTime();
        String channel = "__subkeyspace@0__:" + hashKey;

        subscribeOnAllUpstreams(channel, false);

        connection.sync().hset(hashKey, "fld", "v1");

        Notification<String, String> n = listener.expectMessageOn(channel);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo("hset|3:fld");
    }

    @Test
    void subkeyevent_clusterWide() {
        String hashKey = "cl-subkeyevent-" + System.nanoTime();
        String channel = "__subkeyevent@0__:hset";

        subscribeOnAllUpstreams(channel, false);

        connection.sync().hset(hashKey, "fld", "v1");

        Notification<String, String> n = listener.expectMessageOn(channel);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo(hashKey.length() + ":" + hashKey + "|3:fld");
    }

    @Test
    void subkeyspaceitem_clusterWide() {
        String hashKey = "cl-subkeyspaceitem-" + System.nanoTime();
        String field = "fld";
        String pattern = "__subkeyspaceitem@0__:" + hashKey + "\n*";
        String expectedChannel = "__subkeyspaceitem@0__:" + hashKey + "\n" + field;

        subscribeOnAllUpstreams(pattern, true);

        connection.sync().hset(hashKey, field, "v1");

        Notification<String, String> n = listener.expectMessageOn(expectedChannel);
        assertThat(n.getChannel()).isEqualTo(expectedChannel);
        assertThat(n.getPattern()).isEqualTo(pattern);
        assertThat(n.getMessage()).isEqualTo("hset");
    }

    @Test
    void subkeyspaceevent_clusterWide() {
        String hashKey = "cl-subkeyspaceevent-" + System.nanoTime();
        String channel = "__subkeyspaceevent@0__:hset|" + hashKey;

        subscribeOnAllUpstreams(channel, false);

        connection.sync().hset(hashKey, "fld", "v1");

        Notification<String, String> n = listener.expectMessageOn(channel);
        assertThat(n.getChannel()).isEqualTo(channel);
        assertThat(n.getPattern()).isNull();
        assertThat(n.getMessage()).isEqualTo("3:fld");
    }

    private void subscribeOnAllUpstreams(String channelOrPattern, boolean usePattern) {
        PubSubNodeSelection<String, String> upstream = pubSubConnection.sync().upstream();
        NodeSelectionPubSubCommands<String, String> commands = upstream.commands();
        if (usePattern) {
            commands.psubscribe(channelOrPattern);
        } else {
            commands.subscribe(channelOrPattern);
        }
    }

}
