package com.lambdaworks.redis.support;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;

/**
 * CDI Producer for RedisClient.
 * 
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 15.05.14 21:27
 */
public class CdiProducer {

    @Produces
    public RedisClient create(RedisURI redisURI) {
        return new RedisClient(redisURI);
    }

    public void disposes(@Disposes RedisClient redisClient) {
        redisClient.shutdown();
    }

}
