package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 08.07.15 11:02
 */
public class FastShutdown {

    /**
     * Shut down the redis client with a timeout of 10ms.
     * 
     * @param redisClient
     */
    public static void shutdown(AbstractRedisClient redisClient) {
        redisClient.shutdown(10, 10, TimeUnit.MILLISECONDS);
    }
}
