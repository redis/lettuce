package com.lambdaworks.examples;

import com.lambdaworks.redis.*;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 18.06.15 09:17
 */
public class ConnectToRedisUsingRedisSentinel {

    public static void main(String[] args) {
        // Syntax: redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId
        RedisClient redisClient = new RedisClient(
                RedisURI.create("redis-sentinel://localhost:26379,localhost:26380/0#mymaster"));
        RedisConnection<String, String> connection = redisClient.connect();

        System.out.println("Connected to Redis using Redis Sentinel");

        connection.close();
        redisClient.shutdown();
    }
}
