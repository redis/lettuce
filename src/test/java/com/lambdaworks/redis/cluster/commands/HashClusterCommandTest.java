package com.lambdaworks.redis.cluster.commands;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.StatefulRedisClusterConnectionImpl;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.commands.HashCommandTest;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class HashClusterCommandTest extends HashCommandTest {

    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnectionImpl<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = new RedisClusterClient(RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
    }

    @AfterClass
    public static void closeClient() {
        redisClusterClient.shutdown(0, 0, TimeUnit.SECONDS);
    }

    @Before
    public void openConnection() throws Exception {
        redis = connect();

        for (RedisClusterNode node : clusterConnection.getPartitions()) {

            try {
                clusterConnection.getConnection(node.getNodeId()).sync().flushall();
                clusterConnection.getConnection(node.getNodeId()).sync().flushdb();
            } catch (Exception e) {
            }
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    protected RedisCommands<String, String> connect() {
        clusterConnection = (StatefulRedisClusterConnectionImpl) redisClusterClient.connectCluster().getStatefulConnection();
        InvocationHandler h = clusterConnection.syncInvocationHandler();
        return (RedisCommands<String, String>) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { RedisCommands.class }, h);
    }
}
