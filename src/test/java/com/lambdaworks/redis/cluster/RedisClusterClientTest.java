package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getNodeId;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getOwnPartition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Optional;
import com.lambdaworks.redis.metrics.CommandLatencyId;
import com.lambdaworks.redis.metrics.CommandMetrics;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
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
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotRange;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotsParser;
import com.lambdaworks.redis.metrics.CommandLatencyCollector;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RedisClusterClientTest extends AbstractClusterTest {

    protected static RedisClient client;

    protected Logger log = Logger.getLogger(getClass());

    protected RedisClusterAsyncConnection<String, String> redis1;

    protected RedisClusterConnection<String, String> redissync1;
    protected RedisClusterConnection<String, String> redissync2;
    protected RedisClusterConnection<String, String> redissync3;
    protected RedisClusterConnection<String, String> redissync4;

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, port1, port2, port3, port4);

    @BeforeClass
    public static void setupClient() throws Exception {
        client = new RedisClient(host, port1);
        clusterClient = new RedisClusterClient(ImmutableList.of(RedisURI.Builder.redis(host, port1).build()));

    }

    public static int[] createSlots(int from, int to) {
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

        redis1 = client.connectAsync(RedisURI.Builder.redis(host, port1).build());

        redissync1 = client.connect(RedisURI.Builder.redis(host, port1).build());
        redissync2 = client.connect(RedisURI.Builder.redis(host, port2).build());
        redissync3 = client.connect(RedisURI.Builder.redis(host, port3).build());
        redissync4 = client.connect(RedisURI.Builder.redis(host, port4).build());

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return clusterRule.isStable();
            }
        }, timeout(seconds(5)), new ThreadSleep(Duration.millis(500)));

        clusterClient.reloadPartitions();

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
        assertThat(replication.get()).isGreaterThanOrEqualTo(0L);
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

        SlotHash.getSlot(KEY_B.getBytes()); // 3300 -> Node 1 and Slave (Node 4)
        SlotHash.getSlot(KEY_A.getBytes()); // 15495 -> Node 3

        RedisFuture<String> result = redis1.set(KEY_B, "value");
        assertThat(result.getError()).isEqualTo(null);
        assertThat(redissync3.set(KEY_A, "value")).isEqualTo("OK");

        RedisFuture<String> resultMoved = redis1.set(KEY_A, "value");
        resultMoved.get();
        assertThat(resultMoved.getError()).contains("MOVED 15495");
        assertThat(resultMoved.get()).isEqualTo(null);

        clusterClient.reloadPartitions();
        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        RedisFuture<String> setA = connection.set(KEY_A, "myValue1");
        setA.get();

        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        RedisFuture<String> setB = connection.set(KEY_B, "myValue2");
        assertThat(setB.get()).isEqualTo("OK");

        RedisFuture<String> setD = connection.set("d", "myValue2");
        assertThat(setD.get()).isEqualTo("OK");

        connection.close();
    }

    @Test
    public void testReset() throws Exception {

        clusterClient.reloadPartitions();
        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        RedisFuture<String> setA = connection.set(KEY_A, "myValue1");
        setA.get();

        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        RedisFuture<String> setB = connection.set(KEY_B, "myValue2");
        assertThat(setB.get()).isEqualTo("OK");

        RedisFuture<String> setD = connection.set("d", "myValue2");
        assertThat(setD.get()).isEqualTo("OK");

        RedisChannelHandler<String, String> rch = (RedisChannelHandler<String, String>) connection;
        rch.reset();

        setA = connection.set(KEY_A, "myValue1");
        setA.get();

        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        setB = connection.set(KEY_B, "myValue2");
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
 else {
                partition.setSlots(new ArrayList<Integer>());
            }
        }
        partitions.updateCache();
        connection.setPartitions(partitions);

        // appropriate cluster node
        RedisFuture<String> setB = connection.set(KEY_B, "myValue1");

        assertThat(setB instanceof ClusterCommand).isTrue();

        ClusterCommand clusterCommandB = (ClusterCommand) setB;
        setB.get();
        assertThat(setB.getError()).isNull();
        assertThat(clusterCommandB.getExecutions()).isEqualTo(1);
        assertThat(setB.get()).isEqualTo("OK");
        assertThat(clusterCommandB.isMoved()).isFalse();

        // gets redirection to node 3
        RedisFuture<String> setA = connection.set(KEY_A, "myValue1");

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
    public void testClusterLatencyMetrics() throws Exception {

        CommandLatencyCollector commandLatencyCollector = clusterClient.getResources().commandLatencyCollector();
        commandLatencyCollector.retrieveMetrics();
        testClusterRedirection();

        Map<CommandLatencyId, CommandMetrics> metrics = commandLatencyCollector.retrieveMetrics();
        CommandLatencyId node1 = findId(metrics, port1);
        CommandLatencyId node3 = findId(metrics, port3);

        CommandMetrics node1Metrics = metrics.get(node1);
        assertThat(node1Metrics.getCount()).isEqualTo(2); // the direct and the redirected one

        CommandMetrics node3Metrics = metrics.get(node3); // the redirected one
        assertThat(node3Metrics.getCount()).isEqualTo(1);
    }

    protected CommandLatencyId findId(Map<CommandLatencyId, CommandMetrics> metrics, final int port) {
        Optional<CommandLatencyId> optional = Iterables.tryFind(metrics.keySet(), new Predicate<CommandLatencyId>() {
            @Override
            public boolean apply(CommandLatencyId input) {
                return input.remoteAddress().toString().contains(":" + port);
            }
        });

        return optional.orNull();
    }

    @Test
    public void testClusterConnectionStability() throws Exception {

        RedisAsyncConnectionImpl<String, String> connection = (RedisAsyncConnectionImpl<String, String>) clusterClient
                .connectClusterAsync();

        connection.set(KEY_A, KEY_B);

        ClusterDistributionChannelWriter<String, String> writer = (ClusterDistributionChannelWriter<String, String>) connection
                .getChannelWriter();

        final RedisAsyncConnectionImpl<Object, Object> backendConnection = writer.getClusterConnectionProvider().getConnection(
                ClusterConnectionProvider.Intent.WRITE, 3300);

        backendConnection.set(KEY_A, KEY_B);
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

        connection.set(KEY_A, KEY_B);

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
            futures.add(connection.set(KEY_A + i, "myValue1" + i));
            futures.add(connection.set(KEY_B + i, "myValue2" + i));
            futures.add(connection.set("d" + i, "myValue3" + i));
        }

        for (RedisFuture<?> future : futures) {
            future.get();
        }

        for (int i = 0; i < 100; i++) {
            RedisFuture<String> setA = connection.get(KEY_A + i);
            RedisFuture<String> setB = connection.get(KEY_B + i);
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
            connection.set(KEY_A + i, "myValue1" + i);
            connection.set(KEY_B + i, "myValue2" + i);
            connection.set("d" + i, "myValue3" + i);
        }

        for (int i = 0; i < 100; i++) {

            assertThat(connection.get(KEY_A + i)).isEqualTo("myValue1" + i);
            assertThat(connection.get(KEY_B + i)).isEqualTo("myValue2" + i);
            assertThat(connection.get("d" + i)).isEqualTo("myValue3" + i);
        }

        connection.close();
    }

    @Test
    public void readOnlyReadWrite() throws Exception {

        setNode4SlaveOfNode1();

        redis1.set(KEY_B, value);

        String resultB = redis1.get(KEY_B).get();
        assertThat(resultB).isEqualTo(value);
        Thread.sleep(500); // give some time to replicate

        // assume cluster node 4 is a slave for the master
        final RedisConnection<String, String> connect4 = client.connect(RedisURI.Builder.redis(host, port4).build());

        try {
            connect4.get(KEY_B);
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("MOVED");
        }

        String readOnly = connect4.readOnly();
        assertThat(readOnly).isEqualTo("OK");

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return connect4.get(KEY_B) != null;
            }
        }, timeout(seconds(5)));

        String resultBViewedBySlave = connect4.get(KEY_B);
        assertThat(resultBViewedBySlave).isEqualTo(value);
        connect4.quit();

        resultBViewedBySlave = connect4.get(KEY_B);
        assertThat(resultBViewedBySlave).isEqualTo(value);

        connect4.readWrite();
        try {
            connect4.get(KEY_B);
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
            assertThat(e).isInstanceOf(RedisException.class);
        }
    }

    @Test
    public void getKeysInSlot() throws Exception {

        RedisAdvancedClusterConnection<String, String> connection = clusterClient.connectCluster();
        connection.set(KEY_A, value);
        connection.set(KEY_B, value);

        List<String> keysA = connection.clusterGetKeysInSlot(SLOT_A, 10);
        assertThat(keysA).isEqualTo(ImmutableList.of(KEY_A));

        List<String> keysB = connection.clusterGetKeysInSlot(SLOT_B, 10);
        assertThat(keysB).isEqualTo(ImmutableList.of(KEY_B));

        connection.close();
    }

    @Test
    public void countKeysInSlot() throws Exception {
        RedisAdvancedClusterConnection<String, String> connection = clusterClient.connectCluster();
        connection.set(KEY_A, value);
        connection.set(KEY_B, value);

        Long result = connection.clusterCountKeysInSlot(SLOT_A);
        assertThat(result).isEqualTo(1L);

        result = connection.clusterCountKeysInSlot(SLOT_B);
        assertThat(result).isEqualTo(1L);

        int slotZZZ = SlotHash.getSlot("ZZZ".getBytes());
        result = connection.clusterCountKeysInSlot(slotZZZ);

        assertThat(result).isEqualTo(0L);

        connection.close();
    }

    @Test
    public void testClusterCountFailureReports() throws Exception {
        RedisClusterNode ownPartition = getOwnPartition(redissync1);
        assertThat(redissync1.clusterCountFailureReports(ownPartition.getNodeId())).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testClusterKeyslot() throws Exception {
        assertThat(redissync1.clusterKeyslot(KEY_A)).isEqualTo(SLOT_A);
        assertThat(SlotHash.getSlot(KEY_A)).isEqualTo(SLOT_A);
    }

    @Test
    public void testClusterSaveconfig() throws Exception {
        assertThat(redissync1.clusterSaveconfig()).isEqualTo("OK");
    }

    @Test
    public void testClusterSetConfigEpoch() throws Exception {
        try {
            redissync1.clusterSetConfigEpoch(1L);
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("ERR The user can assign a config epoch only");
        }
    }

    @Test
    public void testReadFrom() throws Exception {

        RedisAdvancedClusterConnection<String, String> connection = clusterClient.connectCluster();
        assertThat(connection.getReadFrom()).isEqualTo(ReadFrom.MASTER);

        connection.setReadFrom(ReadFrom.NEAREST);
        assertThat(connection.getReadFrom()).isEqualTo(ReadFrom.NEAREST);
        connection.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadFromNull() throws Exception {
        RedisAdvancedClusterConnection<String, String> connection = clusterClient.connectCluster();

        connection.setReadFrom(null);

        connection.close();
    }
}
