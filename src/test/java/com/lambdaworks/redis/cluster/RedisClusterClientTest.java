package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.getOwnPartition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotRange;
import com.lambdaworks.redis.cluster.models.slots.ClusterSlotsParser;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RedisClusterClientTest extends AbstractClusterTest {

    public static final int port7 = 7385;

    protected static RedisClient client;

    protected RedisClusterAsyncConnection<String, String> redis1;

    protected RedisClusterConnection<String, String> redissync1;
    protected RedisClusterConnection<String, String> redissync2;
    protected RedisClusterConnection<String, String> redissync3;
    protected RedisClusterConnection<String, String> redissync4;

    protected RedisAdvancedClusterConnection<String, String> syncConnection;

    protected String key = "key";
    protected String value = "value";

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, port1, port2, port3, port4);

    @BeforeClass
    public static void setupClient() throws Exception {
        setupClusterClient();
        client = new RedisClient(host, port1);
        clusterClient = new RedisClusterClient(ImmutableList.of(RedisURI.Builder.redis(host, port1).build()));

    }


    @AfterClass
    public static void shutdownClient() {
        shutdownClusterClient();
        client.shutdown(0, 0, TimeUnit.MILLISECONDS);
        clusterClient.shutdown();
    }

    @Before
    public void before() throws Exception {

        redis1 = client.connectAsync(RedisURI.Builder.redis(host, port1).build());

        redissync1 = client.connect(RedisURI.Builder.redis(host, port1).build());
        redissync2 = client.connect(RedisURI.Builder.redis(host, port2).build());
        redissync3 = client.connect(RedisURI.Builder.redis(host, port3).build());
        redissync4 = client.connect(RedisURI.Builder.redis(host, port4).build());

        syncConnection = clusterClient.connectCluster();

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
    public void statefulConnectionFromSync() throws Exception {
        RedisAdvancedClusterConnection<String, String> sync = clusterClient.connectCluster();
        assertThat(sync.getStatefulConnection().sync()).isSameAs(sync);
    }

    @Test
    public void statefulConnectionFromAsync() throws Exception {
        RedisAsyncConnection<String, String> async = client.connectAsync();
        assertThat(async.getStatefulConnection().async()).isSameAs(async);
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
    public void testClusteredOperations() throws Exception {

        SlotHash.getSlot("b".getBytes()); // 3300 -> Node 1 and Slave (Node 4)
        SlotHash.getSlot("a".getBytes()); // 15495 -> Node 3

        RedisFuture<String> result = redis1.set("b", "value");
        assertThat(result.getError()).isEqualTo(null);
        assertThat(redissync3.set("a", "value")).isEqualTo("OK");

        RedisFuture<String> resultMoved = redis1.set("a", "value");
        try {
            resultMoved.get();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("MOVED 15495");
        }

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
        RedisAdvancedClusterAsyncConnectionImpl<String, String> connection = (RedisAdvancedClusterAsyncConnectionImpl) clusterClient
                .connectClusterAsync();

        RedisFuture<String> setA = connection.set("a", "myValue1");
        setA.get();

        connection.reset();

        setA = connection.set("a", "myValue1");

        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

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
    public void testClusterCommandRedirection() throws Exception {

        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        // Command on node within the default connection
        assertThat(connection.set("b", "myValue1").get()).isEqualTo("OK");

        // gets redirection to node 3
        assertThat(connection.set("a", "myValue1").get()).isEqualTo("OK");
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

        assertThat(setB).isInstanceOf(ClusterCommand.class);

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
    public void readOnly() throws Exception {

        // cluster node 4 is a slave for key "b"
        String key = "b";
        prepareReadonlyTest(key);

        // assume cluster node 4 is a slave for the master
        RedisConnection<String, String> connect4 = client.connect(RedisURI.Builder.redis(host, port4).build());

        assertThat(connect4.readOnly()).isEqualTo("OK");
        waitUntilValueIsVisible(key, connect4);

        String resultBViewedBySlave = connect4.get("b");
        assertThat(resultBViewedBySlave).isEqualTo(value);
        connect4.quit();

        resultBViewedBySlave = connect4.get("b");
        assertThat(resultBViewedBySlave).isEqualTo(value);

    }

    @Test
    public void readOnlyWithReconnect() throws Exception {

        // cluster node 4 is a slave for key "b"
        String key = "b";
        prepareReadonlyTest(key);

        // assume cluster node 4 is a slave for the master
        RedisConnection<String, String> connect4 = client.connect(RedisURI.Builder.redis(host, port4).build());

        assertThat(connect4.readOnly()).isEqualTo("OK");
        connect4.quit();
        waitUntilValueIsVisible(key, connect4);

        String resultViewedBySlave = connect4.get("b");
        assertThat(resultViewedBySlave).isEqualTo(value);

    }

    protected void waitUntilValueIsVisible(String key, RedisConnection<String, String> connection) throws InterruptedException,
            TimeoutException {
        WaitFor.waitOrTimeout(() -> connection.get(key) != null, timeout(seconds(5)));
    }

    protected void prepareReadonlyTest(String key) throws InterruptedException, TimeoutException,
            java.util.concurrent.ExecutionException {
        setNode4SlaveOfNode1();

        redis1.set(key, value);

        String resultB = redis1.get(key).get();
        assertThat(resultB).isEqualTo(value);
        Thread.sleep(500); // give some time to replicate
    }

    @Test
    public void readOnlyReadWrite() throws Exception {

        // cluster node 4 is a slave for key "b"
        String key = "b";
        prepareReadonlyTest(key);

        // assume cluster node 4 is a slave for the master
        final RedisConnection<String, String> connect4 = client.connect(RedisURI.Builder.redis(host, port4).build());

        try {
            connect4.get("b");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("MOVED");
        }

        assertThat(connect4.readOnly()).isEqualTo("OK");
        waitUntilValueIsVisible(key, connect4);

        connect4.readWrite();
        try {
            connect4.get("b");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("MOVED");
        }
    }

    @Test
    public void readOnlyOnCluster() throws Exception {

        syncConnection.readOnly();
        // commands are dispatched to a different connection, therefore it works for us.
        syncConnection.set("b", "b");

    }

    @Test(expected = RedisException.class)
    public void closeConnection() throws Exception {

        try (RedisAdvancedClusterConnection<String, String> connection = clusterClient.connectCluster()) {

            List<String> time = connection.time();
            assertThat(time).hasSize(2);

            connection.close();

            connection.time();
        }
    }

    @Test
    public void clusterAuth() throws Exception {

        RedisClusterClient clusterClient = new RedisClusterClient(RedisURI.Builder.redis(TestSettings.host(), port7)
                .withPassword("foobared").build());

        try (RedisAdvancedClusterConnection<String, String> connection = clusterClient.connectCluster()) {

            List<String> time = connection.time();
            assertThat(time).hasSize(2);

            connection.getStatefulConnection().async().quit().get();

            time = connection.time();
            assertThat(time).hasSize(2);

            char[] password = (char[]) ReflectionTestUtils.getField(connection.getStatefulConnection(), "password");
            assertThat(new String(password)).isEqualTo("foobared");
        } finally {
            clusterClient.shutdown();

        }
    }

    @Test(expected = RedisException.class)
    public void clusterNeedsAuthButNotSupplied() throws Exception {

        RedisClusterClient clusterClient = new RedisClusterClient(RedisURI.Builder.redis(TestSettings.host(), port7).build());

        try (RedisAdvancedClusterConnection<String, String> connection = clusterClient.connectCluster()) {

            List<String> time = connection.time();
            assertThat(time).hasSize(2);
        } finally {
            clusterClient.shutdown();
        }
    }

    @Test
    public void noClusterNodeAvailable() throws Exception {

        RedisClusterClient clusterClient = new RedisClusterClient(RedisURI.Builder.redis(host, 40400).build());
        try {
            clusterClient.connectCluster();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasCauseInstanceOf(RedisException.class).hasRootCauseInstanceOf(ConnectException.class);
        }
    }

    @Test
    public void getClusterNodeConnection() throws Exception {

        RedisClusterNode redis1Node = getOwnPartition(redissync2);

        RedisClusterConnection<String, String> connection = syncConnection.getConnection(TestSettings.host(), port2);

        String result = connection.clusterMyId();
        assertThat(result).isEqualTo(redis1Node.getNodeId());

    }

    @Test
    public void operateOnNodeConnection() throws Exception {

        RedisClusterNode redis1Node = getOwnPartition(redissync2);
        syncConnection.set("a", "b");
        syncConnection.set("b", "c");

        StatefulRedisConnection<String, String> statefulRedisConnection = syncConnection.getStatefulConnection().getConnection(
                TestSettings.host(), port3);

        RedisClusterConnection<String, String> connection = statefulRedisConnection.sync();

        assertThat(connection.get("a")).isEqualTo("b");
        try {
            connection.get("b");
            fail("missing RedisCommandExecutionException: MOVED");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("MOVED");
        }
    }

    @Test
    public void testStatefulConnection() throws Exception {
        RedisAdvancedClusterAsyncConnection<String, String> async = syncConnection.getStatefulConnection().async();

        assertThat(async.ping().get()).isEqualTo("PONG");
    }

}
