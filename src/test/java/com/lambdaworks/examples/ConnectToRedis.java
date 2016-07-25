package com.lambdaworks.examples;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;

/**
 * @author Mark Paluch
 */
public class ConnectToRedis {

    public static void main(String[] args) {

        // Syntax: redis://[password@]host[:port][/databaseNumber]
        RedisClient redisClient = RedisClient.create("redis://password@localhost:6379/0");
        StatefulRedisConnection<String, String> connection = redisClient.connect();

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }
}
