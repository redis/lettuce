package com.lambdaworks.examples;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.cluster.ClusterClientOptions;
import com.lambdaworks.redis.cluster.ClusterTopologyRefreshOptions;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;

/**
 * @author Mark Paluch
 */
public class ConnectToRedisClusterWithTopologyRefreshing {

    public static void main(String[] args) {
        // Syntax: redis://[password@]host[:port]

        RedisClusterClient redisClient = RedisClusterClient.create("redis://password@localhost:7379");
        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = new ClusterTopologyRefreshOptions.Builder()//
                .enablePeriodicRefresh(30, TimeUnit.MINUTES)//
                .enableAllAdaptiveRefreshTriggers()//
                .build();

        ClusterClientOptions clusterClientOptions = new ClusterClientOptions.Builder()//
                .topologyRefreshOptions(clusterTopologyRefreshOptions)//
                .build();

        redisClient.setOptions(clusterClientOptions);

        StatefulRedisClusterConnection<String, String> connection = redisClient.connect();

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }
}
