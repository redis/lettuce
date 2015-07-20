package com.lambdaworks.redis.cluster;

import static com.google.code.tempusfugit.temporal.Duration.*;
import static com.google.code.tempusfugit.temporal.Timeout.*;
import static com.lambdaworks.redis.cluster.ClusterTestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.lambdaworks.redis.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.ThreadSleep;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.lambdaworks.redis.api.StatefulRedisConnection;
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

    protected RedisClusterConnection<String, String> redissync1;
    protected RedisClusterConnection<String, String> redissync2;
    protected RedisClusterConnection<String, String> redissync3;
    protected RedisClusterConnection<String, String> redissync4;

    protected RedisAdvancedClusterConnection<String, String> syncConnection;

    @BeforeClass
    public static void setupClient() throws Exception {
        setupClusterClient();
        client = new RedisClient(host, port1);
        clusterClient = new RedisClusterClient(ImmutableList.of(RedisURI.Builder.redis(host, port1).build()));

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
        syncConnection = clusterClient.connectCluster();
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

        SlotHash.getSlot("b".getBytes()); // 3300 -> Node 1 and Slave (Node 3)
        SlotHash.getSlot("a".getBytes()); // 15495 -> Node 2

        RedisFuture<String> result = redis1.async().set("b", "value");
        assertThat(result.getError()).isEqualTo(null);
        assertThat(redissync1.set("b", "value")).isEqualTo("OK");

        RedisFuture<String> resultMoved = redis1.async().set("a", "value");
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

        connection.close();

    }

    @Test
    public void testReset() throws Exception {

        clusterClient.reloadPartitions();
        RedisAdvancedClusterAsyncCommandsImpl<String, String> connection = (RedisAdvancedClusterAsyncCommandsImpl) clusterClient
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

        assertThat(setB).isInstanceOf(AsyncCommand.class);

        setB.get();
        assertThat(setB.getError()).isNull();
        assertThat(setB.get()).isEqualTo("OK");

        // gets redirection to node 3
        RedisFuture<String> setA = connection.set("a", "myValue1");

        assertThat(setA instanceof AsyncCommand).isTrue();

        setA.get();
        assertThat(setA.getError()).isNull();
        assertThat(setA.get()).isEqualTo("OK");

        connection.close();
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
            FastShutdown.shutdown(clusterClient);

        }
    }

    @Test(expected = RedisException.class)
    public void clusterNeedsAuthButNotSupplied() throws Exception {

        RedisClusterClient clusterClient = new RedisClusterClient(RedisURI.Builder.redis(TestSettings.host(), port7).build());

        try (RedisAdvancedClusterConnection<String, String> connection = clusterClient.connectCluster()) {

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

        RedisClusterConnection<String, String> connection = syncConnection.getConnection(TestSettings.hostAddr(), port2);

        String result = connection.clusterMyId();
        assertThat(result).isEqualTo(redis1Node.getNodeId());

    }

    @Test
    public void operateOnNodeConnection() throws Exception {

        syncConnection.set("a", "b");
        syncConnection.set("b", "c");

        StatefulRedisConnection<String, String> statefulRedisConnection = syncConnection.getStatefulConnection().getConnection(
                TestSettings.hostAddr(), port2);

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

    @Test(expected = RedisException.class)
    public void getButNoPartitionForSlothash() throws Exception {

        for (RedisClusterNode redisClusterNode : clusterClient.getPartitions()) {
            redisClusterNode.setSlots(new ArrayList<>());
        }
        RedisChannelHandler rch = (RedisChannelHandler) syncConnection.getStatefulConnection();
        ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) rch.getChannelWriter();
        writer.setPartitions(clusterClient.getPartitions());
        clusterClient.getPartitions().reload(clusterClient.getPartitions().getPartitions());

        syncConnection.get(key);
    }

    @Test
    public void readOnlyOnCluster() throws Exception {

        syncConnection.readOnly();
        // commands are dispatched to a different connection, therefore it works for us.
        syncConnection.set("b", "b");

        syncConnection.getStatefulConnection().async().quit().get();

        assertThat(ReflectionTestUtils.getField(syncConnection.getStatefulConnection(), "readOnly")).isEqualTo(Boolean.TRUE);

        syncConnection.readWrite();

        assertThat(ReflectionTestUtils.getField(syncConnection.getStatefulConnection(), "readOnly")).isEqualTo(Boolean.FALSE);
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

        redissync1.set("b", value);
        List<String> keys = redissync1.clusterGetKeysInSlot(SlotHash.getSlot("b".getBytes()), 10);
        assertThat(keys).isEqualTo(ImmutableList.of("b"));
    }

}
