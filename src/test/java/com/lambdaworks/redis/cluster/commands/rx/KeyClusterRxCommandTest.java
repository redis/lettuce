package com.lambdaworks.redis.cluster.commands.rx;

import com.lambdaworks.TestClientResources;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.commands.KeyClusterCommandTest;
import com.lambdaworks.redis.commands.rx.RxSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
public class KeyClusterRxCommandTest extends KeyClusterCommandTest {
    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
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

    protected RedisCommands<String, String> connect() {
        clusterConnection = redisClusterClient.connect();
        return RxSyncInvocationHandler.sync(redisClusterClient.connect());
    }

}
