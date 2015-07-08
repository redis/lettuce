package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.*;
import static com.google.code.tempusfugit.temporal.Timeout.*;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.junit.*;
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
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotRange;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotsParser;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RedisClusterClientTest {

    public static final String host = TestSettings.hostAddr();
    public static final int port1 = 7379;
    public static final int port2 = 7380;
    public static final int port3 = 7381;
    public static final int port4 = 7382;

    protected static RedisClient client;
    protected static RedisClusterClient clusterClient;

    protected Logger log = Logger.getLogger(getClass());

    protected RedisClusterAsyncConnection<String, String> redis1;

    protected RedisClusterConnection<String, String> redissync1;
    protected RedisClusterConnection<String, String> redissync2;
    protected RedisClusterConnection<String, String> redissync3;
    protected RedisClusterConnection<String, String> redissync4;

    protected String key = "key";
    protected String value = "value";

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, port1, port2, port3, port4);

    @BeforeClass
    public static void setupClient() throws Exception {
        client = new RedisClient(host, port1);

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

        FastShutdown.shutdown(client);
    }

    @Before
    public void before() throws Exception {

        redis1 = (RedisClusterAsyncConnection<String, String>) client.connectAsync(RedisURI.Builder.redis(host, port1).build());

        redissync1 = (RedisClusterConnection<String, String>) client.connect(RedisURI.Builder.redis(host, port1).build());
        redissync2 = (RedisClusterConnection<String, String>) client.connect(RedisURI.Builder.redis(host, port2).build());
        redissync3 = (RedisClusterConnection<String, String>) client.connect(RedisURI.Builder.redis(host, port3).build());
        redissync4 = (RedisClusterConnection<String, String>) client.connect(RedisURI.Builder.redis(host, port4).build());

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return clusterRule.isStable();
            }
        }, timeout(seconds(5)), new ThreadSleep(Duration.millis(500)));

    }

    @After
    public void after() throws Exception {
        redis1.close();

        redissync1.close();
        redissync2.close();
        redissync3.close();
        redissync4.close();
    }

    @Test
    public void testClusterInfo() throws Exception {

        RedisFuture<String> future = redis1.clusterInfo();

        String status = future.get();

        assertThat(status).contains("cluster_known_nodes:");
        assertThat(status).contains("cluster_slots_fail:0");
        assertThat(status).contains("cluster_state:");
    }

    @Test
    public void testClusterNodes() throws Exception {

        String string = redissync1.clusterNodes();

        assertThat(string).contains("connected");
        assertThat(string).contains("master");
        assertThat(string).contains("myself");
    }

    @Test
    public void testClusterNodesSync() throws Exception {

        RedisClusterConnection<String, String> connection = clusterClient.connectCluster();

        String string = connection.clusterNodes();
        connection.close();

        assertThat(string).contains("connected");
        assertThat(string).contains("master");
        assertThat(string).contains("myself");
    }

    @Test
    public void testClusterSlaves() throws Exception {

        setNode4SlaveOfNode1();

        RedisFuture<Long> replication = redis1.waitForReplication(1, 5);
        assertThat(replication.get()).isEqualTo(0L);
    }

    private void setNode4SlaveOfNode1() throws InterruptedException, TimeoutException {
        clusterClient.reloadPartitions();
        Partitions partitions = ClusterPartitionParser.parse(redissync1.clusterNodes());

        final RedisClusterNode node1 = Iterables.find(partitions, new Predicate<RedisClusterNode>() {
            @Override
            public boolean apply(RedisClusterNode input) {
                return input.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF);
            }
        });

        String replicate = redissync4.clusterReplicate(node1.getNodeId());
        assertThat(replicate).isEqualTo("OK");

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return redissync1.clusterSlaves(node1.getNodeId()).size() == 1;
            }
        }, timeout(seconds(5)));
    }

    @Test
    public void testAsking() throws Exception {
        clusterClient.reloadPartitions();
        assertThat(redissync1.asking()).isEqualTo("OK");
    }

    @Test
    public void testClusterFailover() throws Exception {

        redissync4.clusterReplicate(getNodeId(redissync1));

        RedisClusterNode redis1Node = getOwnPartition(redissync1);
        RedisClusterNode redis4Node = getOwnPartition(redissync4);

        if (redis1Node.getFlags().contains(RedisClusterNode.NodeFlag.MASTER)) {

            log.info("Cluster node 1 is master");
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return getOwnPartition(redissync4).getFlags().contains(RedisClusterNode.NodeFlag.SLAVE);
                }
            }, timeout(seconds(10)));

            log.info("Cluster nodes seen from node 1:" + Layout.LINE_SEP + redissync1.clusterNodes());

            RedisFuture<String> future = redis1.clusterFailover(false);
            future.get();
            assertThat(future.getError()).isEqualTo("ERR You should send CLUSTER FAILOVER to a slave");

            String failover = redissync4.clusterFailover(true);
            assertThat(failover).isEqualTo("OK");
            new ThreadSleep(seconds(2));
            log.info("Cluster nodes seen from node 1 after clusterFailover:" + Layout.LINE_SEP + redissync1.clusterNodes());
            log.info("Cluster nodes seen from node 4 after clusterFailover:" + Layout.LINE_SEP + redissync4.clusterNodes());

            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return getOwnPartition(redissync1).getFlags().contains(RedisClusterNode.NodeFlag.SLAVE);
                }
            }, timeout(seconds(10)));

            redis1Node = getOwnPartition(redissync1);
            redis4Node = getOwnPartition(redissync4);

            assertThat(redis1Node.getFlags()).contains(RedisClusterNode.NodeFlag.SLAVE);
            assertThat(redis4Node.getFlags()).contains(RedisClusterNode.NodeFlag.MASTER);
        }

        if (redis4Node.getFlags().contains(RedisClusterNode.NodeFlag.MASTER)) {

            log.info("Cluster node 4 is master");
            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return getOwnPartition(redissync1).getFlags().contains(RedisClusterNode.NodeFlag.SLAVE);
                }
            }, timeout(seconds(10)));

            log.info("Cluster nodes seen from node 1:" + Layout.LINE_SEP + redissync1.clusterNodes());
            try {
                redissync4.clusterFailover(false);
            } catch (Exception e) {
                assertThat(e).hasMessage("ERR You should send CLUSTER FAILOVER to a slave");
            }

            RedisFuture<String> failover = redis1.clusterFailover(true);
            String result = failover.get();
            assertThat(failover.getError()).isNull();
            assertThat(result).isEqualTo("OK");

            new ThreadSleep(seconds(2));
            log.info("Cluster nodes seen from node 1 after clusterFailover:" + Layout.LINE_SEP + redissync1.clusterNodes());
            log.info("Cluster nodes seen from node 4 after clusterFailover:" + Layout.LINE_SEP + redissync4.clusterNodes());

            WaitFor.waitOrTimeout(new Condition() {
                @Override
                public boolean isSatisfied() {
                    return getOwnPartition(redissync4).getFlags().contains(RedisClusterNode.NodeFlag.SLAVE);
                }
            }, timeout(seconds(10)));

            redis1Node = getOwnPartition(redissync1);
            redis4Node = getOwnPartition(redissync4);

            assertThat(redis1Node.getFlags()).contains(RedisClusterNode.NodeFlag.MASTER);
            assertThat(redis4Node.getFlags()).contains(RedisClusterNode.NodeFlag.SLAVE);
        }

    }

    @Test
    public void testClusteredOperations() throws Exception {

        SlotHash.getSlot("b".getBytes()); // 3300 -> Node 1 and Slave (Node 4)
        SlotHash.getSlot("a".getBytes()); // 15495 -> Node 3

        RedisFuture<String> result = redis1.set("b", "value");
        assertThat(result.getError()).isEqualTo(null);
        assertThat(redissync3.set("a", "value")).isEqualTo("OK");

        RedisFuture<String> resultMoved = redis1.set("a", "value");
        resultMoved.get();
        assertThat(resultMoved.getError()).contains("MOVED 15495");
        assertThat(resultMoved.get()).isEqualTo(null);

        clusterClient.reloadPartitions();
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
    public void testReset() throws Exception {

        clusterClient.reloadPartitions();
        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        RedisFuture<String> setA = connection.set("a", "myValue1");
        setA.get();

        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        RedisFuture<String> setB = connection.set("b", "myValue2");
        assertThat(setB.get()).isEqualTo("OK");

        RedisFuture<String> setD = connection.set("d", "myValue2");
        assertThat(setD.get()).isEqualTo("OK");

        RedisChannelHandler<String, String> rch = (RedisChannelHandler<String, String>) connection;
        rch.reset();

        setA = connection.set("a", "myValue1");
        setA.get();

        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        setB = connection.set("b", "myValue2");
        assertThat(setB.get()).isEqualTo("OK");

        setD = connection.set("d", "myValue2");
        assertThat(setD.get()).isEqualTo("OK");

        connection.close();

    }

    @Test
    public void testClusterSlots() throws Exception {

        List<Object> reply = redissync1.clusterSlots();
        assertThat(reply.size()).isGreaterThan(1);

        List<ClusterSlotRange> parse = ClusterSlotsParser.parse(reply);
        assertThat(parse).hasSize(7);

        ClusterSlotRange clusterSlotRange = parse.get(0);
        assertThat(clusterSlotRange.getFrom()).isEqualTo(0);
        assertThat(clusterSlotRange.getTo()).isEqualTo(6999);

        assertThat(clusterSlotRange.getMaster()).isNotNull();
        assertThat(clusterSlotRange.getSlaves()).isNotNull();
        assertThat(clusterSlotRange.toString()).contains(ClusterSlotRange.class.getSimpleName());

        ClusterSlotRange clusterSlotRange2 = parse.get(1);
        assertThat(clusterSlotRange2.getFrom()).isEqualTo(7000);
        assertThat(clusterSlotRange2.getTo()).isEqualTo(7000);
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void testClusterRedirection() throws Exception {

        RedisAdvancedClusterAsyncConnectionImpl<String, String> connection = (RedisAdvancedClusterAsyncConnectionImpl) clusterClient
                .connectClusterAsync();
        Partitions partitions = clusterClient.getPartitions();

        for (RedisClusterNode partition : partitions) {
            partition.setSlots(Lists.<Integer> newArrayList());
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                partition.getSlots().addAll(Ints.asList(createSlots(0, 16384)));
            }
        }
        partitions.updateCache();
        connection.setPartitions(partitions);

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
        }, timeout(seconds(5)));
        assertThat(backendConnection.isClosed()).isTrue();
        assertThat(backendConnection.isOpen()).isFalse();

        assertThat(connection.isOpen()).isTrue();
        assertThat(connection.isClosed()).isFalse();

        connection.set("a", "b");

        RedisAsyncConnectionImpl<Object, Object> backendConnection2 = writer.getClusterConnectionProvider().getConnection(
                ClusterConnectionProvider.Intent.WRITE, 3300);

        assertThat(backendConnection2.isOpen()).isFalse();
        assertThat(backendConnection2.isClosed()).isTrue();

        assertThat(backendConnection2).isSameAs(backendConnection);

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

    @Test
    public void readOnlyReadWrite() throws Exception {

        setNode4SlaveOfNode1();

        redis1.set("b", value);

        String resultB = redis1.get("b").get();
        assertThat(resultB).isEqualTo(value);
        Thread.sleep(500); // give some time to replicate

        // assume cluster node 4 is a slave for the master
        final RedisConnection<String, String> connect4 = client.connect(RedisURI.Builder.redis(host, port4).build());

        try {
            connect4.get("b");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("MOVED");
        }

        String readOnly = connect4.readOnly();
        assertThat(readOnly).isEqualTo("OK");

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return connect4.get("b") != null;
            }
        }, timeout(seconds(5)));

        String resultBViewedBySlave = connect4.get("b");
        assertThat(resultBViewedBySlave).isEqualTo(value);
        connect4.quit();

        resultBViewedBySlave = connect4.get("b");
        assertThat(resultBViewedBySlave).isEqualTo(value);

        connect4.readWrite();
        try {
            connect4.get("b");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("MOVED");
        }
    }

    @Test
    public void testNoClusterNodeAvailable() throws Exception {

        RedisClusterClient clusterClient = new RedisClusterClient(RedisURI.Builder.redis(host, 40400).build());
        try {
            clusterClient.connectCluster();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasCauseInstanceOf(RedisException.class).hasRootCauseInstanceOf(ConnectException.class);
        }
    }
}
