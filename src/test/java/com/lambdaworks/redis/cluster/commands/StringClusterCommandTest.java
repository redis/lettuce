package com.lambdaworks.redis.cluster.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.ListStreamingAdapter;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.commands.StringCommandTest;
import com.lambdaworks.redis.internal.LettuceSets;

/**
 * @author Mark Paluch
 */
public class StringClusterCommandTest extends StringCommandTest {
    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = RedisClusterClient.create(RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
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
        clusterConnection = redisClusterClient.connect();
        return ClusterTestUtil.redisCommandsOverCluster(clusterConnection);
    }

    @Test
    public void msetnx() throws Exception {
        redis.set("one", "1");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        assertThat(redis.msetnx(map)).isTrue();
        redis.del("one");
        assertThat(redis.msetnx(map)).isTrue();
        assertThat(redis.get("two")).isEqualTo("2");
    }

    @Test
    public void mgetStreaming() throws Exception {
        setupMget();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();
        Long count = redis.mget(streamingAdapter, "one", "two");

        assertThat(LettuceSets.newHashSet(streamingAdapter.getList())).isEqualTo(LettuceSets.newHashSet(list("1", "2")));

        assertThat(count.intValue()).isEqualTo(2);
    }
}
