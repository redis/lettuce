/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import io.lettuce.core.internal.LettuceSets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.lettuce.Wait;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.Executions;
import io.lettuce.core.cluster.api.sync.NodeSelection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
public class NodeSelectionSyncTest extends AbstractClusterTest {

    private RedisAdvancedClusterCommands<String, String> commands;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @Before
    public void before() throws Exception {
        clusterClient.reloadPartitions();
        clusterConnection = clusterClient.connect();
        commands = clusterConnection.sync();
    }

    @After
    public void after() throws Exception {
        commands.getStatefulConnection().close();
    }

    @Test
    public void testMultiNodeOperations() throws Exception {

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
    public void testNodeSelectionCount() throws Exception {
        assertThat(commands.all().size()).isEqualTo(4);
        assertThat(commands.slaves().size()).isEqualTo(2);
        assertThat(commands.masters().size()).isEqualTo(2);

        assertThat(commands.nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MYSELF)).size()).isEqualTo(
                1);
    }

    @Test
    public void testNodeSelection() throws Exception {

        NodeSelection<String, String> onlyMe = commands.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.MYSELF));
        Map<RedisClusterNode, RedisCommands<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        RedisCommands<String, String> node = onlyMe.commands(0);
        assertThat(node).isNotNull();

        RedisClusterNode redisClusterNode = onlyMe.node(0);
        assertThat(redisClusterNode.getFlags()).contains(RedisClusterNode.NodeFlag.MYSELF);

        assertThat(onlyMe.asMap()).hasSize(1);
    }

    @Test
    public void testDynamicNodeSelection() throws Exception {

        Partitions partitions = commands.getStatefulConnection().getPartitions();
        partitions.forEach(redisClusterNode -> redisClusterNode.setFlags(Collections.singleton(RedisClusterNode.NodeFlag.MASTER)));

        NodeSelection<String, String> selection = commands.nodes(
                redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF), true);

        assertThat(selection.asMap()).hasSize(0);
        partitions.getPartition(0)
                .setFlags(LettuceSets.unmodifiableSet(RedisClusterNode.NodeFlag.MYSELF, RedisClusterNode.NodeFlag.MASTER));
        assertThat(selection.asMap()).hasSize(1);

        partitions.getPartition(1)
                .setFlags(LettuceSets.unmodifiableSet(RedisClusterNode.NodeFlag.MYSELF, RedisClusterNode.NodeFlag.MASTER));
        assertThat(selection.asMap()).hasSize(2);

    }

    @Test
    public void testNodeSelectionPing() throws Exception {

        NodeSelection<String, String> onlyMe = commands.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.MYSELF));
        Map<RedisClusterNode, RedisCommands<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        Executions<String> ping = onlyMe.commands().ping();

        assertThat(ping.get(onlyMe.node(0))).isEqualTo("PONG");
    }

    @Test
    public void testStaticNodeSelection() throws Exception {

        NodeSelection<String, String> selection = commands.nodes(
                redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF), false);

        assertThat(selection.asMap()).hasSize(1);

        commands.getStatefulConnection().getPartitions().getPartition(2)
                .setFlags(Collections.singleton(RedisClusterNode.NodeFlag.MYSELF));

        assertThat(selection.asMap()).hasSize(1);
    }

    @Test
    public void testAsynchronicityOfMultiNodeExecution() throws Exception {

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

        Executions<String> kill = commands.masters().commands().scriptKill();
    }

    @Test
    public void testSlavesReadWrite() throws Exception {

        NodeSelection<String, String> nodes = commands.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.SLAVE));

        assertThat(nodes.size()).isEqualTo(2);

        commands.set(key, value);
        waitForReplication(key, port4);

        try {

            nodes.commands().get(key);
            fail("Missing RedisCommandExecutionException: MOVED");
        } catch (RedisCommandExecutionException e) {
            assertThat(e.getSuppressed().length).isGreaterThan(0);
        }
    }

    @Test
    public void testSlavesWithReadOnly() throws Exception {

        int slot = SlotHash.getSlot(key);
        Optional<RedisClusterNode> master = clusterConnection.getPartitions().getPartitions().stream()
                .filter(redisClusterNode -> redisClusterNode.hasSlot(slot)).findFirst();

        NodeSelection<String, String> nodes = commands.slaves(redisClusterNode -> redisClusterNode
                .is(RedisClusterNode.NodeFlag.SLAVE) && redisClusterNode.getSlaveOf().equals(master.get().getNodeId()));

        assertThat(nodes.size()).isEqualTo(1);

        commands.set(key, value);
        waitForReplication(key, port4);

        Executions<String> keys = nodes.commands().get(key);
        assertThat(keys).hasSize(1).contains(value);
    }

    protected void waitForReplication(String key, int port) throws Exception {
        waitForReplication(commands, key, port);
    }

    protected static void waitForReplication(RedisAdvancedClusterCommands<String, String> commands, String key, int port)
            throws Exception {

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
