package com.lambdaworks.redis;

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

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.WaitFor;

public class SentinelFailoverTest extends AbstractCommandTest {

    public static final String MASTER_WITH_SLAVE_ID = "master_with_slave";

    private static RedisClient sentinelClient;
    private RedisSentinelAsyncConnection<String, String> sentinel;

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

    }

    @After
    public void closeConnection() throws Exception {
        sentinel.close();
    }

    @Test
    public void connectToRedisUsingSentinel() throws Exception {

        waitForAvailableSlave();

        RedisConnection<String, String> connect = sentinelClient.connect();
        assertThat(connect.ping()).isEqualToIgnoringCase("PONG");

        connect.close();
        this.sentinel.failover(MASTER_WITH_SLAVE_ID).get();
        waitForAvailableSlave();

        RedisConnection<String, String> connect2 = sentinelClient.connect();
        assertThat(connect2.ping()).isEqualToIgnoringCase("PONG");
        connect2.close();
    }

    protected void waitForAvailableSlave() throws InterruptedException, TimeoutException {
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return sentinelRule.hasConnectedSlaves(MASTER_WITH_SLAVE_ID)
                        && sentinel.getMasterAddrByName(MASTER_WITH_SLAVE_ID) != null;
            }
        }, timeout(seconds(20)));
    }

    protected static RedisClient getRedisSentinelClient() {
        return new RedisClient(RedisURI.Builder.sentinel(host, 26380, MASTER_WITH_SLAVE_ID).build());
    }
}
