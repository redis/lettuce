package com.lambdaworks.redis.support;

import javax.inject.Inject;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.cluster.RedisClusterClient;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 09.08.14 20:09
 */
public class InjectedClient {

    @Inject
    public RedisClient redisClient;

    @Inject
    public RedisClusterClient redisClusterClient;
}
