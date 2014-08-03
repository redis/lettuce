// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SentinelCommandTest extends AbstractCommandTest {

    private static RedisClient sentinelClient;
    private RedisSentinelAsyncConnection<String, String> sentinel;

    @BeforeClass
    public static void setupClient() {
        sentinelClient = getRedisSentinelClient();
    }

    @AfterClass
    public static void shutdownClient() {
        sentinelClient.shutdown();
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinelAsync();
    }

    @After
    public void closeConnection() throws Exception {
        sentinel.close();
    }

    @Test
    public void getMasterAddr() throws Exception {

        Future<SocketAddress> result = sentinel.getMasterAddrByName("mymaster");

        InetSocketAddress socketAddress = (InetSocketAddress) result.get();

        assertThat(socketAddress.getHostName(), containsString("localhost"));
    }

    @Test
    public void getSlaveAddr() throws Exception {

        Future<SocketAddress> result = sentinel.getMasterAddrByName("myslave");

        InetSocketAddress socketAddress = (InetSocketAddress) result.get();

        assertThat(socketAddress.getPort()).isEqualTo(16379);

    }

    @Test
    public void masters() throws Exception {

        Future<List<Map<String, String>>> result = sentinel.masters();
        List<Map<String, String>> list = result.get();

        assertThat(list.size(), greaterThan(0));

        Map<String, String> map = list.get(0);
        assertThat(map.get("flags")).isNotNull();
        assertThat(map.get("config-epoch")).isNotNull();
        assertThat(map.get("port")).isNotNull();

    }

    @Test
    public void getSlaveDownstate() throws Exception {

        Future<Map<String, String>> result = sentinel.master("myslave");
        Map<String, String> map = result.get();
        assertThat(map.get("flags"), containsString("disconnected"));

    }

    @Test
    public void getMaster() throws Exception {

        Future<Map<String, String>> result = sentinel.master("mymaster");
        Map<String, String> map = result.get();
        assertThat(map.get("ip")).isEqualTo("127.0.0.1"); // !! IPv4/IPv6
        assertThat(map.get("role-reported")).isEqualTo("master");

    }

    @Test
    public void role() throws Exception {

        RedisClient redisClient = new RedisClient("localhost", 26381);
        RedisAsyncConnection<String, String> connection = redisClient.connectAsync();
        try {

            RedisFuture<List<Object>> role = connection.role();
            List<Object> objects = role.get();

            assertThat(objects.size(), is(2));

            assertThat(objects.get(0)).isEqualTo("sentinel");
            assertThat(objects.get(1).toString()).isEqualTo("[mymasterfailover]");

        } finally {
            connection.close();
            redisClient.shutdown();
        }
    }

    @Test
    public void getSlaves() throws Exception {

        Future<Map<String, String>> result = sentinel.slaves("mymaster");
        result.get();

    }

    @Test
    public void reset() throws Exception {

        Future<Long> result = sentinel.reset("myslave");
        Long val = result.get();
        assertThat(val.intValue()).isEqualTo(1);

    }

    @Test
    public void failover() throws Exception {

        RedisFuture<String> mymaster = sentinel.failover("mymaster");
        mymaster.get();

    }

    @Test
    public void monitor() throws Exception {

        Future<String> removeResult = sentinel.remove("mymaster2");
        removeResult.get();

        Future<String> result = sentinel.monitor("mymaster2", "127.0.0.1", 8989, 2);
        String val = result.get();
        assertThat(val).isEqualTo("OK");

    }

    @Test
    public void ping() throws Exception {

        Future<String> result = sentinel.ping();
        String val = result.get();
        assertThat(val).isEqualTo("PONG");
    }

    @Test
    public void set() throws Exception {

        Future<String> result = sentinel.set("mymaster", "down-after-milliseconds", "1000");
        String val = result.get();
        assertThat(val).isEqualTo("OK");
    }

    @Test
    public void connectToRedisUsingSentinel() throws Exception {
        RedisConnection<String, String> connect = sentinelClient.connect();
        connect.ping();
        connect.close();
    }

    protected static RedisClient getRedisSentinelClient() {
        return new RedisClient(RedisURI.Builder.sentinel("localhost", 1234, "mymaster").withSentinel("localhost").build());
    }

}
