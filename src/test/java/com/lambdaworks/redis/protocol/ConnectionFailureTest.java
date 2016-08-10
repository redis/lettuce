package com.lambdaworks.redis.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.*;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.ConnectionTestUtil;
import com.lambdaworks.Wait;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.server.RandomResponseServer;

/**
 * @author Mark Paluch
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

        client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

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

        ClientOptions clientOptions = ClientOptions.builder().pingBeforeActivateConnection(true)
                .suspendReconnectOnProtocolFailure(true).build();
        client.setOptions(clientOptions);

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();
        redisUri.setTimeout(5);
        redisUri.setUnit(TimeUnit.SECONDS);

        try {
            RedisAsyncCommands<String, String> connection = client.connect(redisUri).async();
            ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection.getStatefulConnection());

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();
            assertThat(connectionWatchdog.isReconnectSuspended()).isFalse();
            assertThat(clientOptions.isSuspendReconnectOnProtocolFailure()).isTrue();
            assertThat(connectionWatchdog.getReconnectionHandler().getClientOptions()).isSameAs(clientOptions);

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
            connection.getStatefulConnection().close();
        } finally {
            ts.shutdown();
        }
    }

    /**
     * Simulates a failure on reconnect by changing the port to a invalid server and triggering a reconnect.
     *
     * Expectation: {@link com.lambdaworks.redis.ConnectionEvents.Reconnect} events are sent.
     *
     * @throws Exception
     */
    @Test(timeout = 120000)
    public void pingBeforeConnectFailOnReconnectShouldSendEvents() throws Exception {

        client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true)
                .suspendReconnectOnProtocolFailure(false).build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = RedisURI.create(defaultRedisUri.toURI());
        redisUri.setTimeout(5);
        redisUri.setUnit(TimeUnit.SECONDS);

        try {
            final BlockingQueue<ConnectionEvents.Reconnect> events = new LinkedBlockingDeque<>();

            RedisAsyncCommands<String, String> connection = client.connect(redisUri).async();
            ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection.getStatefulConnection());

            ReconnectionListener reconnectionListener = new ReconnectionListener() {
                @Override
                public void onReconnect(ConnectionEvents.Reconnect reconnect) {
                    events.offer(reconnect);
                }
            };

            ReflectionTestUtils.setField(connectionWatchdog, "reconnectionListener", reconnectionListener);

            redisUri.setPort(TestSettings.nonexistentPort());

            connection.quit();
            Wait.untilTrue(() -> events.size() > 1).waitOrTimeout();
            connection.getStatefulConnection().close();

            ConnectionEvents.Reconnect event1 = events.take();
            assertThat(event1.getAttempt()).isEqualTo(1);

            ConnectionEvents.Reconnect event2 = events.take();
            assertThat(event2.getAttempt()).isEqualTo(2);

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

        client.setOptions(
                ClientOptions.builder().pingBeforeActivateConnection(true).cancelCommandsOnReconnectFailure(true).build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = RedisURI.create(defaultRedisUri.toURI());

        try {
            RedisAsyncCommandsImpl<String, String> connection = (RedisAsyncCommandsImpl<String, String>) client
                    .connect(redisUri).async();
            ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection.getStatefulConnection());

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();

            connectionWatchdog.setReconnectSuspended(true);
            redisUri.setPort(TestSettings.nonexistentPort());

            connection.quit();
            Wait.untilTrue(() -> !connection.getStatefulConnection().isOpen()).waitOrTimeout();

            RedisFuture<String> set1 = connection.set(key, value);
            RedisFuture<String> set2 = connection.set(key, value);

            assertThat(set1.isDone()).isFalse();
            assertThat(set1.isCancelled()).isFalse();

            assertThat(connection.getStatefulConnection().isOpen()).isFalse();
            connectionWatchdog.setReconnectSuspended(false);
            connectionWatchdog.run(0);
            Thread.sleep(500);
            assertThat(connection.getStatefulConnection().isOpen()).isFalse();

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

            connection.getStatefulConnection().close();
        } finally {
            ts.shutdown();
        }
    }

    /**
     * Expect to disable {@link ConnectionWatchdog} when closing a broken connection.
     *
     * @throws Exception
     */
    @Test
    public void closingDisconnectedConnectionShouldDisableConnectionWatchdog() throws Exception {

        client.setOptions(ClientOptions.create());

        RedisURI redisUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port())
                .withTimeout(10, TimeUnit.MINUTES).build();

        StatefulRedisConnection<String, String> connection = client.connect(redisUri);

        ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection);

        assertThat(connectionWatchdog.isReconnectSuspended()).isFalse();
        assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();

        connection.sync().ping();

        redisUri.setPort(TestSettings.nonexistentPort() + 5);

        connection.async().quit();
        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

        connection.close();

        assertThat(connectionWatchdog.isReconnectSuspended()).isTrue();
        assertThat(connectionWatchdog.isListenOnChannelInactive()).isFalse();
    }

    protected RandomResponseServer getRandomResponseServer() throws InterruptedException {
        RandomResponseServer ts = new RandomResponseServer();
        ts.initialize(TestSettings.nonexistentPort());
        return ts;
    }
}
