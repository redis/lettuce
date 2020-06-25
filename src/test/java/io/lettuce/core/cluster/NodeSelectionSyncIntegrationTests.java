/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.core.ScriptOutputType.STATUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.Executions;
import io.lettuce.core.cluster.api.sync.NodeSelection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.internal.LettuceSets;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class NodeSelectionSyncIntegrationTests extends TestSupport {

    private final RedisClusterClient clusterClient;

    private final RedisAdvancedClusterCommands<String, String> commands;

    @Inject
    NodeSelectionSyncIntegrationTests(RedisClusterClient clusterClient,
            StatefulRedisClusterConnection<String, String> connection) {

        this.clusterClient = clusterClient;
        this.commands = connection.sync();
        connection.sync().flushall();
    }

    @Test
    void testMultiNodeOperations() {

        List<String> expectation = new ArrayList<>();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            expectation.add(key);
            commands.set(key, value);
        }

        List<String> result = new Vector<>();

        Executions<Long> executions = commands.masters().commands().keys(result::add, "*");

        assertThat(executions).hasSize(2);

        Collections.sort(expectation);
        Collections.sort(result);

        assertThat(result).isEqualTo(expectation);
    }

    @Test
    void testNodeSelectionCount() {
        assertThat(commands.all().size()).isEqualTo(4);
        assertThat(commands.slaves().size()).isEqualTo(2);
        assertThat(commands.masters().size()).isEqualTo(2);

        assertThat(commands.nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MYSELF)).size())
                .isEqualTo(1);
    }

    @Test
    void testNodeSelection() {

        NodeSelection<String, String> onlyMe = commands
                .nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF));
        Map<RedisClusterNode, RedisCommands<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        RedisCommands<String, String> node = onlyMe.commands(0);
        assertThat(node).isNotNull();

        RedisClusterNode redisClusterNode = onlyMe.node(0);
        assertThat(redisClusterNode.getFlags()).contains(RedisClusterNode.NodeFlag.MYSELF);

        assertThat(onlyMe.asMap()).hasSize(1);
    }

    @Test
    void testDynamicNodeSelection() {

        Partitions partitions = commands.getStatefulConnection().getPartitions();
        partitions.forEach(
                redisClusterNode -> redisClusterNode.setFlags(Collections.singleton(RedisClusterNode.NodeFlag.MASTER)));

        NodeSelection<String, String> selection = commands
                .nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF), true);

        assertThat(selection.asMap()).hasSize(0);
        partitions.getPartition(0)
                .setFlags(LettuceSets.unmodifiableSet(RedisClusterNode.NodeFlag.MYSELF, RedisClusterNode.NodeFlag.MASTER));
        assertThat(selection.asMap()).hasSize(1);

        partitions.getPartition(1)
                .setFlags(LettuceSets.unmodifiableSet(RedisClusterNode.NodeFlag.MYSELF, RedisClusterNode.NodeFlag.MASTER));
        assertThat(selection.asMap()).hasSize(2);

        clusterClient.reloadPartitions();
    }

    @Test
    void testNodeSelectionPing() {

        NodeSelection<String, String> onlyMe = commands
                .nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF));
        Map<RedisClusterNode, RedisCommands<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        Executions<String> ping = onlyMe.commands().ping();

        assertThat(ping.get(onlyMe.node(0))).isEqualTo("PONG");
    }

    @Test
    void testStaticNodeSelection() {

        NodeSelection<String, String> selection = commands
                .nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF), false);

        assertThat(selection.asMap()).hasSize(1);

        commands.getStatefulConnection().getPartitions().getPartition(2)
                .setFlags(Collections.singleton(RedisClusterNode.NodeFlag.MYSELF));

        assertThat(selection.asMap()).hasSize(1);

        clusterClient.reloadPartitions();
    }

    @Test
    void testAsynchronicityOfMultiNodeExecution() {

        RedisAdvancedClusterCommands<String, String> connection2 = clusterClient.connect().sync();

        connection2.setTimeout(1, TimeUnit.SECONDS);
        NodeSelection<String, String> masters = connection2.masters();
        masters.commands().configSet("lua-time-limit", "10");

        Executions<Object> eval = null;
        try {
            eval = masters.commands().eval("while true do end", STATUS, new String[0]);
            fail("missing exception");
        } catch (RedisCommandTimeoutException e) {
            assertThat(e).hasMessageContaining("Command timed out for node(s)");
        }

        commands.masters().commands().scriptKill();
    }

    @Test
    void testReplicasReadWrite() {

        NodeSelection<String, String> nodes = commands
                .nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.SLAVE));

        assertThat(nodes.size()).isEqualTo(2);

        commands.set(key, value);
        waitForReplication(key, ClusterTestSettings.port4);

        try {

            nodes.commands().get(key);
            fail("Missing RedisCommandExecutionException: MOVED");
        } catch (RedisCommandExecutionException e) {
            if (e.getMessage().startsWith("MOVED")) {
                assertThat(e.getSuppressed()).isEmpty();
            } else {
                assertThat(e.getSuppressed()).isNotEmpty();
            }
        }
    }

    @Test
    void testSlavesWithReadOnly() {

        int slot = SlotHash.getSlot(key);
        Optional<RedisClusterNode> master = clusterClient.getPartitions().getPartitions().stream()
                .filter(redisClusterNode -> redisClusterNode.hasSlot(slot)).findFirst();

        NodeSelection<String, String> nodes = commands
                .slaves(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE)
                        && redisClusterNode.getSlaveOf().equals(master.get().getNodeId()));

        assertThat(nodes.size()).isEqualTo(1);

        commands.set(key, value);
        waitForReplication(key, ClusterTestSettings.port4);

        Executions<String> keys = nodes.commands().get(key);
        assertThat(keys).hasSize(1).contains(value);
    }

    void waitForReplication(String key, int port) {
        waitForReplication(commands, key, port);
    }

    static void waitForReplication(RedisAdvancedClusterCommands<String, String> commands, String key, int port) {

        NodeSelection<String, String> selection = commands
                .slaves(redisClusterNode -> redisClusterNode.getUri().getPort() == port);
        Wait.untilNotEquals(null, () -> {

            Executions<String> strings = selection.commands().get(key);
            if (strings.stream().filter(s -> s != null).findFirst().isPresent()) {
                return "OK";
            }

            return null;
        }).waitOrTimeout();
    }

}
