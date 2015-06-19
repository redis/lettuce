package com.lambdaworks.redis.cluster.commands;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.StatefulRedisClusterConnectionImpl;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.commands.ListCommandTest;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ListClusterCommandTest extends ListCommandTest {
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
    protected RedisCommands<String, String> connect() {
        clusterConnection = (StatefulRedisClusterConnectionImpl) redisClusterClient.connectCluster().getStatefulConnection();
        InvocationHandler h = clusterConnection.syncInvocationHandler();
        return (RedisCommands) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { RedisCommands.class }, h);
    }

    // re-implementation because keys have to be on the same slot
    @Test
    public void brpoplpush() throws Exception {

        redis.rpush("UKPDHs8Zlp", "1", "2");
        redis.rpush("br7EPz9bbj", "3", "4");
        Assertions.assertThat(redis.brpoplpush(1, "UKPDHs8Zlp", "br7EPz9bbj")).isEqualTo("2");
        Assertions.assertThat(redis.lrange("UKPDHs8Zlp", 0, -1)).isEqualTo(list("1"));
        Assertions.assertThat(redis.lrange("br7EPz9bbj", 0, -1)).isEqualTo(list("2", "3", "4"));
    }

    @Test
    public void brpoplpushTimeout() throws Exception {
        Assertions.assertThat(redis.brpoplpush(1, "UKPDHs8Zlp", "br7EPz9bbj")).isNull();
    }

    @Test
    public void blpop() throws Exception {
        redis.rpush("br7EPz9bbj", "2", "3");
        Assertions.assertThat(redis.blpop(1, "UKPDHs8Zlp", "br7EPz9bbj")).isEqualTo(kv("br7EPz9bbj", "2"));
    }

    @Test
    public void brpop() throws Exception {
        redis.rpush("br7EPz9bbj", "2", "3");
        Assertions.assertThat(redis.brpop(1, "UKPDHs8Zlp", "br7EPz9bbj")).isEqualTo(kv("br7EPz9bbj", "3"));
    }

    @Test
    public void rpoplpush() throws Exception {
        Assertions.assertThat(redis.rpoplpush("UKPDHs8Zlp", "br7EPz9bbj")).isNull();
        redis.rpush("UKPDHs8Zlp", "1", "2");
        redis.rpush("br7EPz9bbj", "3", "4");
        Assertions.assertThat(redis.rpoplpush("UKPDHs8Zlp", "br7EPz9bbj")).isEqualTo("2");
        Assertions.assertThat(redis.lrange("UKPDHs8Zlp", 0, -1)).isEqualTo(list("1"));
        Assertions.assertThat(redis.lrange("br7EPz9bbj", 0, -1)).isEqualTo(list("2", "3", "4"));
    }

}
