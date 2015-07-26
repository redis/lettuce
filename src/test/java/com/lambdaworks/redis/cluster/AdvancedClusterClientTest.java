package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.ScriptOutputType.STATUS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lambdaworks.Wait;
import com.lambdaworks.redis.LettuceFutures;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.AsyncExecutions;
import com.lambdaworks.redis.cluster.api.async.AsyncNodeSelection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class AdvancedClusterClientTest extends AbstractClusterTest {

    private RedisAdvancedClusterAsyncCommands<String, String> commands;

    @Before
    public void before() throws Exception {
        clusterClient.reloadPartitions();
        commands = clusterClient.connectClusterAsync();
    }

    @After
    public void after() throws Exception {
        commands.close();
    }

    @Test
    public void nodeConnections() throws Exception {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncConnection<String, String> nodeConnection = commands.getConnection(redisClusterNode.getNodeId());

            String myid = nodeConnection.clusterMyId().get();
            assertThat(myid).isEqualTo(redisClusterNode.getNodeId());
        }
    }

    @Test(expected = RedisException.class)
    public void unknownNodeId() throws Exception {

        commands.getConnection("unknown");
    }

    @Test(expected = RedisException.class)
    public void invalidHost() throws Exception {
        commands.getConnection("invalid-host", -1);
    }

    @Test
    public void partitions() throws Exception {

        Partitions partitions = commands.getStatefulConnection().getPartitions();
        assertThat(partitions).hasSize(4);
    }

    @Test
    public void doWeirdThingsWithClusterconnections() throws Exception {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncConnection<String, String> nodeConnection = commands.getConnection(redisClusterNode.getNodeId());

            nodeConnection.close();

            RedisClusterAsyncConnection<String, String> nextConnection = commands.getConnection(redisClusterNode.getNodeId());
            assertThat(commands).isNotSameAs(nextConnection);
        }
    }

    @Test
    public void differentConnections() throws Exception {

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncConnection<String, String> nodeId = commands.getConnection(redisClusterNode.getNodeId());
            RedisClusterAsyncConnection<String, String> hostAndPort = commands.getConnection(redisClusterNode.getUri()
                    .getHost(), redisClusterNode.getUri().getPort());

            assertThat(nodeId).isNotSameAs(hostAndPort);
        }

        StatefulRedisClusterConnection<String, String> statefulConnection = commands.getStatefulConnection();
        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {

            StatefulRedisConnection<String, String> nodeId = statefulConnection.getConnection(redisClusterNode.getNodeId());
            StatefulRedisConnection<String, String> hostAndPort = statefulConnection.getConnection(redisClusterNode.getUri()
                    .getHost(), redisClusterNode.getUri().getPort());

            assertThat(nodeId).isNotSameAs(hostAndPort);
        }

        RedisAdvancedClusterCommands<String, String> sync = statefulConnection.sync();
        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {

            RedisClusterCommands<String, String> nodeId = sync.getConnection(redisClusterNode.getNodeId());
            RedisClusterCommands<String, String> hostAndPort = sync.getConnection(redisClusterNode.getUri().getHost(),
                    redisClusterNode.getUri().getPort());

            assertThat(nodeId).isNotSameAs(hostAndPort);
        }

        RedisAdvancedClusterReactiveCommands<String, String> rx = statefulConnection.reactive();
        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {

            RedisClusterReactiveCommands<String, String> nodeId = rx.getConnection(redisClusterNode.getNodeId());
            RedisClusterReactiveCommands<String, String> hostAndPort = rx.getConnection(redisClusterNode.getUri().getHost(),
                    redisClusterNode.getUri().getPort());

            assertThat(nodeId).isNotSameAs(hostAndPort);
        }
    }

    @Test
    public void testMultiNodeOperations() throws Exception {

        List<String> expectation = Lists.newArrayList();
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

        RedisClusterAsyncCommands<String, String> node = onlyMe.node(0);
        assertThat(node).isNotNull();

        RedisClusterNode redisClusterNode = onlyMe.get(0);
        assertThat(redisClusterNode.getFlags()).contains(RedisClusterNode.NodeFlag.MYSELF);

        assertThat(onlyMe.iterator()).hasSize(1);
    }

    @Test
    public void testDynamicNodeSelection() throws Exception {

        Partitions partitions = commands.getStatefulConnection().getPartitions();
        partitions.forEach(redisClusterNode -> redisClusterNode.setFlags(ImmutableSet.of(RedisClusterNode.NodeFlag.MASTER)));

        AsyncNodeSelection<String, String> selection = commands.nodes(
                redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF), true);

        assertThat(selection).hasSize(0);
        partitions.getPartition(0)
                .setFlags(ImmutableSet.of(RedisClusterNode.NodeFlag.MYSELF, RedisClusterNode.NodeFlag.MASTER));
        assertThat(selection).hasSize(1);

        partitions.getPartition(1)
                .setFlags(ImmutableSet.of(RedisClusterNode.NodeFlag.MYSELF, RedisClusterNode.NodeFlag.MASTER));
        assertThat(selection).hasSize(2);

    }

    @Test
    public void testNodeSelectionAsyncPing() throws Exception {

        AsyncNodeSelection<String, String> onlyMe = commands.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.MYSELF));
        Map<RedisClusterNode, RedisAsyncCommands<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        AsyncExecutions<String> ping = onlyMe.commands().ping();
        RedisClusterNode redisClusterNode = onlyMe.get(0);
        CompletionStage<String> completionStage = ping.get(onlyMe.get(0));

        assertThat(completionStage.toCompletableFuture().get()).isEqualTo("PONG");
    }

    @Test
    public void testStaticNodeSelection() throws Exception {

        AsyncNodeSelection<String, String> selection = commands.nodes(
                redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF), false);

        assertThat(selection).hasSize(1);

        commands.getStatefulConnection().getPartitions().getPartition(2)
                .setFlags(ImmutableSet.of(RedisClusterNode.NodeFlag.MYSELF));

        assertThat(selection).hasSize(1);
    }

    @Test
    public void testAsynchronicityOfMultiNodeExecution() throws Exception {

        RedisAdvancedClusterAsyncCommands<String, String> connection2 = clusterClient.connectClusterAsync();

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

        List<Throwable> t = Lists.newArrayList();
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

        List<Throwable> t = Lists.newArrayList();
        List<String> strings = Lists.newArrayList();
        AsyncExecutions<String> keys = nodes.commands().get(key);
        keys.stream().forEach(lcs -> {
            lcs.toCompletableFuture().exceptionally(throwable -> {
                t.add(throwable);
                return null;
            });
            lcs.thenAccept(strings::add);
        });

        CompletableFuture.allOf(keys.futures()).exceptionally(throwable -> null).get();

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

    @Test
    public void msetCrossSlot() throws Exception {

        Map<String, String> mset = prepareMset();

        RedisFuture<String> result = commands.mset(mset);

        assertThat(result.get()).isEqualTo("OK");

        for (String mykey : mset.keySet()) {
            String s1 = commands.get(mykey).get();
            assertThat(s1).isEqualTo("value-" + mykey);
        }
    }

    protected Map<String, String> prepareMset() {
        Map<String, String> mset = Maps.newHashMap();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            mset.put(key, "value-" + key);
        }
        return mset;
    }

    @Test
    public void msetnxCrossSlot() throws Exception {

        Map<String, String> mset = prepareMset();

        RedisFuture<Boolean> result = commands.msetnx(mset);

        assertThat(result.get()).isTrue();

        for (String mykey : mset.keySet()) {
            String s1 = commands.get(mykey).get();
            assertThat(s1).isEqualTo("value-" + mykey);
        }
    }

    @Test
    public void mgetCrossSlot() throws Exception {

        msetCrossSlot();
        List<String> keys = Lists.newArrayList();
        List<String> expectation = Lists.newArrayList();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            keys.add(key);
            expectation.add("value-" + key);
        }

        RedisFuture<List<String>> result = commands.mget(keys.toArray(new String[keys.size()]));

        assertThat(result.get()).hasSize(keys.size());
        assertThat(result.get()).isEqualTo(expectation);

    }

    @Test
    public void delCrossSlot() throws Exception {

        msetCrossSlot();
        List<String> keys = Lists.newArrayList();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            keys.add(key);
        }

        RedisFuture<Long> result = commands.del(keys.toArray(new String[keys.size()]));

        assertThat(result.get()).isEqualTo(25);

        for (String mykey : keys) {
            String s1 = commands.get(mykey).get();
            assertThat(s1).isNull();
        }

    }

    @Test
    public void testSync() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = commands.getStatefulConnection().sync();
        sync.set(key, value);
        assertThat(sync.get(key)).isEqualTo(value);

        RedisClusterCommands<String, String> node2Connection = sync.getConnection(host, port2);
        assertThat(node2Connection.get(key)).isEqualTo(value);

        assertThat(sync.getStatefulConnection()).isSameAs(commands.getStatefulConnection());
    }

    @Test
    public void noAddr() throws Exception {

        RedisAdvancedClusterConnection<String, String> sync = clusterClient.connectCluster();
        try {

            Partitions partitions = clusterClient.getPartitions();
            for (RedisClusterNode partition : partitions) {
                partition.setUri(RedisURI.create("redis://non.existent.host:1234"));
            }

            sync.set("A", "value");// 6373
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("Unable to connect to");
        }
        sync.close();
    }

    @Test
    public void forbiddenHostOnRedirect() throws Exception {

        RedisAdvancedClusterConnection<String, String> sync = clusterClient.connectCluster();
        try {

            Partitions partitions = clusterClient.getPartitions();
            for (RedisClusterNode partition : partitions) {
                partition.setSlots(ImmutableList.of(0));
                if (partition.getUri().getPort() == 7380) {
                    partition.setSlots(ImmutableList.of(6373));
                } else {
                    partition.setUri(RedisURI.create("redis://non.existent.host:1234"));
                }
            }

            partitions.updateCache();

            sync.set("A", "value");// 6373
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("not allowed");
        }
        sync.close();
    }

    @Test
    public void getConnectionToNotAClusterMember() throws Exception {

        RedisAdvancedClusterConnection<String, String> sync = clusterClient.connectCluster();
        try {
            sync.getConnection("8.8.8.8", 1234);
        } catch (RedisException e) {
            assertThat(e).hasRootCauseExactlyInstanceOf(IllegalArgumentException.class);
        }
        sync.close();
    }

    @Test
    public void pipelining() throws Exception {

        RedisAdvancedClusterConnection<String, String> verificationConnection = clusterClient.connectCluster();

        // preheat the first connection
        commands.get(key(0)).get();

        int iterations = 1000;
        commands.setAutoFlushCommands(false);
        List<RedisFuture<?>> futures = Lists.newArrayList();
        for (int i = 0; i < iterations; i++) {
            futures.add(commands.set(key(i), value(i)));
        }

        for (int i = 0; i < iterations; i++) {
            assertThat(verificationConnection.get(key(i))).as("Key " + key(i) + " must be null").isNull();
        }

        commands.flushCommands();
        boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]));
        assertThat(result).isTrue();

        for (int i = 0; i < iterations; i++) {
            assertThat(verificationConnection.get(key(i))).as("Key " + key(i) + " must be " + value(i)).isEqualTo(value(i));
        }

        verificationConnection.close();

    }

    protected String value(int i) {
        return value + "-" + i;
    }

    protected String key(int i) {
        return key + "-" + i;
    }

}
