package com.lambdaworks.redis.support;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;

/**
 * CDI Producer for RedisClient.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 15.05.14 21:27
 */
public class CdiProducer {

    /**
     * Creates a Redis client using a provided RedisURI..
     * 
     * @param redisURI
     * @return RedisClient
     */
    @Produces
    public RedisClient create(RedisURI redisURI) {
        return new RedisClient(redisURI);
    }

    /**
     * Disposes the RedisClient.
     * 
     * @param redisClient
     */
    public void disposes(@Disposes RedisClient redisClient) {
        redisClient.shutdown();
    }

}
