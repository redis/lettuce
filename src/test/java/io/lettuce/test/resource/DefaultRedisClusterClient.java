package io.lettuce.test.resource;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public class DefaultRedisClusterClient {

    private static final DefaultRedisClusterClient instance = new DefaultRedisClusterClient();

    private RedisClusterClient redisClient;

    private DefaultRedisClusterClient() {
        redisClient = RedisClusterClient.create(
                RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).withClientName("my-client").build());
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
    public static RedisClusterClient get() {
        instance.redisClient.setOptions(ClusterClientOptions.create());
        return instance.redisClient;
    }

}
