package com.lambdaworks.redis.cluster;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisFuture;

public class RedisClusterClientTest {

    public static final String host = "127.0.0.1";
    public static final int port1 = 7379;
    public static final int port2 = 7380;
    public static final int port3 = 7381;

    protected static RedisClient client1;
    protected static RedisClient client2;
    protected static RedisClient client3;

    protected RedisAsyncConnection<String, String> redis1;
    protected RedisAsyncConnection<String, String> redis2;
    protected RedisAsyncConnection<String, String> redis3;

    protected RedisClusterAsyncConnection<String, String> cc1;
    protected RedisClusterAsyncConnection<String, String> cc2;
    protected RedisClusterAsyncConnection<String, String> cc3;

    private static int slots1[];
    private static int slots2[];
    private static int slots3[];

    protected String key = "key";
    protected String value = "value";

    @BeforeClass
    public static void setupClient() {
        client1 = new RedisClient(host, port1);
        client2 = new RedisClient(host, port2);
        client3 = new RedisClient(host, port3);

        RedisClusterAsyncConnection connection = (RedisClusterAsyncConnection) client1.connectAsync();

        connection.clusterMeet(host, port1);
        connection.clusterMeet(host, port2);
        connection.clusterMeet(host, port3);

        slots1 = createSlots(0, 8000);
        slots2 = createSlots(8000, 12000);
        slots3 = createSlots(12000, 16384);

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
    }

    @Before
    public void before() throws Exception {
        redis1 = client1.connectAsync();
        redis1.flushall();

        redis2 = client2.connectAsync();
        redis2.flushall();

        redis3 = client3.connectAsync();
        redis3.flushall();

        cc1 = (RedisClusterAsyncConnection) redis1;
        cc2 = (RedisClusterAsyncConnection) redis2;
        cc3 = (RedisClusterAsyncConnection) redis3;

        after();
        slots1[7000] = 12001;
        slots3[1] = 7000;

        RedisFuture<String> f1 = cc1.clusterAddSlots(slots1);
        RedisFuture<String> f2 = cc2.clusterAddSlots(slots2);
        RedisFuture<String> f3 = cc3.clusterAddSlots(slots3);

        f1.get();
        f2.get();
        f3.get();

    }

    public void after() throws Exception {

        int slots[] = createSlots(0, 16384);
        List<RedisFuture<?>> futures = Lists.newArrayList();

        for (int i = 0; i < 16384; i++) {
            futures.add(cc1.clusterDelSlots(i));
            futures.add(cc2.clusterDelSlots(i));
            futures.add(cc3.clusterDelSlots(i));
        }

        for (int i = 0; i < 16384; i++) {
            futures.add(cc1.clusterDelSlots(i));
            futures.add(cc2.clusterDelSlots(i));
            futures.add(cc3.clusterDelSlots(i));
        }

        for (RedisFuture<?> future : futures) {
            future.get();
        }

    }

    @Test
    public void testClusterInfo() throws Exception {

        RedisFuture<List<String>> future = cc1.clusterInfo();

        List<String> list = future.get();

        Collections.sort(list);

        System.out.println(list);

        String status = list.get(0);

        assertThat(status, containsString("cluster_known_nodes:3"));
        assertThat(status, containsString("cluster_slots_fail:0"));
        assertThat(status, containsString("cluster_state:"));
    }

    @Test
    public void testClusterNodes() throws Exception {

        RedisFuture<String> future = cc1.clusterNodes();

        String string = future.get();

        assertThat(string, containsString("connected"));
        assertThat(string, containsString("master"));
        assertThat(string, containsString("myself"));

    }
}
