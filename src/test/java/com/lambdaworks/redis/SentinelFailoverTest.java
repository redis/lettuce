package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.*;
import static com.google.code.tempusfugit.temporal.Timeout.*;
import static com.lambdaworks.redis.TestSettings.port;
import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.*;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;

public class SentinelFailoverTest extends AbstractCommandTest {

    public static final String MASTER_ID = "mymaster";

    private static RedisClient sentinelClient;
    private RedisSentinelAsyncConnection<String, String> sentinel;

    @Rule
    public SentinelRule sentinelRule = new SentinelRule(sentinelClient, false, 26379, 26380);

    @BeforeClass
    public static void setupClient() {
        sentinelClient = getRedisSentinelClient();
    }

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(sentinelClient);
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinelAsync();
    }

    @After
    public void closeConnection() throws Exception {
        sentinel.close();
    }

    @Test
    public void connectToRedisUsingSentinel() throws Exception {

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return sentinelRule.hasConnectedSlaves(MASTER_ID);
            }
        }, timeout(seconds(20)));

        RedisConnection<String, String> connect = sentinelClient.connect();
        assertThat(connect.ping()).isEqualToIgnoringCase("PONG");

        connect.close();
        this.sentinel.failover(MASTER_ID).get();

        RedisConnection<String, String> connect2 = sentinelClient.connect();
        assertThat(connect2.ping()).isEqualToIgnoringCase("PONG");
        connect2.close();
    }

    protected static RedisClient getRedisSentinelClient() {
        return new RedisClient(RedisURI.Builder.sentinel(host, 26380, MASTER_ID).build());
    }
}
