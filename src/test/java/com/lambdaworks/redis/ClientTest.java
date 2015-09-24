// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.google.code.tempusfugit.temporal.WaitFor.waitOrTimeout;
import static com.lambdaworks.redis.ScriptOutputType.STATUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.test.util.ReflectionTestUtils;

import rx.Subscription;
import rx.observers.TestSubscriber;

import com.google.code.tempusfugit.temporal.Condition;
import com.google.code.tempusfugit.temporal.Timeout;
import com.google.code.tempusfugit.temporal.WaitFor;
import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.connection.ConnectedEvent;
import com.lambdaworks.redis.event.connection.ConnectionActivatedEvent;
import com.lambdaworks.redis.event.connection.ConnectionDeactivatedEvent;
import com.lambdaworks.redis.event.connection.DisconnectedEvent;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.server.RandomResponseServer;
import io.netty.channel.Channel;

@SuppressWarnings("unchecked")
public class ClientTest extends AbstractCommandTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    public void openConnection() throws Exception {
        super.openConnection();
    }

    @Override
    public void closeConnection() throws Exception {
        super.closeConnection();
    }

    @Test(expected = RedisException.class)
    public void close() throws Exception {
        redis.close();
        redis.get(key);
    }

    @Test
    public void variousClientOptions() throws Exception {

        RedisAsyncConnectionImpl<String, String> plain = (RedisAsyncConnectionImpl) client.connectAsync();
        assertThat(plain.getOptions().isAutoReconnect()).isTrue();

        client.setOptions(new ClientOptions.Builder().autoReconnect(false).build());
        RedisAsyncConnectionImpl<String, String> connection = (RedisAsyncConnectionImpl) client.connectAsync();
        assertThat(connection.getOptions().isAutoReconnect()).isFalse();

        assertThat(plain.getOptions().isAutoReconnect()).isTrue();

    }

    @Test
    public void requestQueueSize() throws Exception {

        client.setOptions(new ClientOptions.Builder().requestQueueSize(10).build());

        final RedisAsyncConnectionImpl<String, String> connection = (RedisAsyncConnectionImpl) client.connectAsync();

        Channel channel = (Channel) ReflectionTestUtils.getField(connection.getChannelWriter(), "channel");
        ConnectionWatchdog connectionWatchdog = channel.pipeline().get(ConnectionWatchdog.class);
        connectionWatchdog.setListenOnChannelInactive(false);
        connection.quit();
        waitUntilDisconnected(connection);

        for (int i = 0; i < 10; i++) {
            connection.ping();
        }

        try {
            connection.ping();
            fail("missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Request queue size exceeded");
        }

        connection.close();
    }

    protected void waitUntilDisconnected(final RedisAsyncConnectionImpl<String, String> connection)
            throws InterruptedException, TimeoutException {
        Thread.sleep(200);
        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return !connection.isOpen();
            }
        }, Timeout.timeout(seconds(5)));
    }

    @Test(timeout = 10000)
    public void disconnectedConnectionWithoutReconnect() throws Exception {

        client.setOptions(new ClientOptions.Builder().autoReconnect(false).build());

        RedisAsyncConnectionImpl<String, String> connection = (RedisAsyncConnectionImpl) client.connectAsync();

        Channel channel = (Channel) ReflectionTestUtils.getField(connection.getChannelWriter(), "channel");
        ConnectionWatchdog connectionWatchdog = channel.pipeline().get(ConnectionWatchdog.class);
        assertThat(connectionWatchdog).isNull();

        connection.quit();
        waitUntilDisconnected(connection);
        try {
            connection.get(key);
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Connection is in a disconnected state and reconnect is disabled");
        } finally {
            connection.close();
        }
    }

    /**
     * Expect to run into Invalid first byte exception instead of timeout.
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
    @Test(timeout = 120000)
    public void pingBeforeConnectFailOnReconnect() throws Exception {

        client.setOptions(new ClientOptions.Builder().pingBeforeActivateConnection(true)
                .suspendReconnectOnProtocolFailure(true).build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = getDefaultRedisURI();
        redisUri.setTimeout(5);
        redisUri.setUnit(TimeUnit.SECONDS);

        try {
            RedisAsyncConnectionImpl<String, String> connection = (RedisAsyncConnectionImpl) client.connectAsync(redisUri);

            Channel channel = (Channel) ReflectionTestUtils.getField(connection.getChannelWriter(), "channel");
            ConnectionWatchdog connectionWatchdog = channel.pipeline().get(ConnectionWatchdog.class);

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();
            assertThat(connectionWatchdog.isReconnectSuspended()).isFalse();

            connection.set(key, value);

            Thread.sleep(100);
            redisUri.setPort(TestSettings.port(500));
            ReflectionTestUtils.setField(redisUri, "resolvedAddress", null);

            connection.quit();
            waitUntilDisconnected(connection);

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

            Channel channel = (Channel) ReflectionTestUtils.getField(connection.getChannelWriter(), "channel");
            ConnectionWatchdog connectionWatchdog = channel.pipeline().get(ConnectionWatchdog.class);

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();

            connectionWatchdog.setReconnectSuspended(true);
            redisUri.setPort(TestSettings.port(500));
            ReflectionTestUtils.setField(redisUri, "resolvedAddress", null);

            connection.quit();
            waitUntilDisconnected(connection);

            assertThat(connection.isOpen()).isFalse();

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
            } catch (ExecutionException e) {
                assertThat(e).hasRootCauseExactlyInstanceOf(RedisException.class);
                assertThat(e.getCause()).hasMessageStartingWith("Reset");
            }

            try {
                set2.get();
            } catch (ExecutionException e) {
                assertThat(e).hasRootCauseExactlyInstanceOf(RedisException.class);
                assertThat(e.getCause()).hasMessageStartingWith("Reset");
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

        RedisClient client = RedisClient.create(resources, RedisURI.Builder.redis(host, port).build());
        client.addListener(listener);

        assertThat(listener.onConnected).isNull();
        assertThat(listener.onDisconnected).isNull();
        assertThat(listener.onException).isNull();

        RedisAsyncConnection<String, String> connection = client.connectAsync();
        waitOrTimeout(new Condition() {

            @Override
            public boolean isSatisfied() {
                return listener.onConnected != null;
            }
        }, Timeout.timeout(seconds(2)));

        assertThat(listener.onConnected).isEqualTo(connection);
        assertThat(listener.onDisconnected).isNull();

        connection.set(key, value).get();
        connection.close();

        waitOrTimeout(new Condition() {

            @Override
            public boolean isSatisfied() {
                return listener.onDisconnected != null;
            }
        }, Timeout.timeout(seconds(2)));

        assertThat(listener.onConnected).isEqualTo(connection);
        assertThat(listener.onDisconnected).isEqualTo(connection);

        FastShutdown.shutdown(client);
    }

    @Test
    public void listenerTestWithRemoval() throws Exception {

        final TestConnectionListener removedListener = new TestConnectionListener();
        final TestConnectionListener retainedListener = new TestConnectionListener();

        RedisClient client = RedisClient.create(resources, RedisURI.Builder.redis(host, port).build());
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

        FastShutdown.shutdown(client);

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

    @Test(expected = RedisCommandInterruptedException.class, timeout = 50)
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

        RedisClient client = RedisClient.create();
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
        FastShutdown.shutdown(client);
    }

    @Test
    public void testExceptionWithCause() throws Exception {
        RedisException e = new RedisException(new RuntimeException());
        assertThat(e).hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
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

    @Test
    public void clientEvents() throws Exception {

        RedisClient myClient = RedisClient.create(resources, RedisURI.Builder.redis(host, port).build());

        EventBus eventBus = client.getResources().eventBus();
        final TestSubscriber<Event> eventTestSubscriber = new TestSubscriber<Event>();

        Subscription subscribe = eventBus.get().subscribe(eventTestSubscriber);

        RedisAsyncConnection<String, String> async = client.connectAsync();
        async.set(key, value).get();
        async.close();

        WaitFor.waitOrTimeout(new Condition() {
            @Override
            public boolean isSatisfied() {
                return eventTestSubscriber.getOnNextEvents().size() >= 4;
            }

        }, Timeout.timeout(seconds(5)));

        subscribe.unsubscribe();
        List<Event> events = eventTestSubscriber.getOnNextEvents();
        assertThat(events).hasSize(4);

        assertThat(events.get(0)).isInstanceOf(ConnectedEvent.class);
        assertThat(events.get(1)).isInstanceOf(ConnectionActivatedEvent.class);
        assertThat(events.get(2)).isInstanceOf(DisconnectedEvent.class);
        assertThat(events.get(3)).isInstanceOf(ConnectionDeactivatedEvent.class);

        myClient.shutdown();

    }

}
