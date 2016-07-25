package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import com.lambdaworks.TestClientResources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.sentinel.SentinelRule;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;

import io.netty.util.internal.SystemPropertyUtil;

/**
 * @author Mark Paluch
 */
public class UnixDomainSocketTest {

    public static final String MASTER_ID = "mymaster";

    private static RedisClient sentinelClient;

    @Rule
    public SentinelRule sentinelRule = new SentinelRule(sentinelClient, false, 26379, 26380);

    protected Logger log = LogManager.getLogger(getClass());

    protected String key = "key";
    protected String value = "value";

    @BeforeClass
    public static void setupClient() {
        sentinelClient = getRedisSentinelClient();
    }

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(sentinelClient);
    }

    @Test
    public void standalone_Linux_x86_64_RedisClientWithSocket() throws Exception {

        linuxOnly();

        RedisURI redisURI = getSocketRedisUri();

        RedisClient redisClient = RedisClient.create(TestClientResources.get(), redisURI);

        StatefulRedisConnection<String, String> connection = redisClient.connect();

        someRedisAction(connection.sync());
        connection.close();

        FastShutdown.shutdown(redisClient);
    }

    @Test
    public void standalone_Linux_x86_64_ConnectToSocket() throws Exception {

        linuxOnly();

        RedisURI redisURI = getSocketRedisUri();

        RedisClient redisClient = RedisClient.create(TestClientResources.get());

        StatefulRedisConnection<String, String> connection = redisClient.connect(redisURI);

        someRedisAction(connection.sync());
        connection.close();

        FastShutdown.shutdown(redisClient);
    }

    private void linuxOnly() {
        String osName = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim();
        assumeTrue("Only supported on Linux, your os is " + osName, osName.startsWith("linux"));
    }

    private RedisURI getSocketRedisUri() throws IOException {
        File file = new File(TestSettings.socket()).getCanonicalFile();
        return RedisURI.create(RedisURI.URI_SCHEME_REDIS_SOCKET + "://" + file.getCanonicalPath());
    }

    private RedisURI getSentinelSocketRedisUri() throws IOException {
        File file = new File(TestSettings.sentinelSocket()).getCanonicalFile();
        return RedisURI.create(RedisURI.URI_SCHEME_REDIS_SOCKET + "://" + file.getCanonicalPath());
    }

    @Test
    public void sentinel_Linux_x86_64_RedisClientWithSocket() throws Exception {

        linuxOnly();

        RedisURI uri = new RedisURI();
        uri.getSentinels().add(getSentinelSocketRedisUri());
        uri.setSentinelMasterId("mymaster");

        RedisClient redisClient = RedisClient.create(TestClientResources.get(), uri);

        StatefulRedisConnection<String, String> connection = redisClient.connect();

        someRedisAction(connection.sync());

        connection.close();

        StatefulRedisSentinelConnection<String, String> sentinelConnection = redisClient.connectSentinel();

        assertThat(sentinelConnection.sync().ping()).isEqualTo("PONG");
        sentinelConnection.close();

        FastShutdown.shutdown(redisClient);
    }

    @Test
    public void sentinel_Linux_x86_64_ConnectToSocket() throws Exception {

        linuxOnly();

        RedisURI uri = new RedisURI();
        uri.getSentinels().add(getSentinelSocketRedisUri());
        uri.setSentinelMasterId("mymaster");

        RedisClient redisClient = RedisClient.create(TestClientResources.get());

        StatefulRedisConnection<String, String> connection = redisClient.connect(uri);

        someRedisAction(connection.sync());

        connection.close();

        StatefulRedisSentinelConnection<String, String> sentinelConnection = redisClient.connectSentinel(uri);

        assertThat(sentinelConnection.sync().ping()).isEqualTo("PONG");
        sentinelConnection.close();

        FastShutdown.shutdown(redisClient);
    }

    @Test
    public void sentinel_Linux_x86_64_socket_and_inet() throws Exception {

        sentinelRule.waitForMaster(MASTER_ID);
        linuxOnly();

        RedisURI uri = new RedisURI();
        uri.getSentinels().add(getSentinelSocketRedisUri());
        uri.getSentinels().add(RedisURI.create(RedisURI.URI_SCHEME_REDIS + "://" + TestSettings.host() + ":26379"));
        uri.setSentinelMasterId(MASTER_ID);

        RedisClient redisClient = RedisClient.create(TestClientResources.get(), uri);

        StatefulRedisSentinelConnection<String, String> sentinelConnection = redisClient
                .connectSentinel(getSentinelSocketRedisUri());
        log.info("Masters: " + sentinelConnection.sync().masters());

        try {
            redisClient.connect();
            fail("Missing validation exception");
        } catch (RedisConnectionException e) {
            assertThat(e).hasMessageContaining("You cannot mix unix domain socket and IP socket URI's");
        } finally {
            FastShutdown.shutdown(redisClient);
        }

    }

    private void someRedisAction(RedisCommands<String, String> connection) {
        connection.set(key, value);
        String result = connection.get(key);

        assertThat(result).isEqualTo(value);
    }

    protected static RedisClient getRedisSentinelClient() {
        return RedisClient.create(TestClientResources.get(), RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build());
    }
}
