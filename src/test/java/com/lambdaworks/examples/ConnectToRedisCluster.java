package com.lambdaworks.examples;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterConnection;
import com.lambdaworks.redis.cluster.RedisClusterClient;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 18.06.15 09:17
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
