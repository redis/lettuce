package io.lettuce.test.resource;

import java.util.concurrent.TimeUnit;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public class DefaultRedisClient {

    private static final DefaultRedisClient instance = new DefaultRedisClient();

    private RedisClient redisClient;

    private DefaultRedisClient() {
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
