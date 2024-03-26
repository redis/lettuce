package io.lettuce.examples;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;

/**
 * @author Mark Paluch
 */
public class ConnectToRedisClusterSSL {

    public static void main(String[] args) {

        // Syntax: rediss://[password@]host[:port]
        RedisURI redisURI = RedisURI.create("rediss://password@localhost:7379");
        redisURI.setVerifyPeer(false); // depending on your setup, you might want to disable peer verification

        RedisClusterClient redisClient = RedisClusterClient.create(redisURI);
        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }
}
