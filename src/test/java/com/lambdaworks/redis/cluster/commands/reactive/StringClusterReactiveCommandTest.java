package com.lambdaworks.redis.cluster.commands.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import com.lambdaworks.redis.KeyValue;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.commands.StringCommandTest;
import com.lambdaworks.util.ReactiveSyncInvocationHandler;

import reactor.core.publisher.Flux;

/**
 * @author Mark Paluch
 */
public class StringClusterReactiveCommandTest extends StringCommandTest {
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
    protected RedisCommands<String, String> connect() {
        clusterConnection = redisClusterClient.connect();
        return ReactiveSyncInvocationHandler.sync(redisClusterClient.connect());
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
    public void mget() throws Exception {

        redis.set(key, value);
        redis.set("key1", value);
        redis.set("key2", value);

        RedisAdvancedClusterReactiveCommands<String, String> reactive = clusterConnection.reactive();

        Flux<KeyValue<String, String>> mget = reactive.mget(key, "key1", "key2");
        KeyValue<String, String> first = mget.next().block();
        assertThat(first).isEqualTo(KeyValue.just(key, value));
    }

}
