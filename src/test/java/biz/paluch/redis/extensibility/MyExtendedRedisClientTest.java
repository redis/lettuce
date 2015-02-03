package biz.paluch.redis.extensibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.pubsub.RedisPubSubConnection;

/**
 * Test for override/extensability of RedisClient
 */
public class MyExtendedRedisClientTest {
    public static final String host = TestSettings.host();
    public static final int port = TestSettings.port();

    protected static MyExtendedRedisClient client;
    protected Logger log = Logger.getLogger(getClass());
    protected RedisConnection<String, String> redis;
    protected String key = "key";
    protected String value = "value";

    @BeforeClass
    public static void setupClient() {
        client = getRedisClient();
    }

    protected static MyExtendedRedisClient getRedisClient() {
        return new MyExtendedRedisClient(host, port);
    }

    @AfterClass
    public static void shutdownClient() {
        client.shutdown(0, 0, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testPubsub() throws Exception {
        RedisPubSubConnection<String, String> connection = client.connectPubSub();
        assertThat(connection).isInstanceOf(MyPubSubConnection.class);
        connection.set("key", "value").get();
        connection.close();

    }
}