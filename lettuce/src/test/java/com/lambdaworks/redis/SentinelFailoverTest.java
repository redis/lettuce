// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SentinelFailoverTest extends AbstractCommandTest {

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
    public void connectToRedisUsingSentinel() throws Exception {
        RedisConnection<String, String> connect = sentinelClient.connect();
        connect.ping();

        RedisFuture<String> future = this.sentinel.failover("mymaster");

        future.get();
        assertThat(future.getError()).isNull();
        assertThat(future.get()).isEqualTo("OK");
    }

    protected static RedisClient getRedisSentinelClient() {
        return new RedisClient(RedisURI.Builder.sentinel("localhost", 26380, "mymaster").build());
    }
}
