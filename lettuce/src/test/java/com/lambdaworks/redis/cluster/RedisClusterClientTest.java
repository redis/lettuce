package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RedisClusterClientTest {

    public static final String host = "127.0.0.1";
    public static final int port1 = 7379;
    public static final int port2 = 7380;
    public static final int port3 = 7381;
    public static final int port4 = 7382;

    protected static RedisClient client1;
    protected static RedisClient client2;
    protected static RedisClient client3;
    protected static RedisClient client4;

    protected static RedisClusterClient clusterClient;

    protected RedisClusterAsyncConnection<String, String> redis1;
    protected RedisClusterAsyncConnection<String, String> redis2;
    protected RedisClusterAsyncConnection<String, String> redis3;
    protected RedisClusterAsyncConnection<String, String> redis4;

    protected String key = "key";
    protected String value = "value";

    @BeforeClass
    public static void setupClient() throws Exception {
        client1 = new RedisClient(host, port1);
        client2 = new RedisClient(host, port2);
        client3 = new RedisClient(host, port3);
        client4 = new RedisClient(host, port4);

        clusterClient = new RedisClusterClient(ImmutableList.of(RedisURI.Builder.redis(host, port1).build()));

    }

    private static int[] createSlots(int from, int to) {
        int[] result = new int[to - from];
        int counter = 0;
        for (int i = from; i < to; i++) {
            result[counter++] = i;

        }
        return result;
    }

    @AfterClass
    public static void shutdownClient() {

        client1.shutdown(0, 0, TimeUnit.MILLISECONDS);
        client2.shutdown(0, 0, TimeUnit.MILLISECONDS);
        client3.shutdown(0, 0, TimeUnit.MILLISECONDS);
        client4.shutdown(0, 0, TimeUnit.MILLISECONDS);
    }

    @Before
    public void before() throws Exception {
        redis1 = (RedisClusterAsyncConnection<String, String>) client1.connectAsync();
        redis2 = (RedisClusterAsyncConnection<String, String>) client2.connectAsync();
        redis3 = (RedisClusterAsyncConnection<String, String>) client3.connectAsync();
        redis4 = (RedisClusterAsyncConnection<String, String>) client4.connectAsync();

        redis1.flushall();
        redis2.flushall();
        redis3.flushall();
        redis4.flushall();

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                try {
                    String info = redis1.clusterInfo().get();
                    if (info != null && info.contains("cluster_state:ok")) {

                        String s = redis1.clusterNodes().get();
                        Partitions parse = ClusterPartitionParser.parse(s);

                        for (RedisClusterNode redisClusterNode : parse) {
                            if (redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.FAIL)
                                    || redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.EVENTUAL_FAIL)) {
                                return false;
                            }
                        }

                        return true;

                    }
                } catch (Exception e) {

                }

                return false;
            }
        }, timeout(seconds(5)), new ThreadSleep(Duration.millis(500)));
    }

    @After
    public void after() throws Exception {
        redis1.close();
        redis2.close();
        redis3.close();
        redis4.close();
    }

    @Test
    public void testClusterInfo() throws Exception {

        RedisFuture<String> future = redis1.clusterInfo();

        String status = future.get();

        assertThat(status, containsString("cluster_known_nodes:"));
        assertThat(status, containsString("cluster_slots_fail:0"));
        assertThat(status, containsString("cluster_state:"));
    }

    @Test
    public void testClusterNodes() throws Exception {

        RedisFuture<String> future = redis1.clusterNodes();

        String string = future.get();

        assertThat(string, containsString("connected"));
        assertThat(string, containsString("master"));
        assertThat(string, containsString("myself"));
    }

    @Test
    public void testClusterNodesSync() throws Exception {

        RedisClusterConnection<String, String> connection = clusterClient.connectCluster();

        String string = connection.clusterNodes();
        connection.close();

        assertThat(string, containsString("connected"));
        assertThat(string, containsString("master"));
        assertThat(string, containsString("myself"));
    }

    @Test
    public void zzzLastClusterSlaves() throws Exception {
        clusterClient.reloadPartitions();
        Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes().get());

        final RedisClusterNode node1 = Iterables.find(partitions, new Predicate<RedisClusterNode>() {
            @Override
            public boolean apply(RedisClusterNode input) {
                return input.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF);
            }
        });

        RedisFuture<String> replicate = redis4.clusterReplicate(node1.getNodeId());
        replicate.get();
        assertThat(replicate.getError()).isNull();
        assertThat(replicate.get()).isEqualTo("OK");

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                RedisFuture<List<String>> future = redis1.clusterSlaves(node1.getNodeId());
                try {
                    List<String> result = future.get();

                    return result.size() == 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }, timeout(seconds(5)));

    }

    @Test
    public void testClusteredOperations() throws Exception {

        SlotHash.getSlot("b".getBytes()); // 3300 -> Node 1 and Slave (Node 4)
        SlotHash.getSlot("a".getBytes()); // 15495 -> Node 3

        RedisFuture<String> result = redis1.set("b", "value");
        assertThat(result.getError()).isEqualTo(null);
        assertThat(redis3.set("a", "value").get()).isEqualTo("OK");

        RedisFuture<String> resultMoved = redis1.set("a", "value");
        resultMoved.get();
        assertThat(resultMoved.getError(), containsString("MOVED 15495"));
        assertThat(resultMoved.get()).isEqualTo(null);

        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        RedisFuture<String> setA = connection.set("a", "myValue1");
        setA.get();

        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        RedisFuture<String> setB = connection.set("b", "myValue2");
        assertThat(setB.get()).isEqualTo("OK");

        RedisFuture<String> setD = connection.set("d", "myValue2");
        assertThat(setD.get()).isEqualTo("OK");

        List<String> keys = connection.clusterGetKeysInSlot(SlotHash.getSlot("b".getBytes()), 10).get();
        assertThat(keys).isEqualTo(ImmutableList.of("b"));

        connection.close();

    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void testClusterRedirection() throws Exception {

        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();
        Partitions partitions = clusterClient.getPartitions();

        for (RedisClusterNode partition : partitions) {
            partition.setSlots(Lists.<Integer> newArrayList());
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                partition.getSlots().addAll(Ints.asList(createSlots(0, 16384)));
            }
        }

        // appropriate cluster node
        RedisFuture<String> setB = connection.set("b", "myValue1");

        assertThat(setB instanceof ClusterCommand).isTrue();

        ClusterCommand clusterCommandB = (ClusterCommand) setB;
        setB.get();
        assertThat(setB.getError()).isNull();
        assertThat(clusterCommandB.getExecutions()).isEqualTo(1);
        assertThat(setB.get()).isEqualTo("OK");
        assertThat(clusterCommandB.isMoved()).isFalse();

        // gets redirection to node 3
        RedisFuture<String> setA = connection.set("a", "myValue1");

        assertThat(setA instanceof ClusterCommand).isTrue();

        ClusterCommand clusterCommandA = (ClusterCommand) setA;
        setA.get();
        assertThat(setA.getError()).isNull();
        assertThat(clusterCommandA.getExecutions()).isEqualTo(2);
        assertThat(clusterCommandA.getExecutionLimit()).isEqualTo(5);
        assertThat(setA.get()).isEqualTo("OK");
        assertThat(clusterCommandA.isMoved()).isFalse();

        connection.close();

    }

    @Test
    public void testClusterConnectionStability() throws Exception {

        RedisAsyncConnectionImpl<String, String> connection = (RedisAsyncConnectionImpl<String, String>) clusterClient
                .connectClusterAsync();

        connection.set("a", "b");

        ClusterDistributionChannelWriter<String, String> writer = (ClusterDistributionChannelWriter<String, String>) connection
                .getChannelWriter();

        final RedisAsyncConnectionImpl<Object, Object> backendConnection = writer.getClusterConnectionProvider().getConnection(
                ClusterConnectionProvider.Intent.WRITE, 3300);

        backendConnection.set("a", "b");
        backendConnection.close();

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return backendConnection.isClosed() && !backendConnection.isOpen();
            }
        }, timeout(seconds(1)));
        assertThat(backendConnection.isClosed()).isTrue();
        assertThat(backendConnection.isOpen()).isFalse();

        assertThat(connection.isOpen()).isTrue();
        assertThat(connection.isClosed()).isFalse();

        connection.set("a", "b");

        RedisAsyncConnectionImpl<Object, Object> backendConnection2 = writer.getClusterConnectionProvider().getConnection(
                ClusterConnectionProvider.Intent.WRITE, 3300);

        assertThat(backendConnection2.isOpen()).isTrue();
        assertThat(backendConnection2.isClosed()).isFalse();

        assertThat(backendConnection2).isNotSameAs(backendConnection);

        connection.close();

    }

    @Test(timeout = 20000)
    public void distributedClusteredAccessAsync() throws Exception {

        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        List<RedisFuture<?>> futures = Lists.newArrayList();
        for (int i = 0; i < 100; i++) {
            futures.add(connection.set("a" + i, "myValue1" + i));
            futures.add(connection.set("b" + i, "myValue2" + i));
            futures.add(connection.set("d" + i, "myValue3" + i));
        }

        for (RedisFuture<?> future : futures) {
            future.get();
        }

        for (int i = 0; i < 100; i++) {
            RedisFuture<String> setA = connection.get("a" + i);
            RedisFuture<String> setB = connection.get("b" + i);
            RedisFuture<String> setD = connection.get("d" + i);

            setA.get();
            setB.get();
            setD.get();

            assertThat(setA.getError()).isNull();
            assertThat(setB.getError()).isNull();
            assertThat(setD.getError()).isNull();

            assertThat(setA.get()).isEqualTo("myValue1" + i);
            assertThat(setB.get()).isEqualTo("myValue2" + i);
            assertThat(setD.get()).isEqualTo("myValue3" + i);
        }

        connection.close();
    }

    @Test(timeout = 20000)
    public void distributedClusteredAccessSync() throws Exception {

        RedisClusterConnection<String, String> connection = clusterClient.connectCluster();

        for (int i = 0; i < 100; i++) {
            connection.set("a" + i, "myValue1" + i);
            connection.set("b" + i, "myValue2" + i);
            connection.set("d" + i, "myValue3" + i);
        }

        for (int i = 0; i < 100; i++) {

            assertThat(connection.get("a" + i)).isEqualTo("myValue1" + i);
            assertThat(connection.get("b" + i)).isEqualTo("myValue2" + i);
            assertThat(connection.get("d" + i)).isEqualTo("myValue3" + i);
        }

        connection.close();
    }
}
