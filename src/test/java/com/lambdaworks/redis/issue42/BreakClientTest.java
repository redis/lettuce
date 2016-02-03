package com.lambdaworks.redis.issue42;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.category.SlowTests;
import com.lambdaworks.redis.DefaultRedisClient;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.sync.RedisCommands;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SlowTests
@Ignore("Run me manually")
public class BreakClientTest extends BreakClientBase {

    protected static RedisClient client = DefaultRedisClient.get();

    protected RedisCommands<String, String> redis;

    @Before
    public void setUp() throws Exception {
        client.setDefaultTimeout(TIMEOUT, TimeUnit.SECONDS);
        redis = client.connect(this.slowCodec).sync();
        redis.flushall();
        redis.flushdb();
    }

    @After
    public void tearDown() throws Exception {
        redis.close();
    }

    @Test
    public void testStandAlone() throws Exception {
        testSingle(redis);
    }

    @Test
    public void testLooping() throws Exception {
        testLoop(redis);
    }

}
