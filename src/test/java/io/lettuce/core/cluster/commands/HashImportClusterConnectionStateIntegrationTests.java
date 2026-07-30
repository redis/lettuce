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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.HashImport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
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
     * A fieldset first used on each upstream node keeps working after every node connection is bounced: slot-routed
     * {@code SET}s still succeed without any explicit prepare, and — proven via a command listener — with zero
     * {@code no such fieldset} failures. A zero count means each reconnected node re-injected the {@code PREPARE} lazily ahead
     * of the {@code SET}.
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

            List<RedisClusterNode> masters = ClusterTestUtil.upstreamNodesWithSlots(listened);
            assertThat(masters).isNotEmpty();

            // Prime: a slot-routed SET self-prepares the fieldset on each node.
            for (RedisClusterNode master : masters) {
                String key = ClusterTestUtil.keyForNode(master);
                assertThat(cmd.himportSet(key, fieldset, "seed-" + key, "s@x.com")).isEqualTo("OK");
            }

            // Bounce every upstream node connection.
            for (RedisClusterNode master : masters) {
                StatefulRedisConnection<String, String> nodeConnection = listened.getConnection(master.getNodeId());
                nodeConnection.sync().quit();
                Wait.untilTrue(nodeConnection::isOpen).waitOrTimeout();
            }

            // After reconnect each node re-injects PREPARE lazily ahead of the SET.
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
     * {@code close()} discards the fieldset on every node it was used on, not via a blind broadcast. Each touched master has
     * the fieldset before {@code close()} and none has it afterwards, probed with a raw slot-routed {@code SET}.
     */
    @Test
    void closeDiscardsOnEveryTouchedNode() throws Exception {

        StatefulRedisClusterConnection<String, String> cluster = clusterClient.connect();
        try {
            RedisAdvancedClusterCommands<String, String> cmd = cluster.sync();
            cmd.flushall();

            HashImport<String> fieldset = HashImport.of("name", "email");
            List<RedisClusterNode> masters = ClusterTestUtil.upstreamNodesWithSlots(cluster);
            assertThat(masters).isNotEmpty();

            for (RedisClusterNode master : masters) {
                String key = ClusterTestUtil.keyForNode(master);
                assertThat(cmd.himportSet(key, fieldset, "n-" + key, "m-" + key)).isEqualTo("OK");
                assertThat(fieldsetPreparedForKey(cluster, key, fieldset)).isTrue();
            }

            fieldset.close();

            for (RedisClusterNode master : masters) {
                String key = ClusterTestUtil.keyForNode(master);
                assertThat(fieldsetPreparedForKey(cluster, key, fieldset)).isFalse();
            }
        } finally {
            cluster.close();
        }
    }

    /**
     * Probes whether {@code fieldset} is prepared on the node owning {@code key}'s slot by dispatching a raw slot-routed
     * {@code HIMPORT SET} that the outbound handler ignores (no injected {@code PREPARE}). Returns {@code true} when it
     * succeeds and {@code false} on a {@code no such fieldset} error.
     */
    private static boolean fieldsetPreparedForKey(StatefulRedisClusterConnection<String, String> cluster, String key,
            HashImport<String> fieldset) throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandType.SET).addKey(key)
                .addKey(fieldset.name()).addValue("x").addValue("y");
        AsyncCommand<String, String, String> command = new AsyncCommand<>(
                new Command<>(CommandType.HIMPORT, new StatusOutput<>(StringCodec.UTF8), args));
        cluster.dispatch(command);

        try {
            command.get(5, TimeUnit.SECONDS);
            return true;
        } catch (ExecutionException ex) {
            String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            if (message != null && message.toLowerCase().contains("no such fieldset")) {
                return false;
            }
            throw ex;
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
