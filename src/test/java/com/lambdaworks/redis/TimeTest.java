package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TimeTest {
    RedisClient client = RedisClient.create();

    @Before
    public void setUp() throws Exception {
        client.setDefaultTimeout(15, TimeUnit.SECONDS);
    }

    @After
    public void after() throws Exception {
        FastShutdown.shutdown(client);
    }

    @Test
    public void testTime() throws Exception {
        Assert.assertEquals(15000, client.makeTimeout());
    }
}
