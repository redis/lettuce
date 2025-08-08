package io.lettuce.test.resource;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 * @author Hari Mani
 */
public class DefaultRedisClient {

    private static final DefaultRedisClient instance = new DefaultRedisClient();

    private final RedisClient redisClient;

    private DefaultRedisClient() {
        redisClient = RedisClient.create(RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FastShutdown.shutdown(redisClient)));
    }

    /**
     * Do not close the client.
     *
     * @return the default redis client for the tests.
     */
    public static RedisClient get() {
        return instance.redisClient;
    }

}
