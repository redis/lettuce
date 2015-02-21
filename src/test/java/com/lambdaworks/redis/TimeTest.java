package com.lambdaworks.redis;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TimeTest {
    RedisClient client = new RedisClient();

    @Before
    public void setUp() throws Exception {
        client.setDefaultTimeout(15, TimeUnit.SECONDS);
    }

    @Test
    public void testTime() throws Exception {
        Assert.assertEquals(15000, client.makeTimeout());

    }
}
