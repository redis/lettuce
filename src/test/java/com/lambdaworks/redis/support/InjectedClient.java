package com.lambdaworks.redis.support;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;

/**
 * @author Mark Paluch
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

    private RedisCommands<String, String> connection;

    @PostConstruct
    public void postConstruct() {
        connection = redisClient.connect().sync();
    }

    public void pingRedis() {
        connection.ping();
    }

    @PreDestroy
    public void preDestroy() {
        if (connection != null) {
            connection.getStatefulConnection().close();
        }
    }
}
