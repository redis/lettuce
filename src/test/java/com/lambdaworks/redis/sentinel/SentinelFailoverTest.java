package com.lambdaworks.redis.sentinel;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.Timeout.timeout;
import static com.lambdaworks.redis.TestSettings.port;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.async.RedisSentinelAsyncCommands;

public class SentinelFailoverTest extends AbstractRedisClientTest {

    public static final String MASTER_WITH_SLAVE_ID = "master_with_slave";

    private static RedisClient sentinelClient;
    private RedisSentinelAsyncCommands<String, String> sentinel;

    @Rule
    public SentinelRule sentinelRule = new SentinelRule(sentinelClient, 26379, 26380);

    @BeforeClass
    public static void setupClient() {
        sentinelClient = getRedisSentinelClient();
    }

    @AfterClass
    public static void shutdownClient() {
        sentinelClient.shutdown(0, 0, TimeUnit.MILLISECONDS);
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinelAsync();

        int masterPort = sentinelRule.findMaster(port(5), port(6));
        sentinelRule.monitor(MASTER_WITH_SLAVE_ID, TestSettings.hostAddr(), masterPort, 1, true);
        waitForSlave();

    }

    @After
    public void closeConnection() throws Exception {
        sentinel.close();
    }

    @Test
    public void connectToRedisUsingSentinel() throws Exception {

        waitForSlave();

        RedisConnection<String, String> connect = sentinelClient.connect();
        assertThat(connect.ping()).isEqualToIgnoringCase("PONG");

        connect.close();
        WaitFor.waitOrTimeout(() -> {
            try {
                this.sentinel.failover(MASTER_WITH_SLAVE_ID).get();
                return true;
            } catch (Exception e) {
                return false;
            }
        }, timeout(seconds(10)));

        waitForSlave();

        RedisConnection<String, String> connect2 = sentinelClient.connect();
        assertThat(connect2.ping()).isEqualToIgnoringCase("PONG");
        connect2.close();
    }

    protected void waitForSlave() throws InterruptedException, TimeoutException {
        WaitFor.waitOrTimeout(() -> {
            return sentinelRule.hasConnectedSlaves(MASTER_WITH_SLAVE_ID);
        }, timeout(seconds(10)));
    }

    protected static RedisClient getRedisSentinelClient() {
        return new RedisClient(RedisURI.Builder.sentinel(host, 26380, MASTER_WITH_SLAVE_ID).build());
    }
}
