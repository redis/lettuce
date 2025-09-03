/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.test.ConnectionTestUtil.getChannel;
import static io.lettuce.test.ConnectionTestUtil.getConnectionWatchdog;
import static io.lettuce.test.ConnectionTestUtil.getStack;
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

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.test.condition.EnabledOnCommand;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;
import io.lettuce.test.Wait;
import io.lettuce.test.WithPassword;
import io.lettuce.test.settings.TestSettings;
import io.netty.channel.Channel;

/**
 * Integration tests for effects configured via {@link ClientOptions}.
 *
 * @author Mark Paluch
 * @author Hari Mani
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class ClientOptionsIntegrationTests extends TestSupport {

    private static final TimeoutOptions DISABLE_COMMAND_TIMEOUT = TimeoutOptions.builder().timeoutCommands(false).build();

    private final RedisClient client;

    @Inject
    ClientOptionsIntegrationTests(RedisClient client) {
        this.client = client;
    }

    @Test
    void variousClientOptions() {

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
    void requestQueueSize() {
        client.setOptions(ClientOptions.builder().requestQueueSize(10).timeoutOptions(DISABLE_COMMAND_TIMEOUT).build());
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            getConnectionWatchdog(connection).setListenOnChannelInactive(false);
            final RedisAsyncCommands<String, String> async = connection.async();

            async.quit();

            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

            for (int i = 0; i < 10; i++) {
                connection.async().ping();
            }

            assertThatThrownBy(() -> connection.async().ping().toCompletableFuture().join())
                    .hasMessageContaining("Request queue size exceeded");
            assertThatThrownBy(() -> connection.sync().ping()).hasMessageContaining("Request queue size exceeded");
        }
    }

    @Test
    void requestQueueSizeAppliedForReconnect() {
        client.setOptions(ClientOptions.builder().requestQueueSize(10).timeoutOptions(DISABLE_COMMAND_TIMEOUT).build());
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            final RedisAsyncCommands<String, String> commands = connection.async();
            testHitRequestQueueLimit(commands, connection);
        }
    }

    @Test
    void testHitRequestQueueLimitReconnectWithAuthCommand() {
        WithPassword.run(client, () -> {
            client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).pingBeforeActivateConnection(false)
                    .requestQueueSize(10).timeoutOptions(DISABLE_COMMAND_TIMEOUT).build());
            try (StatefulRedisConnection<String, String> connection = client.connect()) {
                final RedisAsyncCommands<String, String> commands = connection.async();
                commands.auth(passwd);
                testHitRequestQueueLimit(commands, connection);
            }
        });
    }

    @Test
    @EnabledOnCommand("ACL")
    void testHitRequestQueueLimitReconnectWithAuthUsernamePasswordCommand() {
        WithPassword.run(client, () -> {
            client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).pingBeforeActivateConnection(false)
                    .requestQueueSize(10).timeoutOptions(DISABLE_COMMAND_TIMEOUT).build());
            try (StatefulRedisConnection<String, String> connection = client.connect()) {
                final RedisAsyncCommands<String, String> commands = connection.async();
                commands.auth(username, passwd);
                testHitRequestQueueLimit(commands, connection);
            }
        });
    }

    @Test
    void testHitRequestQueueLimitReconnectWithUriAuth() {
        WithPassword.run(client, () -> {
            client.setOptions(ClientOptions.builder().requestQueueSize(10).timeoutOptions(DISABLE_COMMAND_TIMEOUT).build());
            final RedisURI redisURI = RedisURI.create(host, port);
            redisURI.setAuthentication(passwd);
            try (StatefulRedisConnection<String, String> connection = client.connect(redisURI)) {
                final RedisAsyncCommands<String, String> commands = connection.async();
                testHitRequestQueueLimit(commands, connection);
            }
        });
    }

    @Test
    void testHitRequestQueueLimitReconnectWithUriAuthPingCommand() {
        WithPassword.run(client, () -> {
            client.setOptions(ClientOptions.builder().requestQueueSize(10).timeoutOptions(DISABLE_COMMAND_TIMEOUT).build());
            final RedisURI redisURI = RedisURI.create(host, port);
            redisURI.setAuthentication(passwd);
            try (StatefulRedisConnection<String, String> connection = client.connect(redisURI)) {
                final RedisAsyncCommands<String, String> commands = connection.async();
                testHitRequestQueueLimit(commands, connection);
            }
        });
    }

    private void testHitRequestQueueLimit(final RedisAsyncCommands<String, String> commands,
            final StatefulRedisConnection<String, String> connection) {
        ConnectionWatchdog watchdog = getConnectionWatchdog(connection);

        watchdog.setListenOnChannelInactive(false);

        commands.quit();

        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

        List<RedisFuture<String>> pings = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            pings.add(commands.ping());
        }

        watchdog.setListenOnChannelInactive(true);
        watchdog.scheduleReconnect();

        for (RedisFuture<String> ping : pings) {
            assertThat(TestFutures.getOrTimeout(ping)).isEqualTo("PONG");
        }
    }

    @Test
    void requestQueueSizeOvercommittedReconnect() {
        client.setOptions(ClientOptions.builder().requestQueueSize(10).timeoutOptions(DISABLE_COMMAND_TIMEOUT).build());

        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            ConnectionWatchdog watchdog = getConnectionWatchdog(connection);

            watchdog.setListenOnChannelInactive(false);

            Queue<Object> buffer = getStack(connection);
            List<RedisFuture<String>> pings = new ArrayList<>();
            for (int i = 0; i < 11; i++) {

                AsyncCommand<String, String, String> command = new AsyncCommand<>(
                        new Command<>(CommandType.PING, new StatusOutput<>(StringCodec.UTF8)));
                pings.add(command);
                buffer.add(command);
            }

            getChannel(connection).disconnect();

            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

            watchdog.setListenOnChannelInactive(true);
            watchdog.scheduleReconnect();

            for (int i = 0; i < 10; i++) {
                assertThat(TestFutures.getOrTimeout(pings.get(i))).isEqualTo("PONG");
            }

            assertThatThrownBy(() -> TestFutures.awaitOrTimeout(pings.get(10))).hasCauseInstanceOf(IllegalStateException.class)
                    .hasMessage("java.lang.IllegalStateException: Queue full");
        }
    }

    @Test
    void disconnectedWithoutReconnect() {
        client.setOptions(ClientOptions.builder().autoReconnect(false).build());
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisAsyncCommands<String, String> commands = connection.async();
            commands.quit();
            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();
            commands.get(key);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("not connected");
        }
    }

    @Test
    void disconnectedRejectCommands() {
        client.setOptions(ClientOptions.builder().disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .timeoutOptions(DISABLE_COMMAND_TIMEOUT).build());
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisAsyncCommands<String, String> commands = connection.async();
            getConnectionWatchdog(connection).setListenOnChannelInactive(false);
            commands.quit();
            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();
            commands.get(key);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisException.class).hasMessageContaining("not connected");
        }
    }

    @Test
    void disconnectedAcceptCommands() {
        client.setOptions(ClientOptions.builder().autoReconnect(false)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS).build());
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisAsyncCommands<String, String> commands = connection.async();
            commands.quit();
            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();
            commands.get(key);
        }
    }

    @Test
    @Inject
    void pingBeforeConnect(StatefulRedisConnection<String, String> sharedConnection) {
        sharedConnection.sync().set(key, value);
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisCommands<String, String> commands = connection.sync();
            String result = commands.get(key);
            assertThat(result).isEqualTo(value);
        }
    }

    @Test
    void connectTimeout() throws Exception {

        try (ServerSocket serverSocket = new ServerSocket(0)) {

            RedisURI redisURI = RedisURI.Builder.redis(TestSettings.host(), serverSocket.getLocalPort())
                    .withTimeout(Duration.ofMillis(500)).build();

            try {
                client.connect(redisURI);
                fail("Missing RedisConnectionException");
            } catch (RedisException e) {
                assertThat(e).isInstanceOf(RedisConnectionException.class)
                        .hasRootCauseInstanceOf(RedisCommandTimeoutException.class);
            }
        }
    }

    @Test
    void connectWithAuthentication() {
        WithPassword.run(client, () -> {
            RedisURI redisURI = RedisURI.Builder.redis(host, port).withPassword(passwd).build();
            try (StatefulRedisConnection<String, String> connection = client.connect(redisURI)) {
                final RedisCommands<String, String> commands = connection.sync();
                String result = commands.info();
                assertThat(result).contains("memory");
            }
        });
    }

    @Test
    void authenticationTimeout() {

        WithPassword.run(client, () -> {

            try (ServerSocket serverSocket = new ServerSocket(0)) {

                RedisURI redisURI = RedisURI.Builder.redis(TestSettings.host(), serverSocket.getLocalPort())
                        .withPassword(passwd).withTimeout(Duration.ofMillis(500)).build();

                try {
                    client.connect(redisURI);
                    fail("Missing RedisConnectionException");
                } catch (RedisException e) {
                    assertThat(e).isInstanceOf(RedisConnectionException.class)
                            .hasRootCauseInstanceOf(RedisCommandTimeoutException.class);
                }
            }
        });
    }

    @Test
    void sslAndAuthentication() {
        WithPassword.run(client, () -> {
            RedisURI redisURI = RedisURI.Builder.redis(host, 6443).withPassword(passwd).withVerifyPeer(false).withSsl(true)
                    .build();
            try (StatefulRedisConnection<String, String> connection = client.connect(redisURI)) {
                final RedisCommands<String, String> commands = connection.sync();
                String result = commands.info();
                assertThat(result).contains("memory");
            }
        });
    }

    @Test
    void authenticationFails() {

        WithPassword.run(client, () -> {

            RedisURI redisURI = RedisURI.Builder.redis(host, port).build();

            try {
                client.connect(redisURI);
                fail("Missing RedisConnectionException");
            } catch (RedisException e) {
                assertThat(e).isInstanceOf(RedisConnectionException.class);
            }
        });
    }

    @Test
    void pingBeforeConnectWithSslAndAuthenticationFails() {

        WithPassword.run(client, () -> {

            RedisURI redisURI = RedisURI.Builder.redis(host, 6443).withVerifyPeer(false).withSsl(true).build();

            try {
                client.connect(redisURI);
                fail("Missing RedisConnectionException");
            } catch (RedisException e) {
                assertThat(e).isInstanceOf(RedisConnectionException.class)
                        .hasRootCauseInstanceOf(RedisCommandExecutionException.class);
            }
        });
    }

    @Test
    void appliesCommandTimeoutToAsyncCommands() {

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
    void appliesCommandTimeoutToReactiveCommands() {

        client.setOptions(ClientOptions.builder().timeoutOptions(TimeoutOptions.enabled()).build());

        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            connection.setTimeout(Duration.ofMillis(100));

            connection.async().clientPause(300);

            Mono<String> mono = connection.reactive().ping();

            StepVerifier.create(mono).expectError(RedisCommandTimeoutException.class).verify();
        }
    }

    @Test
    void timeoutExpiresBatchedCommands() {

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

    @Test
    void pingBeforeConnectWithQueuedCommandsAndReconnect() throws Exception {
        client.setOptions(ClientOptions.builder().timeoutOptions(DISABLE_COMMAND_TIMEOUT).build());
        StatefulRedisConnection<String, String> controlConnection = client.connect();

        StatefulRedisConnection<String, String> redisConnection = client.connect(RedisURI.create("redis://localhost:6479/5"));
        redisConnection.async().set("key1", "value1");
        redisConnection.async().set("key2", "value2");

        RedisFuture<String> sleep = (RedisFuture<String>) controlConnection
                .dispatch(new AsyncCommand<>(new Command<>(CommandType.DEBUG, new StatusOutput<>(StringCodec.UTF8),
                        new CommandArgs<>(StringCodec.UTF8).add("SLEEP").add(2))));

        sleep.await(100, TimeUnit.MILLISECONDS);

        Channel channel = getChannel(redisConnection);
        ConnectionWatchdog connectionWatchdog = getConnectionWatchdog(redisConnection);
        connectionWatchdog.setReconnectSuspended(true);

        TestFutures.awaitOrTimeout(channel.close());
        TestFutures.awaitOrTimeout(sleep);

        redisConnection.async().get(key).cancel(true);

        RedisFuture<String> getFuture1 = redisConnection.async().get("key1");
        RedisFuture<String> getFuture2 = redisConnection.async().get("key2");
        getFuture1.await(100, TimeUnit.MILLISECONDS);

        connectionWatchdog.setReconnectSuspended(false);
        connectionWatchdog.scheduleReconnect();

        assertThat(TestFutures.getOrTimeout(getFuture1)).isEqualTo("value1");
        assertThat(TestFutures.getOrTimeout(getFuture2)).isEqualTo("value2");

        controlConnection.close();
        redisConnection.close();
    }

    @Test
    void authenticatedPingBeforeConnectWithQueuedCommandsAndReconnect() {

        WithPassword.run(client, () -> {

            RedisURI redisURI = RedisURI.Builder.redis(host, port).withPassword(passwd).withDatabase(5).build();
            client.setOptions(ClientOptions.builder().timeoutOptions(DISABLE_COMMAND_TIMEOUT).build());
            StatefulRedisConnection<String, String> controlConnection = client.connect(redisURI);

            StatefulRedisConnection<String, String> redisConnection = client.connect(redisURI);
            redisConnection.async().set("key1", "value1");
            redisConnection.async().set("key2", "value2");

            RedisFuture<String> sleep = (RedisFuture<String>) controlConnection
                    .dispatch(new AsyncCommand<>(new Command<>(CommandType.DEBUG, new StatusOutput<>(StringCodec.UTF8),
                            new CommandArgs<>(StringCodec.UTF8).add("SLEEP").add(2))));

            sleep.await(100, TimeUnit.MILLISECONDS);

            Channel channel = getChannel(redisConnection);
            ConnectionWatchdog connectionWatchdog = getConnectionWatchdog(redisConnection);
            connectionWatchdog.setReconnectSuspended(true);

            TestFutures.awaitOrTimeout(channel.close());
            TestFutures.awaitOrTimeout(sleep);

            redisConnection.async().get(key).cancel(true);

            RedisFuture<String> getFuture1 = redisConnection.async().get("key1");
            RedisFuture<String> getFuture2 = redisConnection.async().get("key2");
            getFuture1.await(100, TimeUnit.MILLISECONDS);

            connectionWatchdog.setReconnectSuspended(false);
            connectionWatchdog.scheduleReconnect();

            assertThat(TestFutures.getOrTimeout(getFuture1)).isEqualTo("value1");
            assertThat(TestFutures.getOrTimeout(getFuture2)).isEqualTo("value2");

            controlConnection.close();
            redisConnection.close();
        });
    }

}
