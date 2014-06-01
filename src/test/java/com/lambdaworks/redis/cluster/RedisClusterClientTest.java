package com.lambdaworks.redis.cluster;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

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
import com.google.code.tempusfugit.temporal.Timeout;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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

        client1.shutdown();
        client2.shutdown();
        client3.shutdown();
        client4.shutdown();
    }

    @Before
    public void before() throws Exception {
        redis1 = (RedisClusterAsyncConnection) client1.connectAsync();
        redis2 = (RedisClusterAsyncConnection) client2.connectAsync();
        redis3 = (RedisClusterAsyncConnection) client3.connectAsync();
        redis4 = (RedisClusterAsyncConnection) client4.connectAsync();

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
                        return true;
                    }
                } catch (Exception e) {

                }

                return false;
            }
        }, Timeout.timeout(Duration.seconds(5)), new ThreadSleep(Duration.millis(500)));
    }

    @After
    public void after() throws Exception {
        redis1.close();
        redis2.close();
        redis3.close();
        redis4.close();
    }

    public void cleanup() throws Exception {

        int slots[] = createSlots(1, 16385);
        List<RedisFuture<?>> futures = Lists.newArrayList();

        for (int i = 0; i < slots.length; i++) {
            futures.add(redis1.clusterDelSlots(i));
            futures.add(redis2.clusterDelSlots(i));
            futures.add(redis3.clusterDelSlots(i));
            futures.add(redis4.clusterDelSlots(i));
        }

        for (int i = 0; i < slots.length; i++) {
            futures.add(redis1.clusterDelSlots(i));
            futures.add(redis2.clusterDelSlots(i));
            futures.add(redis3.clusterDelSlots(i));
            futures.add(redis4.clusterDelSlots(i));
        }

        for (RedisFuture<?> future : futures) {
            future.get();
        }
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
    public void clusterSlaves() throws Exception {
        Partitions partitions = ClusterPartitionParser.parse(redis1.clusterNodes().get());

        final RedisClusterNode node1 = Iterables.find(partitions, new Predicate<RedisClusterNode>() {
            @Override
            public boolean apply(RedisClusterNode input) {
                return input.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF);
            }
        });

        RedisFuture<String> replicate = redis4.clusterReplicate(node1.getNodeId());
        replicate.get();
        assertNull(replicate.getError());
        assertEquals("OK", replicate.get());

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
        }, Timeout.timeout(Duration.seconds(5)));

    }

    @Test
    public void testClusteredOperations() throws Exception {

        int slot1 = SlotHash.getSlot("b".getBytes()); // 3300 -> Node 1 and Slave (Node 4)
        int slot2 = SlotHash.getSlot("a".getBytes()); // 15495 -> Node 3

        RedisFuture<String> result = redis1.set("b", "value");
        assertEquals(null, result.getError());
        assertEquals("OK", redis3.set("a", "value").get());

        RedisFuture<String> resultMoved = redis1.set("a", "value");
        resultMoved.get();
        assertThat(resultMoved.getError(), containsString("MOVED 15495"));
        assertEquals(null, resultMoved.get());

        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        RedisFuture<String> setA = connection.set("a", "myValue1");
        setA.get();

        assertNull(setA.getError());
        assertEquals("OK", setA.get());

        RedisFuture<String> setB = connection.set("b", "myValue2");
        assertEquals("OK", setB.get());

        RedisFuture<String> setD = connection.set("d", "myValue2");
        assertEquals("OK", setD.get());

        connection.close();

    }

    @Test
    public void testClusterRedirection() throws Exception {

        int slot1 = SlotHash.getSlot("b".getBytes()); // 3300 -> Node 1 and Slave (Node 4)
        int slot2 = SlotHash.getSlot("a".getBytes()); // 15495 -> Node 3

        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();
        Partitions partitions = clusterClient.getPartitions();

        for (RedisClusterNode partition : partitions) {
            partition.getSlots().clear();
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                partition.getSlots().addAll(Ints.asList(createSlots(0, 16384)));
            }
        }

        // appropriate cluster node
        RedisFuture<String> setB = connection.set("b", "myValue1");

        assertTrue(setB instanceof ClusterCommand);

        ClusterCommand clusterCommandB = (ClusterCommand) setB;
        setB.get();
        assertNull(setB.getError());
        assertEquals(1, clusterCommandB.getExecutions());
        assertEquals("OK", setB.get());
        assertFalse(clusterCommandB.isMoved());

        // gets redirection to node 3
        RedisFuture<String> setA = connection.set("a", "myValue1");

        assertTrue(setA instanceof ClusterCommand);

        ClusterCommand clusterCommandA = (ClusterCommand) setA;
        setA.get();
        assertNull(setA.getError());
        assertEquals(2, clusterCommandA.getExecutions());
        assertEquals(5, clusterCommandA.getExecutionLimit());
        assertEquals("OK", setA.get());
        assertFalse(clusterCommandA.isMoved());

        connection.close();

    }

    @Test
    public void testClusterConnectionStability() throws Exception {

        RedisAsyncConnectionImpl<String, String> connection = (RedisAsyncConnectionImpl<String, String>) clusterClient
                .connectClusterAsync();

        connection.set("a", "b");

        ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) connection.getChannelWriter();

        RedisAsyncConnectionImpl<Object, Object> backendConnection = writer.getClusterConnectionProvider().getConnection(
                ClusterConnectionProvider.Intent.WRITE, 3300);

        backendConnection.set("a", "b");
        backendConnection.close();

        assertTrue(backendConnection.isClosed());
        assertFalse(backendConnection.isOpen());

        assertTrue(connection.isOpen());
        assertFalse(connection.isClosed());

        connection.set("a", "b");

        RedisAsyncConnectionImpl<Object, Object> backendConnection2 = writer.getClusterConnectionProvider().getConnection(
                ClusterConnectionProvider.Intent.WRITE, 3300);

        assertTrue(backendConnection2.isOpen());
        assertFalse(backendConnection2.isClosed());

        assertNotSame(backendConnection, backendConnection2);

        connection.close();

    }

    @Test(timeout = 10000)
    public void massiveClusteredAccess() throws Exception {

        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        for (int i = 0; i < 10000; i++) {
            connection.set("a" + i, "myValue1" + i).get();
            connection.set("b" + i, "myValue2" + i).get();
            connection.set("d" + i, "myValue3" + i).get();
        }

        for (int i = 0; i < 10000; i++) {
            RedisFuture<String> setA = connection.get("a" + i);
            RedisFuture<String> setB = connection.get("b" + i);
            RedisFuture<String> setD = connection.get("d" + i);

            setA.get();
            setB.get();
            setD.get();

            assertNull(setA.getError());
            assertNull(setB.getError());
            assertNull(setD.getError());

            assertEquals("myValue1" + i, setA.get());
            assertEquals("myValue2" + i, setB.get());
            assertEquals("myValue3" + i, setD.get());
        }

        connection.close();
    }
}
