package io.lettuce.test.resource;

import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.failover.MultiDbTestSupport;
import io.lettuce.core.failover.api.MultiDbOptions;

/**
 * @author Ali Takavci
 */
public class DefaultRedisMultiDbClient {

    private static final DefaultRedisMultiDbClient instance = new DefaultRedisMultiDbClient();

    private final MultiDbClient redisClient;

    private final MultiDbClient redisClientNoFailback;

    private DefaultRedisMultiDbClient() {

        redisClient = MultiDbClient.create(MultiDbTestSupport.DBs);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FastShutdown.shutdown(redisClient)));

        redisClientNoFailback = MultiDbClient.create(MultiDbTestSupport.DBs,
                MultiDbOptions.builder().failbackSupported(false).build());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FastShutdown.shutdown(redisClientNoFailback)));

    }

    /**
     * Do not close the client.
     *
     * @return the default redis client for the tests.
     */
    public static MultiDbClient get() {
        return instance.redisClient;
    }

    /**
     * Do not close the client.
     *
     * @return the default redis client for the tests.
     */
    public static MultiDbClient getNoFailback() {
        return instance.redisClientNoFailback;
    }

}
