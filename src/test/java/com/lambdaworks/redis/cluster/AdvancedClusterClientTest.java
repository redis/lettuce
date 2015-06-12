package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
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

import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisFuture;
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

    private RedisAdvancedClusterAsyncCommands<String, String> connection;

    @Before
    public void before() throws Exception {
        ClusterSetup.setup2Master2Slaves(clusterRule);
        connection = clusterClient.connectClusterAsync();
    }

    @After
    public void after() throws Exception {
        connection.close();
    }

    @Test
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

        Partitions partitions = connection.getStatefulConnection().getPartitions();
        assertThat(partitions).hasSize(4);

        partitions.iterator().forEachRemaining(
                redisClusterNode -> System.out.println(redisClusterNode.getNodeId() + ": " + redisClusterNode.getFlags() + " "
                        + redisClusterNode.getUri()));
    }

    @Test
    public void doWeirdThingsWithClusterconnections() throws Exception {

        assertThat(clusterClient.getPartitions()).hasSize(4);

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            RedisClusterAsyncConnection<String, String> nodeConnection = connection.getConnection(redisClusterNode.getNodeId());

            nodeConnection.close();

            RedisClusterAsyncConnection<String, String> nextConnection = connection.getConnection(redisClusterNode.getNodeId());
            assertThat(connection).isNotSameAs(nextConnection);
        }
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

        CompletableFuture.allOf(connection.masters().commands().keys(result::add, "*").futures()).get();

        assertThat(result).hasSize(expectation.size());

        Collections.sort(expectation);
        Collections.sort(result);

        assertThat(result).isEqualTo(expectation);
    }

    @Test
    public void testNodeSelectionCount() throws Exception {
        assertThat(connection.all().size()).isEqualTo(4);
        assertThat(connection.slaves().size()).isEqualTo(2);
        assertThat(connection.masters().size()).isEqualTo(2);

        assertThat(connection.nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MYSELF)).size())
                .isEqualTo(1);
    }

    @Test
    public void testNodeSelection() throws Exception {

        AsyncNodeSelection<String, String> onlyMe = connection.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.MYSELF));

        Map<RedisClusterNode, RedisAsyncCommands<String, String>> map = onlyMe.asMap();

        assertThat(map).hasSize(1);

        RedisClusterAsyncConnection<String, String> node = onlyMe.node(0);
        assertThat(node).isNotNull();

        assertThat(onlyMe.iterator()).hasSize(1);
    }

    @Test
    public void testDynamicNodeSelection() throws Exception {

        AsyncNodeSelection<String, String> selection = connection.nodes(redisClusterNode -> redisClusterNode.getFlags()
                .contains(RedisClusterNode.NodeFlag.MYSELF), true);

        assertThat(selection).hasSize(1);

        connection.getStatefulConnection().getPartitions().getPartition(2)
                .setFlags(ImmutableSet.of(RedisClusterNode.NodeFlag.MYSELF));

        assertThat(selection).hasSize(2);
    }

    @Test
    public void testStaticNodeSelection() throws Exception {

        AsyncNodeSelection<String, String> selection = connection.nodes(redisClusterNode -> redisClusterNode.getFlags()
                .contains(RedisClusterNode.NodeFlag.MYSELF), false);

        assertThat(selection).hasSize(1);

        connection.getStatefulConnection().getPartitions().getPartition(2)
                .setFlags(ImmutableSet.of(RedisClusterNode.NodeFlag.MYSELF));

        assertThat(selection).hasSize(1);
    }

    @Test
    public void testAsynchronicityOfMultiNodeExecution() throws Exception {

        RedisAdvancedClusterAsyncCommands<String, String> connection2 = clusterClient.connectClusterAsync();

        AsyncNodeSelection<String, String> masters = connection2.masters();
        CompletableFuture.allOf(masters.commands().configSet("lua-time-limit", "10").futures()).get();
        AsyncExecutions<Object> eval = masters.commands().eval("while true do end", STATUS, new String[0]);
        assertThat(eval.nodes()).hasSize(2);

        for (CompletableFuture<Object> future : eval.futures()) {

            assertThat(future.isDone()).isFalse();
            assertThat(future.isCancelled()).isFalse();
        }

        AsyncExecutions<String> kill = connection.masters().commands().scriptKill();
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
    public void testSlavesWithoutReadOnly() throws Exception {

        AsyncNodeSelection<String, String> nodes = connection.nodes(redisClusterNode -> redisClusterNode.getFlags().contains(
                RedisClusterNode.NodeFlag.SLAVE));

        assertThat(nodes.size()).isEqualTo(2);

        connection.set(key, value).get();
        waitForReplication(key, port2);

        List<Throwable> t = Lists.newArrayList();
        AsyncExecutions<String> keys = nodes.commands().get(key);
        keys.stream().forEach(lcs -> {
            lcs.toCompletableFuture().exceptionally(throwable -> {
                t.add(throwable);
                return null;
            });
        });

        CompletableFuture.allOf(keys.futures()).exceptionally(throwable -> null).get();
        assertThat(t).hasSize(2);
    }

    @Test
    public void testSlavesWithReadOnly() throws Exception {


        AsyncNodeSelection<String, String> nodes = connection.slaves(redisClusterNode -> redisClusterNode
                .is(RedisClusterNode.NodeFlag.SLAVE));

        AsyncExecutions<List<String>> executions = nodes.commands().keys("*");
        executions.forEach(cs -> cs.thenAccept(keys -> System.out.println(keys)));

        assertThat(nodes.size()).isEqualTo(2);

        connection.set(key, value).get();
        waitForReplication(key, port2);

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
        WaitFor.waitOrTimeout(() -> {
            RedisClusterAsyncCommands<String, String> c2 = connection.getConnection(host, port);
            c2.readOnly();
            try {
                return c2.get(key).get() != null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }, timeout(seconds(5)));
    }

    @Test
    public void msetCrossSlot() throws Exception {

        Map<String, String> mset = prepareMset();

        RedisFuture<String> result = connection.mset(mset);

        assertThat(result.get()).isEqualTo("OK");

        for (String mykey : mset.keySet()) {
            String s1 = connection.get(mykey).get();
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

    @Test
    public void testSync() throws Exception {

        RedisAdvancedClusterCommands<String, String> sync = connection.getStatefulConnection().sync();
        sync.set(key, value);
        assertThat(sync.get(key)).isEqualTo(value);

        RedisClusterCommands<String, String> node2Connection = sync.getConnection(host, port2);
        assertThat(node2Connection.get(key)).isEqualTo(value);

        assertThat(sync.getStatefulConnection()).isSameAs(connection.getStatefulConnection());
    }
}
