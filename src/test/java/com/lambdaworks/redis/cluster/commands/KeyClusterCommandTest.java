package com.lambdaworks.redis.cluster.commands;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;

/**
 * @author Mark Paluch
 */
public class KeyClusterCommandTest extends AbstractRedisClientTest {

    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = RedisClusterClient
                .create(RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
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
    public void del() throws Exception {

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.del(key, "a", "b")).isEqualTo(3);
        assertThat(redis.exists(key)).isEqualTo(0);
        assertThat(redis.exists("a")).isEqualTo(0);
        assertThat(redis.exists("b")).isEqualTo(0);
    }

    @Test
    public void exists() throws Exception {

        assertThat(redis.exists(key, "a", "b")).isEqualTo(0);

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.exists(key, "a", "b")).isEqualTo(3);
    }

    @Test
    public void touch() throws Exception {

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.touch(key, "a", "b")).isEqualTo(3);
        assertThat(redis.exists(key, "a", "b")).isEqualTo(3);
    }

    @Test
    public void unlink() throws Exception {

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.unlink(key, "a", "b")).isEqualTo(3);
        assertThat(redis.exists(key)).isEqualTo(0);
    }

}
