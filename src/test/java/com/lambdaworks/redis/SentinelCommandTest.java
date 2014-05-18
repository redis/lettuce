// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.protocol.Command;

public class SentinelCommandTest extends AbstractCommandTest {

    private static RedisClient sentinelClient;
    private RedisSentinelAsyncConnectionImpl sentinel;

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

        assertEquals("localhost", socketAddress.getHostName());
    }

    @Test
    public void getSlaveAddr() throws Exception {

        Future<SocketAddress> result = sentinel.getMasterAddrByName("myslave");

        InetSocketAddress socketAddress = (InetSocketAddress) result.get();

        assertEquals(6381, socketAddress.getPort());

    }

    @Test
    public void getSlaveDownstate() throws Exception {

        Future<Map> result = sentinel.getMaster("myslave");
        Map map = result.get();
        assertThat((String) map.get("flags"), containsString("disconnected"));

    }

    @Test
    public void getMaster() throws Exception {

        Future<Map> result = sentinel.getMaster("mymaster");
        Map map = result.get();
        System.out.println(map);
        assertEquals("127.0.0.1", map.get("ip")); // !! IPv4/IPv6
        assertEquals("master", map.get("role-reported"));

    }

    @Test
    public void getSlaves() throws Exception {

        Future<Map> result = sentinel.getSlaves("mymaster");
        Map map = result.get();

    }

    @Test
    public void reset() throws Exception {

        Future<Long> result = sentinel.reset("myslave");
        Long val = result.get();
        assertEquals(1, val.intValue());

    }

    @Test
    public void failover() throws Exception {

        Command result = (Command) sentinel.failover("mymaster");
        result.get();

    }

    @Test
    public void monitor() throws Exception {

        Future<String> removeResult = sentinel.remove("mymaster2");
        removeResult.get();

        Future<String> result = sentinel.monitor("mymaster2", "127.0.0.1", 8989, 2);
        String val = result.get();
        assertEquals("OK", val);

    }

    @Test
    public void ping() throws Exception {

        Future<String> result = sentinel.ping();
        String val = result.get();
        assertEquals("PONG", val);
    }

    @Test
    public void set() throws Exception {

        Future<String> result = sentinel.set("mymaster", "down-after-milliseconds", "1000");
        String val = result.get();
        assertEquals("OK", val);
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
