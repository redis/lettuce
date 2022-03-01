/*
 * Copyright 2011-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionEvents;
import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.NettyCustomizer;
import io.lettuce.test.ConnectionTestUtil;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.server.RandomResponseServer;
import io.lettuce.test.settings.TestSettings;
import io.netty.channel.Channel;
import io.netty.channel.local.LocalAddress;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionFailureIntegrationTests extends TestSupport {

    private final RedisClient client;
    private final RedisURI defaultRedisUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();

    @Inject
    ConnectionFailureIntegrationTests(RedisClient client) {
        this.client = client;
    }

    /**
     * Expect to run into Invalid first byte exception instead of timeout.
     *
     * @throws Exception
     */
    @Test
    void invalidFirstByte() throws Exception {

        client.setOptions(ClientOptions.builder().build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.nonexistentPort())
                .withTimeout(Duration.ofMinutes(10)).build();

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
    @Test
    void failOnReconnect() throws Exception {

        ClientOptions clientOptions = ClientOptions.builder().suspendReconnectOnProtocolFailure(true).build();
        client.setOptions(clientOptions);

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();
        redisUri.setTimeout(Duration.ofSeconds(5));

        try {

            StatefulRedisConnection<String, String> connection = client.connect(redisUri);
            RedisAsyncCommands<String, String> async = connection.async();
            ConnectionWatchdog connectionWatchdog = ConnectionTestUtil
                    .getConnectionWatchdog(connection);

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();
            assertThat(connectionWatchdog.isReconnectSuspended()).isFalse();
            assertThat(clientOptions.isSuspendReconnectOnProtocolFailure()).isTrue();
            assertThat(connectionWatchdog.getReconnectionHandler().getClientOptions()).isSameAs(clientOptions);

            redisUri.setPort(TestSettings.nonexistentPort());

            async.quit();
            Wait.untilTrue(() -> connectionWatchdog.isReconnectSuspended()).waitOrTimeout();

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();

            assertThatThrownBy(() -> TestFutures.awaitOrTimeout(async.info())).hasRootCauseInstanceOf(RedisException.class)
                    .hasMessageContaining("Invalid first byte");

            connection.close();
        } finally {
            ts.shutdown();
        }
    }

    /**
     * Simulates a failure on reconnect by changing the port to a invalid server and triggering a reconnect.
     *
     * Expectation: {@link io.lettuce.core.ConnectionEvents.Reconnect} events are sent.
     *
     * @throws Exception
     */
    @Test
    void failOnReconnectShouldSendEvents() throws Exception {

        client.setOptions(
                ClientOptions.builder().suspendReconnectOnProtocolFailure(false).build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = RedisURI.create(defaultRedisUri.toURI());
        redisUri.setTimeout(Duration.ofSeconds(5));

        try {
            final BlockingQueue<ConnectionEvents.Reconnect> events = new LinkedBlockingDeque<>();

            StatefulRedisConnection<String, String> connection = client.connect(redisUri);
            RedisAsyncCommands<String, String> async = connection.async();
            ConnectionWatchdog connectionWatchdog = ConnectionTestUtil
                    .getConnectionWatchdog(connection);

            ReconnectionListener reconnectionListener = events::offer;

            Field field = ConnectionWatchdog.class.getDeclaredField("reconnectionListener");
            field.setAccessible(true);
            field.set(connectionWatchdog, reconnectionListener);

            redisUri.setPort(TestSettings.nonexistentPort());

            async.quit();
            Wait.untilTrue(() -> events.size() > 1).waitOrTimeout();
            connection.close();

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
    @Test
    void cancelCommandsOnReconnectFailure() throws Exception {

        client.setOptions(
                ClientOptions.builder().cancelCommandsOnReconnectFailure(true).build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = RedisURI.create(defaultRedisUri.toURI());

        try {
            RedisAsyncCommandsImpl<String, String> connection = (RedisAsyncCommandsImpl<String, String>) client
                    .connect(redisUri).async();
            ConnectionWatchdog connectionWatchdog = ConnectionTestUtil
                    .getConnectionWatchdog(connection.getStatefulConnection());

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
            Delay.delay(Duration.ofMillis(500));
            assertThat(connection.getStatefulConnection().isOpen()).isFalse();

            assertThatThrownBy(set1::get).isInstanceOf(CancellationException.class).hasNoCause();
            assertThatThrownBy(set2::get).isInstanceOf(CancellationException.class).hasNoCause();

            assertThatThrownBy(() -> TestFutures.awaitOrTimeout(connection.info())).isInstanceOf(RedisException.class)
                    .hasMessageContaining("Invalid first byte");

            connection.getStatefulConnection().close();
        } finally {
            ts.shutdown();
        }
    }

    @Test
    void emitEventOnReconnectFailure() throws Exception {

        RandomResponseServer ts = getRandomResponseServer();
        Queue<Event> queue = new ConcurrentLinkedQueue<>();
        ClientResources clientResources = ClientResources.create();

        RedisURI redisUri = RedisURI.create(defaultRedisUri.toURI());
        RedisClient client = RedisClient.create(clientResources);

        client.setOptions(ClientOptions.builder().build());

        try {
            StatefulRedisConnection<String, String> connection = client.connect(redisUri);
            RedisAsyncCommandsImpl<String, String> async = (RedisAsyncCommandsImpl<String, String>) connection.async();
            ConnectionWatchdog connectionWatchdog = ConnectionTestUtil
                    .getConnectionWatchdog(connection);

            redisUri.setPort(TestSettings.nonexistentPort());

            client.getResources().eventBus().get().subscribe(queue::add);

            async.quit();
            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

            connectionWatchdog.run(0);
            Delay.delay(Duration.ofMillis(500));

            connection.close();

            assertThat(queue).isNotEmpty();

            List<ReconnectFailedEvent> failures = queue.stream().filter(ReconnectFailedEvent.class::isInstance)
                    .map(ReconnectFailedEvent.class::cast).sorted(Comparator.comparingInt(ReconnectFailedEvent::getAttempt))
                    .collect(Collectors.toList());

            assertThat(failures.size()).isGreaterThanOrEqualTo(2);

            ReconnectFailedEvent failure1 = failures.get(0);
            assertThat(failure1.localAddress()).isEqualTo(LocalAddress.ANY);
            assertThat(failure1.remoteAddress()).isInstanceOf(InetSocketAddress.class);
            assertThat(failure1.getCause()).hasMessageContaining("Invalid first byte");
            assertThat(failure1.getAttempt()).isZero();

            ReconnectFailedEvent failure2 = failures.get(1);
            assertThat(failure2.localAddress()).isEqualTo(LocalAddress.ANY);
            assertThat(failure2.remoteAddress()).isInstanceOf(InetSocketAddress.class);
            assertThat(failure2.getCause()).hasMessageContaining("Invalid first byte");
            assertThat(failure2.getAttempt()).isOne();

        } finally {
            ts.shutdown();
            FastShutdown.shutdown(client);
            FastShutdown.shutdown(clientResources);
        }
    }

    @Test
    void pingOnConnectFailureShouldCloseConnection() throws Exception {

        AtomicReference<Channel> ref = new AtomicReference<>();
        ClientResources clientResources = ClientResources.builder().nettyCustomizer(new NettyCustomizer() {
            @Override
            public void afterChannelInitialized(Channel channel) {
                ref.set(channel);
            }
        }).build();

        // Cluster node with auth
        RedisURI redisUri = RedisURI.create(TestSettings.host(), 7385);
        RedisClient client = RedisClient.create(clientResources);

        client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

        try {
            client.connect(redisUri);
            fail("Missing Exception");
        } catch (Exception e) {
            Wait.untilEquals(false, ref.get()::isRegistered).waitOrTimeout();
            assertThat(ref.get().isOpen()).isFalse();
            Wait.untilEquals(false, () -> ref.get().isRegistered()).waitOrTimeout();
        } finally {
            FastShutdown.shutdown(client);
            FastShutdown.shutdown(clientResources);
        }
    }

    @Test
    void pingOnConnectFailureShouldCloseConnectionOnReconnect() throws Exception {

        BlockingQueue<Channel> ref = new LinkedBlockingQueue<>();
        ClientResources clientResources = ClientResources.builder().nettyCustomizer(new NettyCustomizer() {
            @Override
            public void afterChannelInitialized(Channel channel) {
                ref.add(channel);
            }
        }).build();

        RedisURI redisUri = RedisURI.create(TestSettings.host(), TestSettings.port());
        RedisClient client = RedisClient.create(clientResources, redisUri);
        client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

        StatefulRedisConnection<String, String> connection = client.connect();

        ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection);
        connectionWatchdog.setListenOnChannelInactive(false);
        connection.async().quit();

        // Cluster node with auth
        redisUri.setPort(7385);

        connectionWatchdog.setListenOnChannelInactive(true);
        connectionWatchdog.scheduleReconnect();

        Wait.untilTrue(() -> ref.size() > 1).waitOrTimeout();

        redisUri.setPort(TestSettings.port());

        Channel initial = ref.take();
        assertThat(initial.isOpen()).isFalse();

        Channel reconnect = ref.take();
        Wait.untilTrue(() -> !reconnect.isOpen()).waitOrTimeout();
        assertThat(reconnect.isOpen()).isFalse();

        FastShutdown.shutdown(client);
        FastShutdown.shutdown(clientResources);
    }

    /**
     * Expect to disable {@link ConnectionWatchdog} when closing a broken connection.
     */
    @Test
    void closingDisconnectedConnectionShouldDisableConnectionWatchdog() {

        client.setOptions(ClientOptions.create());

        RedisURI redisUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).withTimeout(Duration.ofMinutes(10))
                .build();

        StatefulRedisConnection<String, String> connection = client.connect(redisUri);

        ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection);

        assertThat(connectionWatchdog.isReconnectSuspended()).isFalse();
        assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();

        connection.sync().ping();

        redisUri.setPort(TestSettings.nonexistentPort() + 5);

        connection.async().quit();
        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

        connection.close();
        Delay.delay(Duration.ofMillis(100));

        assertThat(connectionWatchdog.isReconnectSuspended()).isTrue();
        assertThat(connectionWatchdog.isListenOnChannelInactive()).isFalse();
    }

    RandomResponseServer getRandomResponseServer() throws InterruptedException {
        RandomResponseServer ts = new RandomResponseServer();
        ts.initialize(TestSettings.nonexistentPort());
        return ts;
    }
}
