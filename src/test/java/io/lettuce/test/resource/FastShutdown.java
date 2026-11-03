package io.lettuce.test.resource;

import java.util.concurrent.TimeUnit;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.failover.MultiDbClient;
import io.lettuce.core.resource.ClientResources;

/**
 * @author Mark Paluch
 */
public class FastShutdown {

    /**
     * Shut down a {@link AbstractRedisClient} with a timeout of 10ms.
     *
     * @param redisClient
     */
    public static void shutdown(AbstractRedisClient redisClient) {
        redisClient.shutdown(0, 10, TimeUnit.MILLISECONDS);
    }

    /**
     * Shut down a {@link AbstractRedisClient} with a timeout of 10ms.
     *
     * @param redisClient
     */
    public static void shutdown(MultiDbClient redisClient) {
        redisClient.shutdown(0, 10, TimeUnit.MILLISECONDS);
    }

    /**
     * Shut down a {@link ClientResources} client with a timeout of 10ms.
     *
     * @param clientResources
     */
    public static void shutdown(ClientResources clientResources) {
        clientResources.shutdown(0, 10, TimeUnit.MILLISECONDS);
    }

}
