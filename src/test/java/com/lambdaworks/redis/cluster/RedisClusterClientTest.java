package com.lambdaworks.redis.cluster;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;

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

    private static int slots1[];
    private static int slots2[];
    private static int slots3[];

    protected String key = "key";
    protected String value = "value";
    private boolean setup = false;

    @BeforeClass
    public static void setupClient() throws Exception {
        client1 = new RedisClient(host, port1);
        client2 = new RedisClient(host, port2);
        client3 = new RedisClient(host, port3);
        client4 = new RedisClient(host, port4);

        RedisClusterAsyncConnection<String, String> connection = (RedisClusterAsyncConnection) client1.connectAsync();
        String nodes = connection.clusterNodes().get();

        if (Splitter.on('\n').trimResults().splitToList(nodes).size() < 3) {

            connection.clusterMeet(host, port1);
            connection.clusterMeet(host, port2);
            connection.clusterMeet(host, port3);
            connection.clusterMeet(host, port4);
        }

        slots1 = createSlots(0, 8000);
        slots2 = createSlots(8000, 12000);
        slots3 = createSlots(12000, 16384);

        slots1[7000] = 12001;
        slots3[1] = 7000;

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
        redis1.flushall();

        redis2 = (RedisClusterAsyncConnection) client2.connectAsync();
        redis2.flushall();

        redis3 = (RedisClusterAsyncConnection) client3.connectAsync();
        redis3.flushall();

        redis4 = (RedisClusterAsyncConnection) client4.connectAsync();
        redis4.flushall();

        if (!setup) {
            cleanup();
            addSlots();

            Thread.sleep(500);
            setup = true;
        }
    }

    @After
    public void after() throws Exception {
        redis1.flushall();
        redis2.flushall();
        redis3.flushall();
        redis4.flushall();

        redis1.close();
        redis2.close();
        redis3.close();
        redis4.close();
    }

    protected void addSlots() throws InterruptedException, java.util.concurrent.ExecutionException {

        RedisFuture<String> f1 = redis1.clusterAddSlots(slots1);
        RedisFuture<String> f2 = redis2.clusterAddSlots(slots2);
        RedisFuture<String> f3 = redis3.clusterAddSlots(slots3);

        f1.get();
        f2.get();
        f3.get();
    }

    public void cleanup() throws Exception {

        int slots[] = createSlots(0, 16384);
        List<RedisFuture<?>> futures = Lists.newArrayList();

        for (int i = 0; i < 16384; i++) {
            futures.add(redis1.clusterDelSlots(i));
            futures.add(redis2.clusterDelSlots(i));
            futures.add(redis3.clusterDelSlots(i));
            futures.add(redis4.clusterDelSlots(i));
        }

        for (int i = 0; i < 16384; i++) {
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

        RedisFuture<List<String>> future = redis1.clusterInfo();

        List<String> list = future.get();

        Collections.sort(list);

        String status = list.get(0);

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

        String masterId = null;
        for (RedisClusterNode partition : partitions) {
            if (!partition.getSlots().isEmpty()) {
                masterId = partition.getNodeId();
                RedisFuture<String> result = redis4.clusterReplicate(masterId);
                break;
            }
        }

        RedisFuture<List<String>> future = redis1.clusterSlaves(masterId);

        List<String> result = future.get();

        assertEquals(1, result.size());
    }

    @Test
    public void testClusteredOperations() throws Exception {

        int slot1 = SlotHash.getSlot("b".getBytes()); // 3300 -> Node 1 and Slave (Node 4)
        int slot2 = SlotHash.getSlot("a".getBytes()); // 15495 -> Node 3

        RedisFuture<String> result = redis1.set("b", "value");
        assertEquals(null, result.getError());
        assertEquals("OK", redis3.set("a", "value").get());

        RedisFuture<String> resultMoved = redis1.set("a", "value");
        assertEquals(null, resultMoved.get());
        assertThat(resultMoved.getError(), containsString("MOVED 15495"));

        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        RedisFuture<String> setA = connection.set("a", "myValue1");
        setA.get();

        assertNull(setA.getError());
        assertEquals("OK", setA.get());

        RedisFuture<String> setB = connection.set("b", "myValue2");
        assertEquals("OK", setB.get());

        RedisFuture<String> setD = connection.set("d", "myValue2");
        assertEquals("OK", setB.get());

    }

    @Test(timeout = 10000)
    public void massiveClusteredAccess() throws Exception {

        RedisClusterAsyncConnection<String, String> connection = clusterClient.connectClusterAsync();

        for (int i = 0; i < 10000; i++) {
            RedisFuture<String> setA = connection.set("a" + i, "myValue1");
            RedisFuture<String> setB = connection.set("b" + i, "myValue2");
            RedisFuture<String> setD = connection.set("d" + i, "myValue2");

            assertEquals("OK", setB.get());
            assertEquals("OK", setB.get());
            assertEquals("OK", setA.get());
        }

        connection.close();
        System.out.println();
    }
}
