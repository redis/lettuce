package io.lettuce.test.resource;

import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.failover.MultiDbTestSupport;

/**
 * @author Ali Takavci
 */
public class DefaultRedisMultiDbClient {

    private static final DefaultRedisMultiDbClient instance = new DefaultRedisMultiDbClient();

    private final MultiDbClient redisClient;

    private DefaultRedisMultiDbClient() {
        redisClient = MultiDbClient.create(MultiDbTestSupport.DBs);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FastShutdown.shutdown(redisClient)));
    }

    /**
     * Do not close the client.
     *
     * @return the default redis client for the tests.
     */
    public static MultiDbClient get() {
        return instance.redisClient;
    }

}
