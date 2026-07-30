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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.HashImport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.event.command.CommandFailedEvent;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Cluster connection-session-state scenarios for {@code HIMPORT}: per-node session-state preservation when node connections
 * reconnect. The cluster counterpart of {@link io.lettuce.core.commands.HashImportConnectionStateIntegrationTests}; kept
 * separate from the cluster command-flow overload because it needs raw node-connection and client control.
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("HIMPORT")
class HashImportClusterConnectionStateIntegrationTests {

    private final RedisClusterClient clusterClient;

    private final StatefulRedisClusterConnection<String, String> connection;

    @Inject
    HashImportClusterConnectionStateIntegrationTests(RedisClusterClient clusterClient,
            StatefulRedisClusterConnection<String, String> connection) {
        this.clusterClient = clusterClient;
        this.connection = connection;
    }

    @BeforeEach
    void setUp() {
        this.connection.sync().flushall();
    }

    /**
     * After every upstream node connection is bounced, slot-routed {@code SET}s still succeed without re-preparing, and —
     * proven via a command listener — with zero {@code no such fieldset} failures. A zero count means each bounced node
     * re-prepared the fieldset on activation from the cluster-wide shared registry ahead of the {@code SET}; it is activation
     * replay doing the work, not the retry-once backstop masking a broken per-node replay.
     */
    @Test
    void reconnectPreservesFieldsetOnNodes() {

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        clusterClient.addListener(listener);

        StatefulRedisClusterConnection<String, String> listened = clusterClient.connect();
        try {
            RedisAdvancedClusterCommands<String, String> cmd = listened.sync();
            HashImport<String> fieldset = HashImport.of("name", "email");
            cmd.himportPrepare(fieldset);

            List<RedisClusterNode> masters = ClusterTestUtil.upstreamNodesWithSlots(listened);
            assertThat(masters).isNotEmpty();

            for (RedisClusterNode master : masters) {
                StatefulRedisConnection<String, String> nodeConnection = listened.getConnection(master.getNodeId());
                nodeConnection.sync().quit();
                Wait.untilTrue(nodeConnection::isOpen).waitOrTimeout();
            }

            for (RedisClusterNode master : masters) {
                String key = ClusterTestUtil.keyForNode(master);
                assertThat(cmd.himportSet(key, fieldset, "name-" + key, "mail-" + key)).isEqualTo("OK");
                assertThat(cmd.hget(key, "name")).isEqualTo("name-" + key);
            }

            assertThat(noSuchFieldsetFailures).hasValue(0);
        } finally {
            listened.close();
            clusterClient.removeListener(listener);
        }
    }

    /**
     * Positive control for {@link #reconnectPreservesFieldsetOnNodes()}: a never-prepared {@code SET} routed to a node draws
     * {@code no such fieldset} from that node (recorded by the listener) before retry-once recovers it. Proving the failure IS
     * observed here is what makes the zero-failures assertion in the reconnect test meaningful for the cluster path.
     */
    @Test
    void noSuchFieldsetErrorIsObservable() {

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        clusterClient.addListener(listener);

        StatefulRedisClusterConnection<String, String> listened = clusterClient.connect();
        try {
            RedisAdvancedClusterCommands<String, String> cmd = listened.sync();
            cmd.flushall();

            String key = ClusterTestUtil.keyForNode(ClusterTestUtil.upstreamNodesWithSlots(listened).get(0));
            HashImport<String> fieldset = HashImport.of("name", "email");

            assertThat(cmd.himportSet(key, fieldset, "alice", "a@x.com")).isEqualTo("OK");
            assertThat(cmd.hget(key, "name")).isEqualTo("alice");

            assertThat(noSuchFieldsetFailures.get()).isGreaterThanOrEqualTo(1);
        } finally {
            listened.close();
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
