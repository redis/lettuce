package com.lambdaworks.redis.support;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.cluster.RedisClusterClient;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class InjectedClient {

    @Inject
    public RedisClient redisClient;

    @Inject
    public RedisClusterClient redisClusterClient;

    @Inject
    @PersonDB
    public RedisClient qualifiedRedisClient;

    @Inject
    @PersonDB
    public RedisClusterClient qualifiedRedisClusterClient;

    private RedisConnection<String, String> connection;

    @PostConstruct
    public void postConstruct() {
        connection = redisClient.connect();
    }

    public void pingRedis() {
        connection.ping();
    }

    @PreDestroy
    public void preDestroy() {
        if (connection != null) {
            connection.close();
        }
    }
}
