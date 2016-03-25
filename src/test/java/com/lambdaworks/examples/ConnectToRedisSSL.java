package com.lambdaworks.examples;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;

/**
 * @author Mark Paluch
 */
public class ConnectToRedisSSL {

    public static void main(String[] args) {
        // Syntax: rediss://[password@]host[:port][/databaseNumber]
        // Adopt the port to the stunnel port in front of your Redis instance
        RedisClient redisClient = RedisClient.create("rediss://password@localhost:6443/0");
        StatefulRedisConnection<String, String> connection = redisClient.connect();

        System.out.println("Connected to Redis using SSL");

        connection.close();
        redisClient.shutdown();
    }
}
