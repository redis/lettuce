package com.lambdaworks.redis.sentinel;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.AbstractTest;
import com.lambdaworks.redis.api.sync.RedisSentinelCommands;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.async.RedisSentinelAsyncCommands;

public abstract class AbstractSentinelTest extends AbstractTest {

    public static final String MASTER_ID = "mymaster";

    protected static RedisClient sentinelClient;
    protected RedisSentinelCommands<String, String> sentinel;

    @AfterClass
    public static void shutdownClient() {
        sentinelClient.shutdown(0, 0, TimeUnit.MILLISECONDS);
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinelAsync().getStatefulConnection().sync();
    }

    @After
    public void closeConnection() throws Exception {
        if (sentinel != null) {
            sentinel.close();
        }
    }

}
