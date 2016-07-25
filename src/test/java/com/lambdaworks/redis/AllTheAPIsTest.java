package com.lambdaworks.redis;

import com.lambdaworks.TestClientResources;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.async.AsyncNodeSelection;

/**
 * @author Mark Paluch
 */
public class AllTheAPIsTest {

    private static RedisClient redisClient = DefaultRedisClient.get();
    private static RedisClusterClient clusterClient;
    private static int clusterPort;

    @BeforeClass
    public static void beforeClass() throws Exception {
        clusterPort = TestSettings.port(900);
        clusterClient = RedisClusterClient.create(
                TestClientResources.get(), RedisURI.Builder.redis(TestSettings.host(), clusterPort).build());
    }

    @BeforeClass
    public static void afterClass() throws Exception {
        if (clusterClient != null) {
            FastShutdown.shutdown(clusterClient);
        }
    }

    // Standalone
    @Test
    public void standaloneSync() throws Exception {
        redisClient.connect().close();
    }

    @Test
    public void standaloneAsync() throws Exception {
        redisClient.connect().async().getStatefulConnection().close();
    }

    @Test
    public void standaloneReactive() throws Exception {
        redisClient.connect().reactive().getStatefulConnection().close();
    }

    @Test
    public void standaloneStateful() throws Exception {
        redisClient.connect().close();
    }

    // PubSub
    @Test
    public void pubsubSync() throws Exception {
        redisClient.connectPubSub().close();
    }

    @Test
    public void pubsubAsync() throws Exception {
        redisClient.connectPubSub().close();
    }

    @Test
    public void pubsubReactive() throws Exception {
        redisClient.connectPubSub().close();
    }

    @Test
    public void pubsubStateful() throws Exception {
        redisClient.connectPubSub().close();
    }

    // Sentinel
    @Test
    public void sentinelSync() throws Exception {
        redisClient.connectSentinel().sync().close();
    }

    @Test
    public void sentinelAsync() throws Exception {
        redisClient.connectSentinel().async().getStatefulConnection().close();
    }

    @Test
    public void sentinelReactive() throws Exception {
        redisClient.connectSentinel().reactive().getStatefulConnection().close();
    }

    @Test
    public void sentinelStateful() throws Exception {
        redisClient.connectSentinel().close();
    }

    // Cluster
    @Test
    public void clusterSync() throws Exception {
        clusterClient.connect().sync().getStatefulConnection().close();
    }

    @Test
    public void clusterAsync() throws Exception {
        clusterClient.connect().async().getStatefulConnection().close();
    }

    @Test
    public void clusterReactive() throws Exception {
        clusterClient.connect().reactive().getStatefulConnection().close();
    }

    @Test
    public void clusterStateful() throws Exception {
        clusterClient.connect().close();
    }

    @Test
    public void clusterPubSubSync() throws Exception {
        clusterClient.connectPubSub().sync().getStatefulConnection().close();
    }

    @Test
    public void clusterPubSubAsync() throws Exception {
        clusterClient.connectPubSub().async().getStatefulConnection().close();
    }

    @Test
    public void clusterPubSubReactive() throws Exception {
        clusterClient.connectPubSub().reactive().getStatefulConnection().close();
    }

    @Test
    public void clusterPubSubStateful() throws Exception {
        clusterClient.connectPubSub().close();
    }

    // Advanced Cluster
    @Test
    public void advancedClusterSync() throws Exception {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connect();
        RedisURI uri = clusterClient.getPartitions().getPartition(0).getUri();
        statefulConnection.getConnection(uri.getHost(), uri.getPort()).sync();
        statefulConnection.close();
    }

    @Test
    public void advancedClusterAsync() throws Exception {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connect();
        RedisURI uri = clusterClient.getPartitions().getPartition(0).getUri();
        statefulConnection.getConnection(uri.getHost(), uri.getPort()).sync();
        statefulConnection.close();
    }

    @Test
    public void advancedClusterReactive() throws Exception {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connect();
        RedisURI uri = clusterClient.getPartitions().getPartition(0).getUri();
        statefulConnection.getConnection(uri.getHost(), uri.getPort()).reactive();
        statefulConnection.close();
    }

    @Test
    public void advancedClusterStateful() throws Exception {
        clusterClient.connect().close();
    }

    // Cluster node selection
    @Test
    public void nodeSelectionClusterAsync() throws Exception {
        StatefulRedisClusterConnection<String, String> statefulConnection = clusterClient.connect();
        AsyncNodeSelection<String, String> masters = statefulConnection.async().masters();
        statefulConnection.close();
    }

}
