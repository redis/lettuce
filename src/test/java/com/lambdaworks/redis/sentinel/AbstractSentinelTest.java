package com.lambdaworks.redis.sentinel;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.async.RedisSentinelAsyncCommands;

public abstract class AbstractSentinelTest {

    public static final String MASTER_ID = "mymaster";
    public static final String SLAVE_ID = "myslave";
    public static final String MASTER_WITH_SLAVE_ID = "master_with_slave";

    protected static RedisClient sentinelClient;
    protected RedisSentinelAsyncCommands<String, String> sentinel;

    @AfterClass
    public static void shutdownClient() {
        sentinelClient.shutdown(0, 0, TimeUnit.MILLISECONDS);
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinelAsync();
    }

    @After
    public void closeConnection() throws Exception {
        if (sentinel != null) {
            sentinel.close();
        }
    }

}
