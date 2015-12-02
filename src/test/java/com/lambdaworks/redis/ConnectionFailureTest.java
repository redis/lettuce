package com.lambdaworks.redis;

import com.lambdaworks.Connections;
import com.lambdaworks.Wait;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.server.RandomResponseServer;
import io.netty.channel.Channel;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.StrictAssertions.assertThat;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ConnectionFailureTest extends AbstractRedisClientTest {

    private RedisURI defaultRedisUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();

    /**
     * Expect to run into Invalid first byte exception instead of timeout.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void pingBeforeConnectFails() throws Exception {

        client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.nonexistentPort())
                .withTimeout(10, TimeUnit.MINUTES).build();

        try {
            client.connect(redisUri);
        } catch (Exception e) {
            assertThat(e).isExactlyInstanceOf(RedisConnectionException.class);
            assertThat(e.getCause()).hasMessageContaining("Invalid first byte:");
        } finally {
            ts.shutdown();
        }
    }

    /**
     * Simulates a failure on reconnect by changing the port to a invalid server and triggering a reconnect. Meanwhile a command
     * is fired to the connection and the watchdog is triggered afterwards to reconnect.
     *
     * Expectation: Command after failed reconnect contains the reconnect exception.
     *
     * @throws Exception
     */
    @Test(timeout = 120000)
    public void pingBeforeConnectFailOnReconnect() throws Exception {

        client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true)
                .suspendReconnectOnProtocolFailure(true).build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = defaultRedisUri;
        redisUri.setTimeout(5);
        redisUri.setUnit(TimeUnit.SECONDS);

        try {
            RedisAsyncCommands<String, String> connection = client.connectAsync(redisUri);
            ConnectionWatchdog connectionWatchdog = Connections.getConnectionWatchdog(connection.getStatefulConnection());

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();
            assertThat(connectionWatchdog.isReconnectSuspended()).isFalse();

            redisUri.setPort(TestSettings.nonexistentPort());

            connection.quit();
            Wait.untilTrue(() -> connectionWatchdog.isReconnectSuspended()).waitOrTimeout();

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();

            try {
                connection.info().get(1, TimeUnit.MINUTES);
            } catch (ExecutionException e) {
                assertThat(e).hasRootCauseExactlyInstanceOf(RedisException.class);
                assertThat(e.getCause()).hasMessageStartingWith("Invalid first byte");
            }
        } finally {
            ts.shutdown();
        }
    }

    /**
     * Simulates a failure on reconnect by changing the port to a invalid server and triggering a reconnect. Meanwhile a command
     * is fired to the connection and the watchdog is triggered afterwards to reconnect.
     *
     * Expectation: Queued commands are canceled (reset), subsequent commands contain the connection exception.
     *
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void cancelCommandsOnReconnectFailure() throws Exception {

        client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).cancelCommandsOnReconnectFailure(true)
                .build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = defaultRedisUri;

        try {
            RedisAsyncCommandsImpl<String, String> connection = (RedisAsyncCommandsImpl<String, String>) client
                    .connectAsync(redisUri);
            ConnectionWatchdog connectionWatchdog = Connections.getConnectionWatchdog(connection.getStatefulConnection());

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();

            connectionWatchdog.setReconnectSuspended(true);
            redisUri.setPort(TestSettings.nonexistentPort());

            connection.quit();
            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

            RedisFuture<String> set1 = connection.set(key, value);
            RedisFuture<String> set2 = connection.set(key, value);

            assertThat(set1.isDone()).isFalse();
            assertThat(set1.isCancelled()).isFalse();

            assertThat(connection.isOpen()).isFalse();
            connectionWatchdog.setReconnectSuspended(false);
            connectionWatchdog.run(null);
            Thread.sleep(500);
            assertThat(connection.isOpen()).isFalse();

            try {
                set1.get();
            } catch (CancellationException e) {
                assertThat(e).hasNoCause();
            }

            try {
                set2.get();
            } catch (CancellationException e) {
                assertThat(e).hasNoCause();
            }

            try {
                connection.info().get();
            } catch (ExecutionException e) {
                assertThat(e).hasRootCauseExactlyInstanceOf(RedisException.class);
                assertThat(e.getCause()).hasMessageStartingWith("Invalid first byte");
            }
        } finally {
            ts.shutdown();
        }
    }

    protected RandomResponseServer getRandomResponseServer() throws InterruptedException {
        RandomResponseServer ts = new RandomResponseServer();
        ts.initialize(TestSettings.nonexistentPort());
        return ts;
    }
}
