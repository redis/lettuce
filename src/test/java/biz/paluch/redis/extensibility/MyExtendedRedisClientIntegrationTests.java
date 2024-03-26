package biz.paluch.redis.extensibility;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAsyncCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;

/**
 * Test for override/extensability of RedisClient
 */
class MyExtendedRedisClientIntegrationTests {

    private static final String host = TestSettings.host();

    private static final int port = TestSettings.port();

    private static MyExtendedRedisClient client;

    protected RedisCommands<String, String> redis;

    protected String key = "key";

    protected String value = "value";

    @BeforeAll
    static void setupClient() {
        client = getRedisClient();
    }

    static MyExtendedRedisClient getRedisClient() {
        return new MyExtendedRedisClient(null, RedisURI.create(host, port));
    }

    @AfterAll
    static void shutdownClient() {
        FastShutdown.shutdown(client);
    }

    @Test
    void testPubsub() throws Exception {
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();
        RedisPubSubAsyncCommands<String, String> commands = connection.async();
        assertThat(commands).isInstanceOf(RedisPubSubAsyncCommandsImpl.class);
        assertThat(commands.getStatefulConnection()).isInstanceOf(MyPubSubConnection.class);
        commands.set("key", "value").get();
        connection.close();
    }

}
