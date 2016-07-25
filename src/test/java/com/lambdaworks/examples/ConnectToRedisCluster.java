package com.lambdaworks.examples;

import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;

/**
 * @author Mark Paluch
 */
public class ConnectToRedisCluster {

    public static void main(String[] args) {

        // Syntax: redis://[password@]host[:port]
        RedisClusterClient redisClient = RedisClusterClient.create("redis://password@localhost:7379");

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }
}
