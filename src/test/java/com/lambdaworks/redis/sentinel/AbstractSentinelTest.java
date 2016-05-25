package com.lambdaworks.redis.sentinel;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import com.lambdaworks.redis.AbstractTest;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.sentinel.api.sync.RedisSentinelCommands;

import static sun.java2d.opengl.OGLRenderQueue.sync;

public abstract class AbstractSentinelTest extends AbstractTest {

    public static final String MASTER_ID = "mymaster";

    protected static RedisClient sentinelClient;
    protected RedisSentinelCommands<String, String> sentinel;

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(sentinelClient);
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinel().sync();
    }

    @After
    public void closeConnection() throws Exception {
        if (sentinel != null) {
            sentinel.close();
        }
    }

}
