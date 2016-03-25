package com.lambdaworks.examples;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterConnection;
import com.lambdaworks.redis.cluster.RedisClusterClient;

/**
 * @author Mark Paluch
 */
public class ConnectToRedisCluster {

    public static void main(String[] args) {
        // Syntax: redis://[password@]host[:port]
        RedisClusterClient redisClient = new RedisClusterClient(RedisURI.create("redis://password@localhost:7379"));
        RedisAdvancedClusterConnection<String, String> connection = redisClient.connectCluster();

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }
}
