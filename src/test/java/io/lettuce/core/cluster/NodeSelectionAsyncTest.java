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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.lettuce.Wait;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.AsyncExecutions;
import io.lettuce.core.cluster.api.async.AsyncNodeSelection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.internal.LettuceSets;

/**
 * @author Mark Paluch
 */
public class NodeSelectionAsyncTest extends AbstractClusterTest {

    private RedisAdvancedClusterAsyncCommands<String, String> commands;
    private RedisAdvancedClusterCommands<String, String> syncCommands;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @Before
    public void before() throws Exception {
        clusterClient.reloadPartitions();
        clusterConnection = clusterClient.connect();
        commands = clusterConnection.async();
        syncCommands = clusterConnection.sync();
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
            commands.set(key, value).get();
        }

        List<String> result = new Vector<>();

        CompletableFuture.allOf(commands.masters().commands().keys(result::add, "*").futures()).get();

        assertThat(result).hasSize(expectation.size());

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

        AsyncNodeSelection<String, String> onlyMe = commands.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.MYSELF));
        Map<RedisClusterNode, RedisAsyncCommands<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        RedisClusterAsyncCommands<String, String> node = onlyMe.commands(0);
        assertThat(node).isNotNull();

        RedisClusterNode redisClusterNode = onlyMe.node(0);
        assertThat(redisClusterNode.getFlags()).contains(RedisClusterNode.NodeFlag.MYSELF);

        assertThat(onlyMe.asMap()).hasSize(1);
    }

    @Test
    public void testDynamicNodeSelection() throws Exception {

        Partitions partitions = commands.getStatefulConnection().getPartitions();
        partitions.forEach(redisClusterNode -> redisClusterNode.setFlags(Collections.singleton(RedisClusterNode.NodeFlag.MASTER)));

        AsyncNodeSelection<String, String> selection = commands.nodes(
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
    public void testNodeSelectionAsyncPing() throws Exception {

        AsyncNodeSelection<String, String> onlyMe = commands.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.MYSELF));
        Map<RedisClusterNode, RedisAsyncCommands<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        AsyncExecutions<String> ping = onlyMe.commands().ping();
        CompletionStage<String> completionStage = ping.get(onlyMe.node(0));

        assertThat(completionStage.toCompletableFuture().get()).isEqualTo("PONG");
    }

    @Test
    public void testStaticNodeSelection() throws Exception {

        AsyncNodeSelection<String, String> selection = commands.nodes(
                redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF), false);

        assertThat(selection.asMap()).hasSize(1);

        commands.getStatefulConnection().getPartitions().getPartition(2)
                .setFlags(Collections.singleton(RedisClusterNode.NodeFlag.MYSELF));

        assertThat(selection.asMap()).hasSize(1);
    }

    @Test
    public void testAsynchronicityOfMultiNodeExecution() throws Exception {

        RedisAdvancedClusterAsyncCommands<String, String> connection2 = clusterClient.connect().async();

        AsyncNodeSelection<String, String> masters = connection2.masters();
        CompletableFuture.allOf(masters.commands().configSet("lua-time-limit", "10").futures()).get();
        AsyncExecutions<Object> eval = masters.commands().eval("while true do end", STATUS, new String[0]);

        for (CompletableFuture<Object> future : eval.futures()) {
            assertThat(future.isDone()).isFalse();
            assertThat(future.isCancelled()).isFalse();
        }
        Thread.sleep(200);

        AsyncExecutions<String> kill = commands.masters().commands().scriptKill();
        CompletableFuture.allOf(kill.futures()).get();

        for (CompletionStage<String> execution : kill) {
            assertThat(execution.toCompletableFuture().get()).isEqualTo("OK");
        }

        CompletableFuture.allOf(eval.futures()).exceptionally(throwable -> null).get();
        for (CompletableFuture<Object> future : eval.futures()) {
            assertThat(future.isDone()).isTrue();
        }
    }

    @Test
    public void testSlavesReadWrite() throws Exception {

        AsyncNodeSelection<String, String> nodes = commands.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.SLAVE));

        assertThat(nodes.size()).isEqualTo(2);

        commands.set(key, value).get();
        waitForReplication(key, port4);

        List<Throwable> t = new ArrayList<>();
        AsyncExecutions<String> keys = nodes.commands().get(key);
        keys.stream().forEach(lcs -> {
            lcs.toCompletableFuture().exceptionally(throwable -> {
                t.add(throwable);
                return null;
            });
        });

        CompletableFuture.allOf(keys.futures()).exceptionally(throwable -> null).get();

        assertThat(t.size()).isGreaterThan(0);
    }

    @Test
    public void testSlavesWithReadOnly() throws Exception {

        AsyncNodeSelection<String, String> nodes = commands.slaves(redisClusterNode -> redisClusterNode
                .is(RedisClusterNode.NodeFlag.SLAVE));

        assertThat(nodes.size()).isEqualTo(2);

        commands.set(key, value).get();
        waitForReplication(key, port4);

        List<Throwable> t = new ArrayList<>();
        List<String> strings = new ArrayList<>();
        AsyncExecutions<String> keys = nodes.commands().get(key);
        keys.stream().forEach(lcs -> {
            lcs.toCompletableFuture().exceptionally(throwable -> {
                t.add(throwable);
                return null;
            });
            lcs.thenAccept(strings::add);
        });

        CompletableFuture.allOf(keys.futures()).exceptionally(throwable -> null).get();
        Wait.untilEquals(1, () -> t.size()).waitOrTimeout();

        assertThat(t).hasSize(1);
        assertThat(strings).hasSize(1).contains(value);
    }

    protected void waitForReplication(String key, int port) throws Exception {
        waitForReplication(commands, key, port);
    }

    protected static void waitForReplication(RedisAdvancedClusterAsyncCommands<String, String> commands, String key, int port)
            throws Exception {

        AsyncNodeSelection<String, String> selection = commands
                .slaves(redisClusterNode -> redisClusterNode.getUri().getPort() == port);
        Wait.untilNotEquals(null, () -> {
            for (CompletableFuture<String> future : selection.commands().get(key).futures()) {
                if (future.get() != null) {
                    return future.get();
                }
            }
            return null;
        }).waitOrTimeout();
    }

}
