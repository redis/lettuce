package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
<<<<<<< HEAD
import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.redis.RedisException;
=======
import static com.lambdaworks.redis.ScriptOutputType.STATUS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class AdvancedClusterClientTest extends AbstractClusterTest {

    private RedisAdvancedClusterAsyncConnection<String, String> connection;

    @Before
    public void before() throws Exception {

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return clusterRule.isStable();
            }
        }, timeout(seconds(5)), new ThreadSleep(Duration.millis(500)));

        connection = clusterClient.connectClusterAsync();
    }

    @After
    public void after() throws Exception {
        connection.close();
    }

    @Test
<<<<<<< HEAD
    public void nodeConnections() throws Exception {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncConnection<String, String> nodeConnection = connection.getConnection(redisClusterNode.getNodeId());

            String myid = nodeConnection.clusterMyId().get();
            assertThat(myid).isEqualTo(redisClusterNode.getNodeId());
        }
    }

    @Test(expected = RedisException.class)
    public void unknownNodeId() throws Exception {

        connection.getConnection("unknown");
    }

    @Test(expected = RedisException.class)
    public void invalidHost() throws Exception {
        connection.getConnection("invalid-host", -1);
    }
    
    @Test
    public void partitions() throws Exception {

        Partitions partitions = connection.getPartitions();
        assertThat(partitions).hasSize(4);

        partitions.iterator().forEachRemaining(
                redisClusterNode -> System.out.println(redisClusterNode.getNodeId() + ": " + redisClusterNode.getFlags() + " "
                        + redisClusterNode.getUri()));
    }

    @Test
    public void testNodeSelectionCount() throws Exception {
        assertThat(connection.all().size()).isEqualTo(4);
        assertThat(connection.slaves().size()).isEqualTo(0);
        assertThat(connection.masters().size()).isEqualTo(4);

        assertThat(connection.nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MYSELF)).size())
                .isEqualTo(1);
    }

    @Test
    public void testNodeSelection() throws Exception {

        NodeSelection<String, String> onlyMe = connection.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.MYSELF));

        Map<RedisClusterNode, RedisClusterAsyncConnection<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        RedisClusterAsyncConnection<String, String> node = onlyMe.node(0);
        assertThat(node).isNotNull();
    }

    @Test
    public void doWeirdThingsWithClusterconnections() throws Exception {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncConnection<String, String> nodeConnection = connection.getConnection(redisClusterNode.getNodeId());

            nodeConnection.close();

            RedisClusterAsyncConnection<String, String> nextConnection = connection.getConnection(redisClusterNode.getNodeId());
            assertThat(connection).isNotSameAs(nextConnection);

=======
    public void partitions() throws Exception {

        Partitions partitions = connection.getPartitions();
        assertThat(partitions).hasSize(4);

        partitions.iterator().forEachRemaining(
                redisClusterNode -> System.out.println(redisClusterNode.getNodeId() + ": " + redisClusterNode.getFlags() + " "
                        + redisClusterNode.getUri()));
    }

    @Test
    public void testNodeSelectionCount() throws Exception {
        assertThat(connection.all().size()).isEqualTo(4);
        assertThat(connection.slaves().size()).isEqualTo(0);
        assertThat(connection.masters().size()).isEqualTo(4);

        assertThat(connection.nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MYSELF)).size())
                .isEqualTo(1);
    }

    @Test
    public void testNodeSelection() throws Exception {

        NodeSelection<String, String> onlyMe = connection.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.MYSELF));

        Map<RedisClusterNode, RedisClusterAsyncConnection<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        RedisClusterAsyncConnection<String, String> node = onlyMe.node(0);
        assertThat(node).isNotNull();
    }

    @Test
    public void testMultiNodeOperations() throws Exception {

        List<String> expectation = Lists.newArrayList();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            expectation.add(key);
            connection.set(key, value);
        }

        List<String> result = new Vector<>();

        CompletableFuture.allOf(connection.masters().keys(result::add, "*").futures()).get();

        assertThat(result).hasSize(expectation.size());

        Collections.sort(expectation);
        Collections.sort(result);

        assertThat(result).isEqualTo(expectation);
    }

    @Test
    public void partitions() throws Exception {

        Partitions partitions = connection.getPartitions();
        assertThat(partitions).hasSize(4);

        partitions.iterator().forEachRemaining(
                redisClusterNode -> System.out.println(redisClusterNode.getNodeId() + ": " + redisClusterNode.getFlags() + " "
                        + redisClusterNode.getUri()));
    }

    @Test
    public void testNodeSelectionCount() throws Exception {
        assertThat(connection.all().size()).isEqualTo(4);
        assertThat(connection.slaves().size()).isEqualTo(0);
        assertThat(connection.masters().size()).isEqualTo(4);

        assertThat(connection.nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MYSELF)).size())
                .isEqualTo(1);
    }

    @Test
    public void testNodeSelection() throws Exception {

        NodeSelection<String, String> onlyMe = connection.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.MYSELF));

        Map<RedisClusterNode, RedisClusterAsyncConnection<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        RedisClusterAsyncConnection<String, String> node = onlyMe.node(0);
        assertThat(node).isNotNull();
    }

    @Test
    public void testMultiNodeOperations() throws Exception {

        List<String> expectation = Lists.newArrayList();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            expectation.add(key);
            connection.set(key, value);
        }

        List<String> result = new Vector<>();

        CompletableFuture.allOf(connection.masters().keys(result::add, "*").futures()).get();

        assertThat(result).hasSize(expectation.size());

        Collections.sort(expectation);
        Collections.sort(result);

        assertThat(result).isEqualTo(expectation);
    }

    @Test
    public void testAsynchronicityOfMultiNodeExeccution() throws Exception {

        RedisAdvancedClusterConnection<String, String> connection2 = clusterClient.connectClusterAsync();

        NodeSelectionAsyncOperations<String, String> masters = connection2.masters();
        CompletableFuture.allOf(masters.configSet("lua-time-limit", "10").futures()).get();
        AsyncExecutions<Object> eval = masters.eval("while true do end", STATUS, new String[0]);
        assertThat(eval.nodes()).hasSize(4);

        for (CompletableFuture<Object> future : eval.futures()) {

            assertThat(future.isDone()).isFalse();
            assertThat(future.isCancelled()).isFalse();
        }

        AsyncExecutions<String> kill = connection.masters().scriptKill();
        CompletableFuture.allOf(kill.futures()).get();

        for (CompletionStage<String> execution : kill) {
            assertThat(execution.toCompletableFuture().get()).isEqualTo("OK");
        }

        CompletableFuture.allOf(eval.futures()).exceptionally(throwable -> {
            return null;
        }).get();

        for (CompletableFuture<Object> future : eval.futures()) {
            assertThat(future.isDone()).isTrue();
        }
    }

    @Test
    public void msetCrossSlot() throws Exception {

        Map<String, String> mset = Maps.newHashMap();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            mset.put(key, "value-" + key);
        }

        RedisFuture<String> result = connection.mset(mset);

        assertThat(result.get()).isEqualTo("OK");

        for (String mykey : mset.keySet()) {
            String s1 = connection.get(mykey).get();
            assertThat(s1).isEqualTo("value-" + mykey);
        }
    }

    @Test
    public void msetnxCrossSlot() throws Exception {

        Map<String, String> mset = Maps.newHashMap();
        for (char c = 'a'; c < 'z'; c++) {
            String key = new String(new char[] { c, c, c });
            mset.put(key, "value-" + key);
        }

        RedisFuture<Boolean> result = connection.msetnx(mset);

        assertThat(result.get()).isTrue();

        for (String mykey : mset.keySet()) {
            String s1 = connection.get(mykey).get();
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

        RedisFuture<List<String>> result = connection.mget(keys.toArray(new String[keys.size()]));

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

        RedisFuture<Long> result = connection.del(keys.toArray(new String[keys.size()]));

        assertThat(result.get()).isEqualTo(25);

        for (String mykey : keys) {
            String s1 = connection.get(mykey).get();
            assertThat(s1).isNull();
        }

    }
}
