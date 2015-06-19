package com.lambdaworks.redis.cluster.commands.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.ListStreamingAdapter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.commands.StringCommandTest;
import com.lambdaworks.redis.commands.rx.RxSyncInvocationHandler;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class StringClusterRxCommandTest extends StringCommandTest {
    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

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
    protected RedisCommands<String, String> connect() {
        clusterConnection = redisClusterClient.connectCluster().getStatefulConnection();
        return RxSyncInvocationHandler.sync(redisClusterClient.connectCluster().getStatefulConnection());
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
    @Ignore("fix me with #66")
    public void mgetStreaming() throws Exception {
        super.mgetStreaming();
    }
}
