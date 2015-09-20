package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.ClusterTestUtil.getOwnPartition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.protocol.AsyncCommand;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("unchecked")
public class RedisClusterClientTest extends AbstractClusterTest {

    protected static RedisClient client;

    protected StatefulRedisConnection<String, String> redis1;
    protected StatefulRedisConnection<String, String> redis2;
    protected StatefulRedisConnection<String, String> redis3;
    protected StatefulRedisConnection<String, String> redis4;

    protected RedisClusterCommands<String, String> redissync1;
    protected RedisClusterCommands<String, String> redissync2;
    protected RedisClusterCommands<String, String> redissync3;
    protected RedisClusterCommands<String, String> redissync4;

    protected RedisAdvancedClusterCommands<String, String> sync;

    @BeforeClass
    public static void setupClient() throws Exception {
        setupClusterClient();
        client = RedisClient.create(RedisURI.Builder.redis(host, port1).build());
        clusterClient = RedisClusterClient.create(ImmutableList.of(RedisURI.Builder.redis(host, port1).build()));
    }

    @AfterClass
    public static void shutdownClient() {
        shutdownClusterClient();
        FastShutdown.shutdown(client);
        FastShutdown.shutdown(clusterClient);
    }

    @Before
    public void before() throws Exception {

        clusterRule.getClusterClient().reloadPartitions();

        redis1 = client.connect(RedisURI.Builder.redis(host, port1).build());
        redis2 = client.connect(RedisURI.Builder.redis(host, port2).build());
        redis3 = client.connect(RedisURI.Builder.redis(host, port3).build());
        redis4 = client.connect(RedisURI.Builder.redis(host, port4).build());

        redissync1 = redis1.sync();
        redissync2 = redis2.sync();
        redissync3 = redis3.sync();
        redissync4 = redis4.sync();

        clusterClient.reloadPartitions();
        sync = clusterClient.connectCluster();
    }

    @After
    public void after() throws Exception {
        sync.close();
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
    public void reloadPartitions() throws Exception {
        assertThat(clusterClient.getPartitions()).hasSize(4);

        assertThat(clusterClient.getPartitions().getPartition(0).getUri());
        assertThat(clusterClient.getPartitions().getPartition(1).getUri());
        assertThat(clusterClient.getPartitions().getPartition(2).getUri());
        assertThat(clusterClient.getPartitions().getPartition(3).getUri());

        clusterClient.reloadPartitions();

        assertThat(clusterClient.getPartitions().getPartition(0).getUri());
        assertThat(clusterClient.getPartitions().getPartition(1).getUri());
        assertThat(clusterClient.getPartitions().getPartition(2).getUri());
        assertThat(clusterClient.getPartitions().getPartition(3).getUri());
    }

    @Test
    public void testClusteredOperations() throws Exception {

        SlotHash.getSlot(KEY_B.getBytes()); // 3300 -> Node 1 and Slave (Node 3)
        SlotHash.getSlot(KEY_A.getBytes()); // 15495 -> Node 2

        RedisFuture<String> result = redis1.async().set(KEY_B, value);
        assertThat(result.getError()).isEqualTo(null);
        assertThat(redissync1.set(KEY_B, "value")).isEqualTo("OK");

        RedisFuture<String> resultMoved = redis1.async().set(KEY_A, value);
        try {
            resultMoved.get();
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("MOVED 15495");
        }

        clusterClient.reloadPartitions();
        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        RedisFuture<String> setA = connection.set(KEY_A, value);
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
        RedisAdvancedClusterAsyncCommandsImpl<String, String> connection = (RedisAdvancedClusterAsyncCommandsImpl) clusterClient
                .connectClusterAsync();

        RedisFuture<String> setA = connection.set(KEY_A, value);
        setA.get();

        connection.reset();

        setA = connection.set(KEY_A, "myValue1");

        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        connection.close();

    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void testClusterCommandRedirection() throws Exception {

        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterClient.connect().async();

        // Command on node within the default connection
        assertThat(connection.set(KEY_B, value).get()).isEqualTo("OK");

        // gets redirection to node 3
        assertThat(connection.set(KEY_A, value).get()).isEqualTo("OK");
        connection.close();
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void testClusterRedirection() throws Exception {

        RedisAdvancedClusterAsyncCommands<String, String> connection = clusterClient.connect().async();
        Partitions partitions = clusterClient.getPartitions();

        for (RedisClusterNode partition : partitions) {
            partition.setSlots(Lists.<Integer> newArrayList());
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                partition.getSlots().addAll(Ints.asList(createSlots(0, 16384)));
            }
        }

        // appropriate cluster node
        RedisFuture<String> setB = connection.set(KEY_B, value);

        assertThat(setB).isInstanceOf(AsyncCommand.class);

        setB.get();
        assertThat(setB.getError()).isNull();
        assertThat(setB.get()).isEqualTo("OK");

        // gets redirection to node 3
        RedisFuture<String> setA = connection.set(KEY_A, value);

        assertThat(setA instanceof AsyncCommand).isTrue();

        setA.get();
        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        connection.close();
    }

    @Test(expected = RedisException.class)
    public void closeConnection() throws Exception {

        try (RedisAdvancedClusterCommands<String, String> connection = clusterClient.connect().sync()) {

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
            FastShutdown.shutdown(clusterClient);

        }
    }

    @Test(expected = RedisException.class)
    public void clusterNeedsAuthButNotSupplied() throws Exception {

        RedisClusterClient clusterClient = new RedisClusterClient(RedisURI.Builder.redis(TestSettings.host(), port7).build());

        try (RedisClusterCommands<String, String> connection = clusterClient.connectCluster()) {

            List<String> time = connection.time();
            assertThat(time).hasSize(2);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    public void noClusterNodeAvailable() throws Exception {

        RedisClusterClient clusterClient = new RedisClusterClient(RedisURI.Builder.redis(host, 40400).build());
        try {
            clusterClient.connectCluster();
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).isInstanceOf(RedisException.class);
        }
    }

    @Test
    public void getClusterNodeConnection() throws Exception {

        RedisClusterNode redis1Node = getOwnPartition(redissync2);

        RedisClusterCommands<String, String> connection = sync.getConnection(TestSettings.hostAddr(), port2);

        String result = connection.clusterMyId();
        assertThat(result).isEqualTo(redis1Node.getNodeId());

    }

    @Test
    public void operateOnNodeConnection() throws Exception {

        sync.set(KEY_A, value);
        sync.set(KEY_B, "d");

        StatefulRedisConnection<String, String> statefulRedisConnection = sync.getStatefulConnection().getConnection(
                TestSettings.hostAddr(), port2);

        RedisClusterCommands<String, String> connection = statefulRedisConnection.sync();

        assertThat(connection.get(KEY_A)).isEqualTo(value);
        try {
            connection.get(KEY_B);
            fail("missing RedisCommandExecutionException: MOVED");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("MOVED");
        }
    }

    @Test
    public void testStatefulConnection() throws Exception {
        RedisAdvancedClusterAsyncCommands<String, String> async = sync.getStatefulConnection().async();

        assertThat(async.ping().get()).isEqualTo("PONG");
    }

    @Test(expected = RedisException.class)
    public void getButNoPartitionForSlothash() throws Exception {

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            redisClusterNode.setSlots(new ArrayList<>());
        }
        RedisChannelHandler rch = (RedisChannelHandler) sync.getStatefulConnection();
        ClusterDistributionChannelWriter<String, String> writer = (ClusterDistributionChannelWriter<String, String>) rch
                .getChannelWriter();
        writer.setPartitions(clusterClient.getPartitions());
        clusterClient.getPartitions().reload(clusterClient.getPartitions().getPartitions());

        sync.get(key);
    }

    @Test
    public void readOnlyOnCluster() throws Exception {

        sync.readOnly();
        // commands are dispatched to a different connection, therefore it works for us.
        sync.set(KEY_B, value);

        sync.getStatefulConnection().async().quit().get();

        assertThat(ReflectionTestUtils.getField(sync.getStatefulConnection(), "readOnly")).isEqualTo(Boolean.TRUE);

        sync.readWrite();

        assertThat(ReflectionTestUtils.getField(sync.getStatefulConnection(), "readOnly")).isEqualTo(Boolean.FALSE);
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

        sync.set(KEY_A, value);
        sync.set(KEY_B, value);

        List<String> keysA = sync.clusterGetKeysInSlot(SLOT_A, 10);
        assertThat(keysA).isEqualTo(ImmutableList.of(KEY_A));

        List<String> keysB = sync.clusterGetKeysInSlot(SLOT_B, 10);
        assertThat(keysB).isEqualTo(ImmutableList.of(KEY_B));

    }

    @Test
    public void countKeysInSlot() throws Exception {

        sync.set(KEY_A, value);
        sync.set(KEY_B, value);

        Long result = sync.clusterCountKeysInSlot(SLOT_A);
        assertThat(result).isEqualTo(1L);

        result = sync.clusterCountKeysInSlot(SLOT_B);
        assertThat(result).isEqualTo(1L);

        int slotZZZ = SlotHash.getSlot("ZZZ".getBytes());
        result = sync.clusterCountKeysInSlot(slotZZZ);
        assertThat(result).isEqualTo(0L);

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
        StatefulRedisClusterConnection<String, String> statefulConnection = sync.getStatefulConnection();

        assertThat(statefulConnection.getReadFrom()).isEqualTo(ReadFrom.MASTER);

        statefulConnection.setReadFrom(ReadFrom.NEAREST);
        assertThat(statefulConnection.getReadFrom()).isEqualTo(ReadFrom.NEAREST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadFromNull() throws Exception {
        sync.getStatefulConnection().setReadFrom(null);
    }
}
