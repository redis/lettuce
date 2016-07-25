package com.lambdaworks.redis.cluster.commands;

import static com.lambdaworks.redis.cluster.ClusterTestUtil.flushDatabaseOfAllNodes;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.commands.GeoCommandTest;

/**
 * @author Mark Paluch
 */
public class GeoClusterCommandTest extends GeoCommandTest {
    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = RedisClusterClient.create(
                RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
    }

    @AfterClass
    public static void closeClient() {
        FastShutdown.shutdown(redisClusterClient);
    }

    @Before
    public void openConnection() throws Exception {
        redis = connect();
        flushDatabaseOfAllNodes(clusterConnection);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RedisCommands<String, String> connect() {
        clusterConnection = redisClusterClient.connect();
        return ClusterTestUtil.redisCommandsOverCluster(clusterConnection);
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void geoaddWithTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void geoaddMultiWithTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void georadiusWithTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void geodistWithTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void georadiusWithArgsAndTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void georadiusbymemberWithArgsAndTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void geoposWithTransaction() throws Exception {
    }

    @Ignore("MULTI not available on Redis Cluster")
    @Override
    public void geohashWithTransaction() throws Exception {
    }
}
