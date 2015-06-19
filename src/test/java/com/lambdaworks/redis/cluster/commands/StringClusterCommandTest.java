package com.lambdaworks.redis.cluster.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.ListStreamingAdapter;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.StatefulRedisClusterConnectionImpl;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.commands.StringCommandTest;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class StringClusterCommandTest extends StringCommandTest {
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

        flushClusterDb();
    }

    protected void flushClusterDb() {
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
        return (RedisCommands) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { RedisCommands.class }, h);
    }

    @Test
    public void msetnx() throws Exception {
        redis.set("one", "1");
        Map<String, String> map = new HashMap<String, String>();
        map.put("one", "1");
        map.put("two", "2");
        assertTrue(redis.msetnx(map));
        redis.del("one");
        assertTrue(redis.msetnx(map));
        Assert.assertEquals("2", redis.get("two"));
    }

    @Test
    public void mgetStreaming() throws Exception {
        setupMget();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();
        Long count = redis.mget(streamingAdapter, "one");

        assertEquals(list("1"), streamingAdapter.getList());

        assertEquals(1, count.intValue());
    }
}
