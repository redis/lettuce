package com.lambdaworks.examples;

import com.lambdaworks.redis.*;

/**
 * @author Mark Paluch
 */
public class ConnectToRedisSSL {

    public static void main(String[] args) {
        // Syntax: rediss://[password@]host[:port][/databaseNumber]
        // Adopt the port to the stunnel port in front of your Redis instance
        RedisClient redisClient = new RedisClient(RedisURI.create("rediss://password@localhost:6443/0"));
        RedisConnection<String, String> connection = redisClient.connect();

        System.out.println("Connected to Redis using SSL");

        connection.close();
        redisClient.shutdown();
    }
}
