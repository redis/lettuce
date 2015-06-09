// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static com.lambdaworks.redis.ScriptOutputType.STATUS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Timeout;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.server.RandomResponseServer;
import io.netty.channel.Channel;

public class ClientTest extends AbstractRedisClientTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    public void openConnection() throws Exception {
        Logger logger = LogManager.getLogger("com.lambdaworks.redis.protocol");
        logger.setLevel(Level.ALL);
        super.openConnection();
    }

    @Override
    public void closeConnection() throws Exception {
        super.closeConnection();
        Logger logger = LogManager.getLogger("com.lambdaworks.redis.protocol");
        logger.setLevel(Level.INFO);
    }

    @Test(expected = RedisException.class)
    public void close() throws Exception {
        redis.close();
        redis.get(key);
    }

    @Test
    public void statefulConnectionFromSync() throws Exception {
        assertThat(redis.getStatefulConnection().sync()).isSameAs(redis);
    }

    @Test
    public void statefulConnectionFromAsync() throws Exception {
        RedisAsyncConnection<String, String> async = client.connectAsync();
        assertThat(async.getStatefulConnection().async()).isSameAs(async);
    }

    @Test
    public void variousClientOptions() throws Exception {

        RedisAsyncConnection<String, String> plain = client.connectAsync();

        assertThat(getStatefulConnection(plain).getOptions().isAutoReconnect()).isTrue();

        client.setOptions(new ClientOptions.Builder().autoReconnect(false).build());
        RedisAsyncConnection<String, String> connection = client.connectAsync();
        assertThat(getStatefulConnection(connection).getOptions().isAutoReconnect()).isFalse();

        assertThat(getStatefulConnection(plain).getOptions().isAutoReconnect()).isTrue();

    }

    @Test(timeout = 10000)
    public void disconnectedConnectionWithoutReconnect() throws Exception {

        client.setOptions(new ClientOptions.Builder().autoReconnect(false).build());

        RedisAsyncConnection<String, String> connection = client.connectAsync();
        RedisChannelHandler<String, String> channelHandler = getStatefulConnection(connection);

        Channel channel = (Channel) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "channel");
        ConnectionWatchdog connectionWatchdog = channel.pipeline().get(ConnectionWatchdog.class);
        assertThat(connectionWatchdog).isNull();

        connection.quit();
        Thread.sleep(500);
        try {
            connection.get(key).get();
        } catch (Exception e) {
            assertThat(e).hasRootCauseInstanceOf(RedisException.class).hasMessageContaining(
                    "Connection is in a disconnected state and reconnect is disabled");
        } finally {
            connection.close();
        }
    }

    @Test(expected = RedisConnectionException.class)
    public void pingBeforeConnectFailsWithVeryShortTimeout() throws Exception {

        client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());

        RedisURI redisUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port())
                .withTimeout(1, TimeUnit.NANOSECONDS).build();

        client.connect(redisUri);
    }

    /**
     * Expect to run into invalid something exception instead of timeout.
     * 
     * @throws Exception
     */
    @Test(timeout = 10000)
    public void pingBeforeConnectFails() throws Exception {

        client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(500))
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
    @Test(timeout = 10000)
    public void pingBeforeConnectFailOnReconnect() throws Exception {

        client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true)
                .suspendReconnectOnProtocolFailure(true).build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = getDefaultRedisURI();

        try {
            RedisAsyncConnection<String, String> connection = client.connectAsync(redisUri);
            RedisChannelHandler<String, String> channelHandler = getStatefulConnection(connection);

            Channel channel = (Channel) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "channel");
            ConnectionWatchdog connectionWatchdog = channel.pipeline().get(ConnectionWatchdog.class);

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();
            assertThat(connectionWatchdog.isReconnectSuspended()).isFalse();

            connection.set(key, value);

            Thread.sleep(100);
            redisUri.setPort(TestSettings.port(500));
            ReflectionTestUtils.setField(redisUri, "resolvedAddress", null);

            connection.quit();
            Thread.sleep(500);
            assertThat(connection.isOpen()).isFalse();
            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();
            assertThat(connectionWatchdog.isReconnectSuspended()).isTrue();

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

    protected RandomResponseServer getRandomResponseServer() throws InterruptedException {
        RandomResponseServer ts = new RandomResponseServer();
        ts.initialize(TestSettings.port(500));
        return ts;
    }

    protected RedisURI getDefaultRedisURI() {
        return RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();
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

        RedisURI redisUri = getDefaultRedisURI();

        try {
            RedisAsyncConnectionImpl<String, String> connection = (RedisAsyncConnectionImpl) client.connectAsync(redisUri);
            RedisChannelHandler<String, String> channelHandler = (RedisChannelHandler<String, String>) connection
                    .getStatefulConnection();

            Channel channel = (Channel) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "channel");
            ConnectionWatchdog connectionWatchdog = channel.pipeline().get(ConnectionWatchdog.class);

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();

            connectionWatchdog.setReconnectSuspended(true);
            redisUri.setPort(TestSettings.port(500));
            ReflectionTestUtils.setField(redisUri, "resolvedAddress", null);

            connection.quit();
            Thread.sleep(100);

            assertThat(connection.isOpen()).isFalse();

            RedisFuture<String> set1 = connection.set(key, value);
            RedisFuture<String> set2 = connection.set(key, value);

            assertThat(set1.isDone()).isFalse();
            assertThat(set1.isCancelled()).isFalse();

            assertThat(connection.isOpen()).isFalse();
            connectionWatchdog.setReconnectSuspended(false);
            connectionWatchdog.scheduleReconnect();
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

    @Test(timeout = 10000)
    public void pingBeforeConnect() throws Exception {

        redis.set(key, value);
        client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true).build());
        RedisConnection<String, String> connection = client.connect();

        try {
            String result = connection.get(key);
            assertThat(result).isEqualTo(value);
        } finally {
            connection.close();
        }
    }

    @Test
    public void listenerTest() throws Exception {

        final TestConnectionListener listener = new TestConnectionListener();

        RedisClient client = new RedisClient(host, port);
        client.addListener(listener);

        assertThat(listener.onConnected).isNull();
        assertThat(listener.onDisconnected).isNull();
        assertThat(listener.onException).isNull();

        RedisAsyncConnection<String, String> connection = client.connectAsync();

        StatefulRedisConnection statefulRedisConnection = getStatefulConnection(connection);

        waitOrTimeout(new Condition() {

            @Override
            public boolean isSatisfied() {
                return listener.onConnected != null;
            }
        }, Timeout.timeout(seconds(2)));

        assertThat(listener.onConnected).isEqualTo(statefulRedisConnection);
        assertThat(listener.onDisconnected).isNull();

        connection.set(key, value).get();
        connection.close();

        waitOrTimeout(new Condition() {

            @Override
            public boolean isSatisfied() {
                return listener.onDisconnected != null;
            }
        }, Timeout.timeout(seconds(2)));

        assertThat(listener.onConnected).isEqualTo(statefulRedisConnection);
        assertThat(listener.onDisconnected).isEqualTo(statefulRedisConnection);

        client.shutdown();
    }

    @Test
    public void listenerTestWithRemoval() throws Exception {

        final TestConnectionListener removedListener = new TestConnectionListener();
        final TestConnectionListener retainedListener = new TestConnectionListener();

        RedisClient client = new RedisClient(host, port);
        client.addListener(removedListener);
        client.addListener(retainedListener);
        client.removeListener(removedListener);

        RedisAsyncConnection<String, String> connection = client.connectAsync();
        waitOrTimeout(new Condition() {

            @Override
            public boolean isSatisfied() {
                return retainedListener.onConnected != null;
            }
        }, Timeout.timeout(seconds(2)));

        assertThat(retainedListener.onConnected).isNotNull();

        assertThat(removedListener.onConnected).isNull();
        assertThat(removedListener.onDisconnected).isNull();
        assertThat(removedListener.onException).isNull();

        client.shutdown();

    }

    @Test(expected = RedisException.class)
    public void timeout() throws Exception {
        redis.setTimeout(0, TimeUnit.MICROSECONDS);
        redis.eval(" os.execute(\"sleep \" .. tonumber(1))", ScriptOutputType.STATUS);
    }

    @Test
    public void reconnect() throws Exception {

        redis.set(key, value);

        redis.quit();
        Thread.sleep(100);
        assertThat(redis.get(key)).isEqualTo(value);
        redis.quit();
        Thread.sleep(100);
        assertThat(redis.get(key)).isEqualTo(value);
        redis.quit();
        Thread.sleep(100);
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test(expected = RedisCommandInterruptedException.class, timeout = 10)
    public void interrupt() throws Exception {
        Thread.currentThread().interrupt();
        redis.blpop(0, key);
    }

    @Test
    public void connectFailure() throws Exception {
        RedisClient client = new RedisClient("invalid");
        exception.expect(RedisException.class);
        exception.expectMessage("Unable to connect");
        client.connect();
    }

    @Test
    public void connectPubSubFailure() throws Exception {
        RedisClient client = new RedisClient("invalid");
        exception.expect(RedisException.class);
        exception.expectMessage("Unable to connect");
        client.connectPubSub();
    }

    private class TestConnectionListener implements RedisConnectionStateListener {

        public RedisChannelHandler<?, ?> onConnected;
        public RedisChannelHandler<?, ?> onDisconnected;
        public RedisChannelHandler<?, ?> onException;

        @Override
        public void onRedisConnected(RedisChannelHandler<?, ?> connection) {
            onConnected = connection;
        }

        @Override
        public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
            onDisconnected = connection;
        }

        @Override
        public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, Throwable cause) {
            onException = connection;

        }
    }

    @Test
    public void emptyClient() throws Exception {

        RedisClient client = new RedisClient();
        try {
            client.connect();
        } catch (IllegalStateException e) {
            assertThat(e).hasMessageContaining("RedisURI");
        }

        try {
            client.connectAsync();
        } catch (IllegalStateException e) {
            assertThat(e).hasMessageContaining("RedisURI");
        }

        try {
            client.connect((RedisURI) null);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("RedisURI");
        }

        try {
            client.connectAsync((RedisURI) null);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("RedisURI");
        }
        client.shutdown();
    }

    @Test
    public void testExceptionWithCause() throws Exception {
        RedisException e = new RedisException(new RuntimeException());
        assertThat(e).hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test(timeout = 20000)
    public void reset() throws Exception {

        RedisAsyncConnectionImpl<String, String> async = (RedisAsyncConnectionImpl<String, String>) client.connectAsync();

        async.set(key, value).get();
        async.reset();
        async.set(key, value).get();

        RedisFuture<Object> eval = async.eval("while true do end", STATUS, new String[0]);
        Thread.sleep(200);
        assertThat(eval.isDone()).isFalse();
        assertThat(eval.isCancelled()).isFalse();

        async.reset();

        assertThat(eval.isCancelled()).isTrue();
        assertThat(eval.isDone()).isTrue();

        assertThat(redis.scriptKill()).isEqualTo("OK");

        async.close();
    }

    <K, V> StatefulRedisConnectionImpl<K, V> getStatefulConnection(RedisAsyncConnection redisAsyncConnection) {

        return (StatefulRedisConnectionImpl) redisAsyncConnection.getStatefulConnection();
    }

}
