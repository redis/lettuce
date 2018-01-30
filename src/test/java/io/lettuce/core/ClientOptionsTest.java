/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static io.lettuce.ConnectionTestUtil.getChannel;
import static io.lettuce.ConnectionTestUtil.getConnectionWatchdog;
import static io.lettuce.ConnectionTestUtil.getStack;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import io.lettuce.Wait;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.netty.channel.Channel;

/**
 * @author Mark Paluch
 */
public class ClientOptionsTest extends AbstractRedisClientTest {

    @Test
    public void testNew() {
        checkAssertions(ClientOptions.create());
    }

    @Test
    public void testBuilder() {
        checkAssertions(ClientOptions.builder().build());
    }

    @Test
    public void testCopy() {
        checkAssertions(ClientOptions.copyOf(ClientOptions.builder().build()));
    }

    protected void checkAssertions(ClientOptions sut) {
        assertThat(sut.isAutoReconnect()).isEqualTo(true);
        assertThat(sut.isCancelCommandsOnReconnectFailure()).isEqualTo(false);
        assertThat(sut.isPingBeforeActivateConnection()).isEqualTo(false);
        assertThat(sut.isSuspendReconnectOnProtocolFailure()).isEqualTo(false);
        assertThat(sut.getDisconnectedBehavior()).isEqualTo(ClientOptions.DisconnectedBehavior.DEFAULT);
    }

    @Test
    public void variousClientOptions() {

        StatefulRedisConnection<String, String> connection1 = client.connect();

        assertThat(connection1.getOptions().isAutoReconnect()).isTrue();
        connection1.close();

        client.setOptions(ClientOptions.builder().autoReconnect(false).build());

        StatefulRedisConnection<String, String> connection2 = client.connect();
        assertThat(connection2.getOptions().isAutoReconnect()).isFalse();

        assertThat(connection1.getOptions().isAutoReconnect()).isTrue();

        connection1.close();
        connection2.close();
    }

    @Test
    public void requestQueueSize() {

        client.setOptions(ClientOptions.builder().requestQueueSize(10).build());

        RedisAsyncCommands<String, String> connection = client.connect().async();
        getConnectionWatchdog(connection.getStatefulConnection()).setListenOnChannelInactive(false);

        connection.quit();

        Wait.untilTrue(() -> !connection.getStatefulConnection().isOpen()).waitOrTimeout();

        for (int i = 0; i < 10; i++) {
            connection.ping();
        }

        try {
            connection.ping();
            fail("missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasMessageContaining("Request queue size exceeded");
        }

        connection.getStatefulConnection().close();
    }

    @Test
    public void requestQueueSizeAppliedForReconnect() {

        client.setOptions(ClientOptions.builder().requestQueueSize(10).build());

        RedisAsyncCommands<String, String> connection = client.connect().async();
        ConnectionWatchdog watchdog = getConnectionWatchdog(connection.getStatefulConnection());

        watchdog.setListenOnChannelInactive(false);

        connection.quit();

        Wait.untilTrue(() -> !connection.getStatefulConnection().isOpen()).waitOrTimeout();

        List<RedisFuture<String>> pings = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            pings.add(connection.ping());
        }

        watchdog.setListenOnChannelInactive(true);
        watchdog.scheduleReconnect();

        for (RedisFuture<String> ping : pings) {
            assertThat(ping.toCompletableFuture().join()).isEqualTo("PONG");
        }

        connection.getStatefulConnection().close();
    }

    @Test
    public void requestQueueSizeOvercommittedReconnect() throws Exception {

        client.setOptions(ClientOptions.builder().requestQueueSize(10).build());

        StatefulRedisConnection<String, String> connection = client.connect();
        ConnectionWatchdog watchdog = getConnectionWatchdog(connection);

        watchdog.setListenOnChannelInactive(false);

        Queue<Object> buffer = getStack(connection);
        List<RedisFuture<String>> pings = new ArrayList<>();
        for (int i = 0; i < 11; i++) {

            AsyncCommand<String, String, String> command = new AsyncCommand<>(new Command<>(CommandType.PING,
                    new StatusOutput<>(StringCodec.UTF8)));
            pings.add(command);
            buffer.add(command);
        }

        getChannel(connection).disconnect();

        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

        watchdog.setListenOnChannelInactive(true);
        watchdog.scheduleReconnect();

        for (int i = 0; i < 10; i++) {
            assertThat(pings.get(i).get()).isEqualTo("PONG");
        }

        assertThatThrownBy(() -> pings.get(10).toCompletableFuture().join()).hasCauseInstanceOf(IllegalStateException.class)
                .hasMessage("java.lang.IllegalStateException: Queue full");

        connection.close();
    }

    @Test
    public void disconnectedWithoutReconnect() {

        client.setOptions(ClientOptions.builder().autoReconnect(false).build());

        RedisAsyncCommands<String, String> connection = client.connect().async();

        connection.quit();
        Wait.untilTrue(() -> !connection.getStatefulConnection().isOpen()).waitOrTimeout();
        try {
            connection.get(key);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("not connected");
        } finally {
            connection.getStatefulConnection().close();
        }
    }

    @Test
    public void disconnectedRejectCommands() {

        client.setOptions(ClientOptions.builder().disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build());

        RedisAsyncCommands<String, String> connection = client.connect().async();

        getConnectionWatchdog(connection.getStatefulConnection()).setListenOnChannelInactive(false);
        connection.quit();
        Wait.untilTrue(() -> !connection.getStatefulConnection().isOpen()).waitOrTimeout();
        try {
            connection.get(key);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("not connected");
        } finally {
            connection.getStatefulConnection().close();
        }
    }

    @Test
    public void disconnectedAcceptCommands() {

        client.setOptions(ClientOptions.builder().autoReconnect(false)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS).build());

        RedisAsyncCommands<String, String> connection = client.connect().async();

        connection.quit();
        Wait.untilTrue(() -> !connection.getStatefulConnection().isOpen()).waitOrTimeout();
        connection.get(key);
        connection.getStatefulConnection().close();
    }

    @Test(timeout = 10000)
    public void pingBeforeConnect() {

        redis.set(key, value);
        client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
        RedisCommands<String, String> connection = client.connect().sync();

        try {
            String result = connection.get(key);
            assertThat(result).isEqualTo(value);
        } finally {
            connection.getStatefulConnection().close();
        }
    }

    @Test(timeout = 2000)
    public void pingBeforeConnectTimeout() throws Exception {

        client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

        try (ServerSocket serverSocket = new ServerSocket(0)) {

            RedisURI redisURI = RedisURI.builder().redis(TestSettings.host(), serverSocket.getLocalPort())
                    .withTimeout(500, TimeUnit.MILLISECONDS).build();

            try {
                client.connect(redisURI);
                fail("Missing RedisConnectionException");
            } catch (RedisException e) {
                assertThat(e).isInstanceOf(RedisConnectionException.class).hasRootCauseInstanceOf(
                        RedisCommandTimeoutException.class);
            }
        }
    }

    @Test
    public void pingBeforeConnectWithAuthentication() {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) {

                client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = RedisURI.Builder.redis(host, port).withPassword(passwd).build();

                RedisCommands<String, String> connection = client.connect(redisURI).sync();

                try {
                    String result = connection.info();
                    assertThat(result).contains("memory");
                } finally {
                    connection.getStatefulConnection().close();
                }

            }
        };
    }

    @Test(timeout = 2000)
    public void pingBeforeConnectWithAuthenticationTimeout() {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) throws Exception {

                client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

                try (ServerSocket serverSocket = new ServerSocket(0)) {

                    RedisURI redisURI = RedisURI.builder().redis(TestSettings.host(), serverSocket.getLocalPort())
                            .withPassword(passwd).withTimeout(Duration.ofMillis(500)).build();

                    try {
                        client.connect(redisURI);
                        fail("Missing RedisConnectionException");
                    } catch (RedisException e) {
                        assertThat(e).isInstanceOf(RedisConnectionException.class).hasRootCauseInstanceOf(
                                RedisCommandTimeoutException.class);
                    }
                }
            }
        };
    }

    @Test
    public void pingBeforeConnectWithSslAndAuthentication() {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) {

                client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = RedisURI.Builder.redis(host, 6443).withPassword(passwd).withVerifyPeer(false).withSsl(true)
                        .build();

                RedisCommands<String, String> connection = client.connect(redisURI).sync();

                try {
                    String result = connection.info();
                    assertThat(result).contains("memory");
                } finally {
                    connection.getStatefulConnection().close();
                }

            }
        };
    }

    @Test
    public void pingBeforeConnectWithAuthenticationFails() {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) {

                client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = RedisURI.builder().redis(host, port).build();

                try {
                    client.connect(redisURI);
                    fail("Missing RedisConnectionException");
                } catch (RedisException e) {
                    assertThat(e).isInstanceOf(RedisConnectionException.class);
                }
            }
        };
    }

    @Test
    public void pingBeforeConnectWithSslAndAuthenticationFails() {

        new WithPasswordRequired() {
            @Override
            protected void run(RedisClient client) {

                client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());
                RedisURI redisURI = RedisURI.builder().redis(host, 6443).withVerifyPeer(false).withSsl(true).build();

                try {
                    client.connect(redisURI);
                    fail("Missing RedisConnectionException");
                } catch (RedisException e) {
                    assertThat(e).isInstanceOf(RedisConnectionException.class).hasRootCauseInstanceOf(
                            RedisCommandExecutionException.class);
                }
            }
        };
    }

    @Test
    public void appliesCommandTimeoutToAsyncCommands() {

        client.setOptions(ClientOptions.builder().timeoutOptions(TimeoutOptions.enabled()).build());

        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            connection.setTimeout(Duration.ofMillis(100));

            connection.async().clientPause(300);

            RedisFuture<String> future = connection.async().ping();

            assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RedisCommandTimeoutException.class).hasMessageContaining("100 milli");
        }
    }

    @Test
    public void appliesCommandTimeoutToReactiveCommands() {

        client.setOptions(ClientOptions.builder().timeoutOptions(TimeoutOptions.enabled()).build());

        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            connection.setTimeout(Duration.ofMillis(100));

            connection.async().clientPause(300);

            Mono<String> mono = connection.reactive().ping();

            StepVerifier.create(mono).expectError(RedisCommandTimeoutException.class).verify();
        }
    }

    @Test
    public void timeoutExpiresBatchedCommands() {

        client.setOptions(ClientOptions.builder()
                .timeoutOptions(TimeoutOptions.builder().fixedTimeout(Duration.ofMillis(1)).build()).build());

        try (StatefulRedisConnection<String, String> connection = client.connect()) {

            connection.setAutoFlushCommands(false);
            RedisFuture<String> future = connection.async().ping();
            Wait.untilTrue(future::isDone).waitOrTimeout();

            assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RedisCommandTimeoutException.class).hasMessageContaining("1 milli");

            connection.flushCommands();
        }
    }

    @Test(timeout = 10000)
    public void pingBeforeConnectWithQueuedCommandsAndReconnect() throws Exception {

        StatefulRedisConnection<String, String> controlConnection = client.connect();

        client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

        StatefulRedisConnection<String, String> redisConnection = client.connect(RedisURI.create("redis://localhost:6479/5"));
        redisConnection.async().set("key1", "value1");
        redisConnection.async().set("key2", "value2");

        RedisFuture<String> sleep = (RedisFuture<String>) controlConnection.dispatch(new AsyncCommand<>(new Command<>(
                CommandType.DEBUG, new StatusOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).add("SLEEP")
                        .add(2))));

        sleep.await(100, TimeUnit.MILLISECONDS);

        Channel channel = getChannel(redisConnection);
        ConnectionWatchdog connectionWatchdog = getConnectionWatchdog(redisConnection);
        connectionWatchdog.setReconnectSuspended(true);

        channel.close().get();
        sleep.get();

        redisConnection.async().get(key).cancel(true);

        RedisFuture<String> getFuture1 = redisConnection.async().get("key1");
        RedisFuture<String> getFuture2 = redisConnection.async().get("key2");
        getFuture1.await(100, TimeUnit.MILLISECONDS);

        connectionWatchdog.setReconnectSuspended(false);
        connectionWatchdog.scheduleReconnect();

        assertThat(getFuture1.get()).isEqualTo("value1");
        assertThat(getFuture2.get()).isEqualTo("value2");

        controlConnection.close();
        redisConnection.close();
    }

    @Test(timeout = 10000)
    public void authenticatedPingBeforeConnectWithQueuedCommandsAndReconnect() {

        new WithPasswordRequired() {

            @Override
            protected void run(RedisClient client) throws Exception {

                RedisURI redisURI = RedisURI.Builder.redis(host, port).withPassword(passwd).withDatabase(5).build();
                StatefulRedisConnection<String, String> controlConnection = client.connect(redisURI);

                client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true).build());

                StatefulRedisConnection<String, String> redisConnection = client.connect(redisURI);
                redisConnection.async().set("key1", "value1");
                redisConnection.async().set("key2", "value2");

                RedisFuture<String> sleep = (RedisFuture<String>) controlConnection.dispatch(new AsyncCommand<>(new Command<>(
                        CommandType.DEBUG, new StatusOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).add(
                                "SLEEP").add(2))));

                sleep.await(100, TimeUnit.MILLISECONDS);

                Channel channel = getChannel(redisConnection);
                ConnectionWatchdog connectionWatchdog = getConnectionWatchdog(redisConnection);
                connectionWatchdog.setReconnectSuspended(true);

                channel.close().get();
                sleep.get();

                redisConnection.async().get(key).cancel(true);

                RedisFuture<String> getFuture1 = redisConnection.async().get("key1");
                RedisFuture<String> getFuture2 = redisConnection.async().get("key2");
                getFuture1.await(100, TimeUnit.MILLISECONDS);

                connectionWatchdog.setReconnectSuspended(false);
                connectionWatchdog.scheduleReconnect();

                assertThat(getFuture1.get()).isEqualTo("value1");
                assertThat(getFuture2.get()).isEqualTo("value2");

                controlConnection.close();
                redisConnection.close();
            }
        };

    }
}
