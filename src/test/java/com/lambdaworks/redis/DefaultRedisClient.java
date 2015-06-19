package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 16.06.15 15:23
 */
public class DefaultRedisClient {

    public final static DefaultRedisClient instance = new DefaultRedisClient();

    private RedisClient redisClient;

    public DefaultRedisClient() {
        redisClient = new RedisClient(TestSettings.host(), TestSettings.port());
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                redisClient.shutdown(0, 0, TimeUnit.MILLISECONDS);
            }
        });
    }

    /**
     * Do not close the client.
     * 
     * @return the default redis client for the tests.
     */
    public static RedisClient get() {
        instance.redisClient.setDefaultTimeout(60, TimeUnit.SECONDS);
        return instance.redisClient;
    }
}
