package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

/**
 * @author Mark Paluch
 */
public class DefaultRedisClient {

    public final static DefaultRedisClient instance = new DefaultRedisClient();

    private RedisClient redisClient;

    public DefaultRedisClient() {
        redisClient = RedisClient.create(RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build());
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                FastShutdown.shutdown(redisClient);
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
