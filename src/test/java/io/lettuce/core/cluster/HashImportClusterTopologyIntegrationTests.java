/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.cluster.ClusterTestUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.category.SlowTests;
import io.lettuce.core.HashImport;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.event.command.CommandFailedEvent;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.resource.DefaultRedisClient;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * Cluster topology-change coverage for {@code HIMPORT}: a node that becomes the owner of a slot <em>after</em> a fieldset was
 * declared self-prepares that fieldset on the connection's activation, from the cluster-wide shared registry — the same
 * mechanism a reconnecting node uses. Both a newly joined master and a promoted replica are covered. Each is proven via a
 * command listener: the {@code SET} routed to the new owner succeeds with zero {@code no such fieldset} failures, so the
 * guarantee comes from activation replay, not from the retry-once backstop.
 */
@Tag(INTEGRATION_TEST)
@SlowTests
@EnabledOnCommand("HIMPORT")
public class HashImportClusterTopologyIntegrationTests extends TestSupport {

    private static final String host = TestSettings.hostAddr();

    private static final RedisClient client = DefaultRedisClient.get();

    private static RedisClusterClient clusterClient;

    private static ClusterTestHelper clusterHelper;

    private StatefulRedisConnection<String, String> connection5;

    private StatefulRedisConnection<String, String> connection6;

    private RedisCommands<String, String> redis5;

    private RedisCommands<String, String> redis6;

    @BeforeAll
    static void setupClient() {

        clusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(host, ClusterTestSettings.port5).build());
        clusterClient
                .setOptions(
                        ClusterClientOptions.builder()
                                .topologyRefreshOptions(ClusterTopologyRefreshOptions.builder()
                                        .enablePeriodicRefresh(Duration.ofSeconds(1)).dynamicRefreshSources(false).build())
                                .build());
        clusterHelper = new ClusterTestHelper(clusterClient, ClusterTestSettings.port5, ClusterTestSettings.port6);
    }

    @AfterAll
    static void shutdownClient() {
        FastShutdown.shutdown(clusterClient);
    }

    @BeforeEach
    void openConnections() {
        clusterHelper.flushdb();
        connection5 = client.connect(RedisURI.Builder.redis(host, ClusterTestSettings.port5).build());
        connection6 = client.connect(RedisURI.Builder.redis(host, ClusterTestSettings.port6).build());
        redis5 = connection5.sync();
        redis6 = connection6.sync();
        clusterHelper.clusterReset();
    }

    @AfterEach
    void closeConnections() {
        try {
            clusterHelper.clusterReset();
        } catch (RuntimeException ignored) {
            // best-effort cleanup
        }
        connection5.close();
        connection6.close();
    }

    /**
     * A brand-new master that joins the cluster after the fieldset was declared, and takes over a slot, self-prepares the
     * fieldset when the client first connects to it.
     */
    @Test
    void newMasterSelfPreparesFieldsetOnJoin() {

        // Form a single-master cluster: port5 owns every slot (port6 is not part of the cluster yet).
        redis5.clusterAddSlots(ClusterTestSettings.createSlots(0, 16384));
        Wait.untilEquals(16384, () -> getOwnPartition(redis5).getSlots().size()).waitOrTimeout();

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        clusterClient.addListener(listener);

        StatefulRedisClusterConnection<String, String> cluster = clusterClient.connect();
        try {
            clusterClient.reloadPartitions();
            Wait.untilEquals(1, () -> clusterClient.getPartitions().size()).waitOrTimeout();

            RedisAdvancedClusterCommands<String, String> cmd = cluster.sync();

            // Declare a fieldset while port5 is the only master (broadcast reaches port5 only).
            HashImport<String> fieldset = HashImport.of("name", "email");
            assertThat(cmd.himportPrepare(fieldset)).isEqualTo("OK");

            // A brand-new master (port6) joins AFTER the fieldset was declared and takes over one slot.
            int slot = 16000;
            String node5 = redis5.clusterMyId();
            String node6 = redis6.clusterMyId();
            redis5.clusterMeet(host, ClusterTestSettings.port6);

            // Wait for the gossip handshake to complete so both nodes know each other before reassigning the slot.
            Wait.untilTrue(() -> redis5.clusterNodes().contains(node6)).waitOrTimeout();
            Wait.untilTrue(() -> redis6.clusterNodes().contains(node5)).waitOrTimeout();

            redis6.clusterSetSlotNode(slot, node6);
            redis5.clusterSetSlotNode(slot, node6);

            // Wait for the cluster to settle and for the client to observe port6 owning the slot.
            Wait.untilTrue(clusterHelper::isStable).waitOrTimeout();
            Wait.untilTrue(() -> {
                clusterClient.reloadPartitions();
                RedisClusterNode owner = clusterClient.getPartitions().getPartitionBySlot(slot);
                return owner != null && owner.getNodeId().equals(node6);
            }).waitOrTimeout();

            String key = keyForSlot(slot);
            assertThat(cmd.himportSet(key, fieldset, "alice", "a@x.com")).isEqualTo("OK");
            assertThat(cmd.hget(key, "name")).isEqualTo("alice");

            assertThat(noSuchFieldsetFailures).hasValue(0);
        } finally {
            cluster.close();
            clusterClient.removeListener(listener);
        }
    }

    /**
     * A replica promoted to master after the fieldset was declared serves {@code SET} without re-preparing: the promoted node
     * self-prepares on the connection's activation, just like a newly joined master.
     */
    @Test
    void promotedReplicaSelfPreparesFieldset() {

        // Form a master (port5) with a replica (port6).
        ClusterSetup.setupMasterWithReplica(clusterHelper);
        Wait.untilTrue(() -> getOwnPartition(redis5).is(RedisClusterNode.NodeFlag.UPSTREAM)).waitOrTimeout();
        Wait.untilTrue(() -> getOwnPartition(redis6).is(RedisClusterNode.NodeFlag.REPLICA)).waitOrTimeout();

        String node6 = redis6.clusterMyId();

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        clusterClient.addListener(listener);

        StatefulRedisClusterConnection<String, String> cluster = clusterClient.connect();
        try {
            RedisAdvancedClusterCommands<String, String> cmd = cluster.sync();

            // Declare a fieldset. The broadcast reaches the master (port5) only; the replica (port6) is not connected.
            HashImport<String> fieldset = HashImport.of("name", "email");
            assertThat(cmd.himportPrepare(fieldset)).isEqualTo("OK");

            // Promote the replica (port6) to master.
            assertThat(redis6.clusterFailover(true)).isEqualTo("OK");
            Wait.untilTrue(() -> getOwnPartition(redis6).is(RedisClusterNode.NodeFlag.UPSTREAM)).waitOrTimeout();
            Wait.untilTrue(() -> getOwnPartition(redis5).is(RedisClusterNode.NodeFlag.REPLICA)).waitOrTimeout();

            // Wait until the client observes port6 as the upstream owner of the slots.
            Wait.untilTrue(() -> {
                clusterClient.reloadPartitions();
                RedisClusterNode owner = clusterClient.getPartitions().getPartitionBySlot(0);
                return owner != null && node6.equals(owner.getNodeId()) && owner.is(RedisClusterNode.NodeFlag.UPSTREAM);
            }).waitOrTimeout();

            assertThat(cmd.himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");
            assertThat(cmd.hget("u:1", "name")).isEqualTo("alice");

            assertThat(noSuchFieldsetFailures).hasValue(0);
        } finally {
            cluster.close();
            clusterClient.removeListener(listener);
        }
    }

    private static CommandListener countingNoSuchFieldset(AtomicInteger counter) {
        return new CommandListener() {

            @Override
            public void commandFailed(CommandFailedEvent event) {
                Throwable cause = event.getCause();
                if (cause != null && cause.getMessage() != null
                        && cause.getMessage().toLowerCase().contains("no such fieldset")) {
                    counter.incrementAndGet();
                }
            }

        };
    }

}
