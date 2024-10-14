package io.lettuce.core.cluster;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
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
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceSets;
import io.lettuce.core.output.StringListOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;
import io.lettuce.test.Wait;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
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
            TestFutures.awaitOrTimeout(commands.set(key, value));
        }

        List<String> result = new Vector<>();

        TestFutures.awaitOrTimeout(commands.masters().commands().keys(result::add, "*"));

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
            TestFutures.awaitOrTimeout(commands.set(key, value));
        }

        Collector<List<String>, List<String>, List<String>> collector = Collector.of(ArrayList::new, List::addAll, (a, b) -> a,
                it -> it);

        CompletableFuture<List<String>> future = commands.masters().commands().keys("*").thenCollect(collector)
                .toCompletableFuture();

        TestFutures.awaitOrTimeout(future);
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

        TestFutures.awaitOrTimeout(transformed);

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
                redisClusterNode -> redisClusterNode.setFlags(Collections.singleton(RedisClusterNode.NodeFlag.UPSTREAM)));

        AsyncNodeSelection<String, String> selection = commands
                .nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF), true);

        assertThat(selection.asMap()).hasSize(0);
        partitions.getPartition(0)
                .setFlags(LettuceSets.unmodifiableSet(RedisClusterNode.NodeFlag.MYSELF, RedisClusterNode.NodeFlag.UPSTREAM));
        assertThat(selection.asMap()).hasSize(1);

        partitions.getPartition(1)
                .setFlags(LettuceSets.unmodifiableSet(RedisClusterNode.NodeFlag.MYSELF, RedisClusterNode.NodeFlag.UPSTREAM));
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

        assertThat(TestFutures.getOrTimeout(completionStage)).isEqualTo("PONG");
    }

    @Test
    void testDispatch() {

        AsyncNodeSelection<String, String> all = commands.all();

        AsyncExecutions<List<String>> dispatched = all.commands().dispatch(CommandType.PING,
                () -> new StringListOutput<>(StringCodec.UTF8));

        List<List<String>> joined = dispatched.thenCollect(Collectors.toList()).toCompletableFuture().join();

        for (List<String> strings : joined) {
            assertThat(strings).hasSize(1).contains("PONG");
        }
    }

    @Test
    void testDispatchWithArgs() {

        AsyncNodeSelection<String, String> all = commands.all();

        AsyncExecutions<List<String>> dispatched = all.commands().dispatch(CommandType.ECHO,
                () -> new StringListOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).add("HELLO"));

        List<List<String>> joined = dispatched.thenCollect(Collectors.toList()).toCompletableFuture().join();

        for (List<String> strings : joined) {
            assertThat(strings).hasSize(1).contains("HELLO");
        }
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
    void testReplicaReadWrite() {

        AsyncNodeSelection<String, String> nodes = commands
                .nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.REPLICA));

        assertThat(nodes.size()).isEqualTo(2);

        TestFutures.awaitOrTimeout(commands.set(key, value));

        waitForReplication(key, ClusterTestSettings.port4);

        List<Throwable> t = new ArrayList<>();
        AsyncExecutions<String> keys = nodes.commands().get(key);
        keys.stream().forEach(lcs -> {
            lcs.toCompletableFuture().exceptionally(throwable -> {
                t.add(throwable);
                return null;
            });
        });

        TestFutures.awaitOrTimeout(CompletableFuture.allOf(keys.futures()).exceptionally(throwable -> null));

        assertThat(t.size()).isGreaterThan(0);
    }

    @Test
    void testReplicasWithReadOnly() {

        AsyncNodeSelection<String, String> nodes = commands
                .replicas(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.REPLICA));

        assertThat(nodes.size()).isEqualTo(2);

        TestFutures.awaitOrTimeout(commands.set(key, value));
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

        TestFutures.awaitOrTimeout(CompletableFuture.allOf(keys.futures()).exceptionally(throwable -> null));
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

                TestFutures.awaitOrTimeout(future);

                String result = TestFutures.getOrTimeout((Future<String>) future);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }).waitOrTimeout();
    }

}
