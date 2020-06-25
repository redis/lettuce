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

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.stream.Collector;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.TestSupport;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.AsyncExecutions;
import io.lettuce.core.cluster.api.async.AsyncNodeSelection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.internal.LettuceSets;
import io.lettuce.test.Delay;
import io.lettuce.test.Futures;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class NodeSelectionAsyncIntegrationTests extends TestSupport {

    private final RedisClusterClient clusterClient;

    private final RedisAdvancedClusterAsyncCommands<String, String> commands;

    @Inject
    NodeSelectionAsyncIntegrationTests(RedisClusterClient clusterClient,
            StatefulRedisClusterConnection<String, String> connection) {

        this.clusterClient = clusterClient;
        this.commands = connection.async();
        connection.sync().flushall();
    }

    @Test
    void testMultiNodeOperations() {

        List<String> expectation = new ArrayList<>();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            expectation.add(key);
            Futures.await(commands.set(key, value));
        }

        List<String> result = new Vector<>();

        Futures.await(commands.masters().commands().keys(result::add, "*"));

        assertThat(result).hasSize(expectation.size());

        Collections.sort(expectation);
        Collections.sort(result);

        assertThat(result).isEqualTo(expectation);
    }

    @Test
    void testThenCollect() {

        List<String> expectation = new ArrayList<>();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            expectation.add(key);
            Futures.await(commands.set(key, value));
        }

        Collector<List<String>, List<String>, List<String>> collector = Collector.of(ArrayList::new, List::addAll, (a, b) -> a,
                it -> it);

        CompletableFuture<List<String>> future = commands.masters().commands().keys("*").thenCollect(collector)
                .toCompletableFuture();

        Futures.await(future);
        List<String> result = future.join();

        assertThat(result).hasSize(expectation.size());

        Collections.sort(expectation);
        Collections.sort(result);

        assertThat(result).isEqualTo(expectation);
    }

    @Test
    void testCompletionStageTransformation() {

        CompletableFuture<String> transformed = commands.masters().commands().ping()
                .thenApply(it -> String.join(" ", it.toArray(new String[0]))).toCompletableFuture();

        Futures.await(transformed);

        assertThat(transformed.join()).isEqualTo("PONG PONG");
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

        AsyncNodeSelection<String, String> onlyMe = commands
                .nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF));
        Map<RedisClusterNode, RedisAsyncCommands<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        RedisClusterAsyncCommands<String, String> node = onlyMe.commands(0);
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

        AsyncNodeSelection<String, String> selection = commands
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
    void testNodeSelectionAsyncPing() {

        AsyncNodeSelection<String, String> onlyMe = commands
                .nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF));
        Map<RedisClusterNode, RedisAsyncCommands<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        AsyncExecutions<String> ping = onlyMe.commands().ping();
        CompletionStage<String> completionStage = ping.get(onlyMe.node(0));

        assertThat(Futures.get(completionStage)).isEqualTo("PONG");
    }

    @Test
    void testStaticNodeSelection() {

        AsyncNodeSelection<String, String> selection = commands
                .nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF), false);

        assertThat(selection.asMap()).hasSize(1);

        commands.getStatefulConnection().getPartitions().getPartition(2)
                .setFlags(Collections.singleton(RedisClusterNode.NodeFlag.MYSELF));

        assertThat(selection.asMap()).hasSize(1);

        clusterClient.reloadPartitions();
    }

    @Test
    void testAsynchronicityOfMultiNodeExecution() {

        StatefulRedisClusterConnection<String, String> connection2 = clusterClient.connect();
        RedisAdvancedClusterAsyncCommands<String, String> async2 = connection2.async();

        AsyncNodeSelection<String, String> masters = async2.masters();
        Futures.await(masters.commands().configSet("lua-time-limit", "10"));

        AsyncExecutions<Object> eval = masters.commands().eval("while true do end", STATUS, new String[0]);

        for (CompletableFuture<Object> future : eval.futures()) {
            assertThat(future.isDone()).isFalse();
            assertThat(future.isCancelled()).isFalse();
        }
        Delay.delay(Duration.ofMillis(200));

        AsyncExecutions<String> kill = commands.masters().commands().scriptKill();
        Futures.await(kill);

        for (CompletionStage<String> execution : kill) {
            assertThat(Futures.get(execution)).isEqualTo("OK");
        }

        Futures.await(CompletableFuture.allOf(eval.futures()).exceptionally(throwable -> null));
        for (CompletableFuture<Object> future : eval.futures()) {
            assertThat(future.isDone()).isTrue();
        }

        connection2.close();
    }

    @Test
    void testReplicaReadWrite() {

        AsyncNodeSelection<String, String> nodes = commands
                .nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.REPLICA));

        assertThat(nodes.size()).isEqualTo(2);

        Futures.await(commands.set(key, value));

        waitForReplication(key, ClusterTestSettings.port4);

        List<Throwable> t = new ArrayList<>();
        AsyncExecutions<String> keys = nodes.commands().get(key);
        keys.stream().forEach(lcs -> {
            lcs.toCompletableFuture().exceptionally(throwable -> {
                t.add(throwable);
                return null;
            });
        });

        Futures.await(CompletableFuture.allOf(keys.futures()).exceptionally(throwable -> null));

        assertThat(t.size()).isGreaterThan(0);
    }

    @Test
    void testReplicasWithReadOnly() {

        AsyncNodeSelection<String, String> nodes = commands
                .replicas(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.REPLICA));

        assertThat(nodes.size()).isEqualTo(2);

        Futures.await(commands.set(key, value));
        waitForReplication(key, ClusterTestSettings.port4);

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

        Futures.await(CompletableFuture.allOf(keys.futures()).exceptionally(throwable -> null));
        Wait.untilEquals(1, t::size).waitOrTimeout();

        assertThat(t).hasSize(1);
        assertThat(strings).hasSize(1).contains(value);
    }

    void waitForReplication(String key, int port) {
        waitForReplication(commands, key, port);
    }

    static void waitForReplication(RedisAdvancedClusterAsyncCommands<String, String> commands, String key, int port) {

        AsyncNodeSelection<String, String> selection = commands
                .replicas(redisClusterNode -> redisClusterNode.getUri().getPort() == port);
        Wait.untilNotEquals(null, () -> {
            for (CompletableFuture<String> future : selection.commands().get(key).futures()) {

                Futures.await(future);

                String result = Futures.get((Future<String>) future);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }).waitOrTimeout();
    }

}
