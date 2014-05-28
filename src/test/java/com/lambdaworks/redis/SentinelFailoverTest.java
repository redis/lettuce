// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SentinelFailoverTest extends AbstractCommandTest {

    private static RedisClient sentinelClient;
    private RedisSentinelAsyncConnection sentinel;

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
    public void connectToRedisUsingSentinel() throws Exception {
        RedisConnection<String, String> connect = sentinelClient.connect();
        connect.ping();

        String info = connect.info();
        assertThat(info, containsString("tcp_port:6381"));
        assertThat(info, containsString("role:master"));

        assertEquals("OK", connect.set(key, value));
        sentinel.failover("mymaster").get();
        Thread.sleep(1500);

        assertEquals("OK", connect.set(key, value));

        RedisConnection<String, String> connect2 = sentinelClient.connect();
        assertEquals("OK", connect2.set(key, value));

        String info2 = connect2.info();
        assertThat(info2, containsString("tcp_port:6382"));
        assertThat(info2, containsString("role:master"));


        String infoAfterFailover = connect.info();
        assertThat(infoAfterFailover, containsString("tcp_port:6381"));

        sentinel.failover("mymaster").get();

        connect.close();
        connect2.close();
    }

    protected static RedisClient getRedisSentinelClient() {
        return new RedisClient(RedisURI.Builder.sentinel("localhost", 26380, "mymaster").build());
    }
}
