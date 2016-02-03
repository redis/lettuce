package com.lambdaworks.redis.cluster.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;

import org.junit.*;

import com.google.common.collect.Maps;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.ListStreamingAdapter;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.commands.StringCommandTest;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class StringClusterCommandTest extends StringCommandTest {
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
    @SuppressWarnings("unchecked")
    protected RedisCommands<String, String> connect() {
        clusterConnection = redisClusterClient.connectCluster().getStatefulConnection();
        return ClusterTestUtil.redisCommandsOverCluster(clusterConnection);
    }

    @Test
    public void msetnx() throws Exception {
        redis.set("one", "1");
        Map<String, String> map = Maps.newLinkedHashMap();
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
        Long count = redis.mget(streamingAdapter, "one", "two");

        assertEquals(new HashSet<>(list("1", "2")), new HashSet<String>(streamingAdapter.getList()));

        assertEquals(2, count.intValue());
    }
}
