package com.lambdaworks.redis.cluster.commands.rx;

import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.commands.HashCommandTest;
import com.lambdaworks.redis.commands.rx.RxSyncInvocationHandler;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class HashClusterRxCommandTest extends HashCommandTest {

    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = new RedisClusterClient(RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
    }

    @AfterClass
    public static void closeClient() {
        FastShutdown.shutdown(redisClusterClient);
    }

    @Before
    public void openConnection() throws Exception {
        redis = connect();
        ClusterTestUtil.flushDatabaseOfAllNodes(clusterConnection);
    }

    @Override
    protected RedisCommands<String, String> connect() {
        clusterConnection = redisClusterClient.connectCluster().getStatefulConnection();
        return RxSyncInvocationHandler.sync(redisClusterClient.connectCluster().getStatefulConnection());
    }
}
