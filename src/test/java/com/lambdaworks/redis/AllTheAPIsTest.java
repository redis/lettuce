package com.lambdaworks.redis;

import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.AsyncNodeSelection;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class AllTheAPIsTest {

    private static RedisClient redisClient = DefaultRedisClient.get();
    private static RedisClusterClient clusterClient;
    private static int clusterPort;

    @BeforeClass
    public static void beforeClass() throws Exception {
        clusterPort = TestSettings.port(900);
        clusterClient = new RedisClusterClient(RedisURI.Builder.redis(TestSettings.host(), clusterPort).build());
    }

    @BeforeClass
    public static void afterClass() throws Exception {
        if (clusterClient != null) {
            clusterClient.shutdown();
        }
    }

    // Standalone
    @Test
    public void standaloneSync() throws Exception {
        redisClient.connect().close();
    }

    @Test
    public void standaloneAsync() throws Exception {
        redisClient.connectAsync().close();
    }

    @Test
    public void standaloneReactive() throws Exception {
        redisClient.connectAsync().getStatefulConnection().reactive().close();
    }

    @Test
    public void standaloneStateful() throws Exception {
        redisClient.connectAsync().getStatefulConnection().close();
    }

    // PubSub
    @Test
    public void pubsubSync() throws Exception {
        redisClient.connectPubSub().getStatefulConnection().sync().close();
    }

    @Test
    public void pubsubAsync() throws Exception {
        redisClient.connectPubSub().getStatefulConnection().async().close();
    }

    @Test
    public void pubsubReactive() throws Exception {
        redisClient.connectPubSub().getStatefulConnection().reactive().close();
    }

    @Test
    public void pubsubStateful() throws Exception {
        redisClient.connectPubSub().getStatefulConnection().close();
    }

    // Sentinel
    @Test
    public void sentinelSync() throws Exception {
        redisClient.connectSentinelAsync().getStatefulConnection().sync().close();
    }

    @Test
    public void sentinelAsync() throws Exception {
        redisClient.connectSentinelAsync().getStatefulConnection().async().close();
    }

    @Test
    public void sentinelReactive() throws Exception {
        redisClient.connectSentinelAsync().getStatefulConnection().reactive().close();
    }

    @Test
    public void sentinelStateful() throws Exception {
        redisClient.connectSentinelAsync().getStatefulConnection().close();
    }

    // Pool
    @Test
    public void poolSync() throws Exception {
        redisClient.pool().close();
    }

    @Test
    public void poolAsync() throws Exception {
        redisClient.asyncPool().close();
    }

    // Cluster
    @Test
    public void clusterSync() throws Exception {
        clusterClient.connectCluster().getStatefulConnection().sync().close();
    }

    @Test
    public void clusterAsync() throws Exception {
        clusterClient.connectCluster().getStatefulConnection().async().close();
    }

    @Test
    public void clusterReactive() throws Exception {
        clusterClient.connectCluster().getStatefulConnection().reactive().close();
    }

    @Test
    public void clusterStateful() throws Exception {
        clusterClient.connectCluster().getStatefulConnection().close();
    }

    // Advanced Cluster
    @Test
    public void advancedClusterSync() throws Exception {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connectCluster()
                .getStatefulConnection();
        statefulConnection.getConnection(TestSettings.host(), clusterPort).sync();
        statefulConnection.close();
    }

    @Test
    public void advancedClusterAsync() throws Exception {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connectCluster()
                .getStatefulConnection();
        statefulConnection.getConnection(TestSettings.host(), clusterPort).async();
        statefulConnection.close();
    }

    @Test
    public void advancedClusterReactive() throws Exception {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connectCluster()
                .getStatefulConnection();
        statefulConnection.getConnection(TestSettings.host(), clusterPort).reactive();
        statefulConnection.close();
    }

    @Test
    public void advancedClusterStateful() throws Exception {
        clusterClient.connectCluster().getStatefulConnection().close();
    }

    // Cluster node selection
    @Test
    public void nodeSelectionClusterAsync() throws Exception {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connectCluster()
                .getStatefulConnection();
        AsyncNodeSelection<String, String> masters = statefulConnection.async().masters();
        statefulConnection.close();
    }

}
