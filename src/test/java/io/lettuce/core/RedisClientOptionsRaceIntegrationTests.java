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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandExpiryWriter;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.PushHandler;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnectionImpl;
import io.lettuce.core.sentinel.RedisSentinelReactiveCommandsImpl;
import io.lettuce.core.sentinel.StatefulRedisSentinelConnectionImpl;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import io.netty.channel.Channel;
import reactor.core.publisher.Mono;

@Tag(INTEGRATION_TEST)
class RedisClientOptionsRaceIntegrationTests {

    private static final ClientOptions FIRST = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2)
            .autoReconnect(false).requestQueueSize(10).timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5))).build();

    private static final ClientOptions SECOND = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
            .autoReconnect(true).requestQueueSize(20)
            .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS)
            .jsonParser(() -> ClientOptions.DEFAULT_JSON_PARSER.get()).build();

    static Stream<Arguments> connectionStages() {
        return Stream.of(false, true).flatMap(explicitOptions -> Arrays.stream(ConnectionType.values())
                .flatMap(type -> Arrays.stream(Stage.values()).map(stage -> Arguments.of(type, stage, explicitOptions))));
    }

    @ParameterizedTest
    @MethodSource("connectionStages")
    void shouldKeepOptionsConsistentDuringConnectionCreation(ConnectionType type, Stage stage, boolean explicitOptions)
            throws Exception {

        PausingRedisClient client = explicitOptions ? new OptionsAwareRedisClient(stage) : new PausingRedisClient(stage);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        client.setOptions(FIRST);

        try {
            CompletableFuture<RedisChannelHandler<String, String>> pending = CompletableFuture
                    .supplyAsync(() -> connect(client, type, uri()), executor).thenCompose(future -> future);

            assertThat(client.paused.await(10, SECONDS)).isTrue();
            client.setOptions(SECOND);
            assertThat(client.getOptions()).isSameAs(SECOND);
            client.resume.countDown();

            try (RedisChannelHandler<String, String> first = pending.get(10, SECONDS)) {
                assertConnectionOptions(first, FIRST);
            }

            // Reuse the initiating thread to catch a snapshot leaking into the next connection.
            try (RedisChannelHandler<String, String> second = CompletableFuture
                    .supplyAsync(() -> connect(client, type, uri()), executor).thenCompose(future -> future).get(10, SECONDS)) {
                assertConnectionOptions(second, SECOND);
            }

            assertThat(client.endpointCalls).isEqualTo(2);
            assertThat(client.connectionCalls).isEqualTo(2);
            assertThat(client.handshakeCalls).isEqualTo(2);
        } finally {
            client.resume.countDown();
            executor.shutdownNow();
            try {
                assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
            } finally {
                FastShutdown.shutdown(client);
            }
        }
    }

    @Test
    void sentinelRetryShouldRetainOptionsAcrossAsyncFailure() throws Exception {

        CompletableFuture<SocketAddress> firstAddress = new CompletableFuture<>();
        AtomicBoolean firstAttempt = new AtomicBoolean(true);
        RedisClient client = new RedisClient(null, uri()) {

            @Override
            protected Mono<SocketAddress> getSocketAddress(RedisURI redisURI) {
                return firstAttempt.compareAndSet(true, false) ? Mono.fromFuture(firstAddress)
                        : super.getSocketAddress(redisURI);
            }

        };
        RedisURI sentinels = RedisURI.Builder.sentinel(TestSettings.host(), TestSettings.port(), "mymaster")
                .withSentinel(TestSettings.host(), TestSettings.port()).build();
        client.setOptions(FIRST);

        try {
            CompletableFuture<RedisChannelHandler<String, String>> pending = connect(client, ConnectionType.SENTINEL,
                    sentinels);
            assertThat(pending).isNotDone();
            client.setOptions(SECOND);
            firstAddress.completeExceptionally(new IllegalStateException("First Sentinel unavailable"));

            try (RedisChannelHandler<String, String> connection = pending.get(10, SECONDS)) {
                assertConnectionOptions(connection, FIRST);
            }
            try (RedisChannelHandler<String, String> connection = connect(client, ConnectionType.SENTINEL, sentinels).get(10,
                    SECONDS)) {
                assertConnectionOptions(connection, SECOND);
            }
        } finally {
            firstAddress.cancel(false);
            FastShutdown.shutdown(client);
        }
    }

    static Stream<Arguments> sentinelConnections() {
        return Stream.of(ConnectionType.STANDALONE, ConnectionType.PUBSUB)
                .flatMap(type -> Stream.of(false, true).map(explicitOptions -> Arguments.of(type, explicitOptions)));
    }

    @ParameterizedTest
    @MethodSource("sentinelConnections")
    void shouldRetainOptionsDuringSentinelAddressLookup(ConnectionType type, boolean explicitOptions) throws Exception {

        SentinelLookupRedisClient client = new SentinelLookupRedisClient();
        ClientOptions firstOptions = FIRST.mutate().autoReconnect(true).build();
        client.setOptions(explicitOptions ? SECOND : firstOptions);
        RedisURI sentinel = RedisURI.Builder.sentinel(TestSettings.host(), TestSettings.port(), "mymaster").build();

        try {
            CompletableFuture<RedisChannelHandler<String, String>> pending;
            if (explicitOptions) {
                pending = (type == ConnectionType.STANDALONE ? client.connectAsync(StringCodec.UTF8, sentinel, firstOptions)
                        : client.connectPubSubAsync(StringCodec.UTF8, sentinel, firstOptions))
                                .thenApply(connection -> (RedisChannelHandler<String, String>) connection)
                                .toCompletableFuture();
            } else {
                pending = connect(client, type, sentinel);
            }

            client.lookupStarted.get(10, SECONDS);
            assertThat(pending).isNotDone();
            client.setOptions(SECOND);
            client.lookupReady.complete(true);

            try (RedisChannelHandler<String, String> connection = pending.get(10, SECONDS)) {
                assertConnectionOptions(connection, firstOptions);
                client.assertDiscoveryOptions(firstOptions, 1);

                RedisChannelWriter writer = connection.getChannelWriter();
                if (writer instanceof CommandExpiryWriter) {
                    writer = ((CommandExpiryWriter) writer).getDelegate();
                }
                Channel channel = ReflectionTestUtils.getField(writer, "channel");
                channel.close().sync();
                assertThat(((StatefulRedisConnection<String, String>) connection).async().ping().get(10, SECONDS))
                        .isEqualTo("PONG");
                assertConnectionOptions(connection, firstOptions);
                client.assertDiscoveryOptions(firstOptions, 2);
            }

            client.discoveryOptions.clear();
            client.discoveryProtocols.clear();
            try (RedisChannelHandler<String, String> connection = connect(client, type, sentinel).get(10, SECONDS)) {
                assertConnectionOptions(connection, SECOND);
                client.assertDiscoveryOptions(SECOND, 1);
            }
            assertThat(client.getOptions()).isSameAs(SECOND);
        } finally {
            client.lookupReady.complete(true);
            FastShutdown.shutdown(client);
        }
    }

    @Test
    void nestedConnectionShouldRestoreOuterOptions() throws Exception {

        AtomicBoolean firstAttempt = new AtomicBoolean(true);
        RedisClient client = new RedisClient(null, uri()) {

            @Override
            protected DefaultEndpoint createEndpoint() {
                if (firstAttempt.compareAndSet(true, false)) {
                    setOptions(SECOND);
                    try (StatefulRedisConnection<String, String> nested = connect()) {
                        assertConnectionOptions((RedisChannelHandler<String, String>) nested, SECOND);
                    }
                }
                return super.createEndpoint();
            }

        };
        client.setOptions(FIRST);

        try {
            try (RedisChannelHandler<String, String> outer = connect(client, ConnectionType.STANDALONE, uri()).get(10,
                    SECONDS)) {
                assertConnectionOptions(outer, FIRST);
            }
            assertThat(client.getOptions()).isSameAs(SECOND);
            assertFactoryOptions(client, SECOND);
        } finally {
            FastShutdown.shutdown(client);
        }
    }

    @Test
    void failedConnectionShouldClearOptionsSnapshot() throws Exception {

        AtomicBoolean firstAttempt = new AtomicBoolean(true);
        RedisClient client = new RedisClient(null, uri()) {

            @Override
            protected DefaultEndpoint createEndpoint() {
                if (firstAttempt.compareAndSet(true, false)) {
                    setOptions(SECOND);
                    throw new IllegalStateException("Endpoint creation failed");
                }
                return super.createEndpoint();
            }

        };
        client.setOptions(FIRST);

        try {
            assertThatThrownBy(() -> client.connectAsync(StringCodec.UTF8, uri())).isInstanceOf(IllegalStateException.class)
                    .hasMessage("Endpoint creation failed");
            assertFactoryOptions(client, SECOND);
            try (RedisChannelHandler<String, String> connection = connect(client, ConnectionType.STANDALONE, uri()).get(10,
                    SECONDS)) {
                assertConnectionOptions(connection, SECOND);
            }
        } finally {
            FastShutdown.shutdown(client);
        }
    }

    private static void assertFactoryOptions(RedisClient client, ClientOptions expected) {
        DefaultEndpoint endpoint = client.createEndpoint();
        try {
            assertOptions(ReflectionTestUtils.getField(endpoint, "clientOptions"), expected);
        } finally {
            endpoint.close();
        }
    }

    private static RedisURI uri() {
        return RedisURI.create(TestSettings.host(), TestSettings.port());
    }

    private static CompletableFuture<RedisChannelHandler<String, String>> connect(RedisClient client, ConnectionType type,
            RedisURI redisURI) {
        switch (type) {
            case STANDALONE:
                return client.connectAsync(StringCodec.UTF8, redisURI)
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection).toCompletableFuture();
            case PUBSUB:
                return client.connectPubSubAsync(StringCodec.UTF8, redisURI)
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection).toCompletableFuture();
            case SENTINEL:
                return client.connectSentinelAsync(StringCodec.UTF8, redisURI)
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection);
            default:
                throw new IllegalArgumentException("Unsupported connection type: " + type);
        }
    }

    private static void assertConnectionOptions(RedisChannelHandler<String, String> connection, ClientOptions expected) {

        assertOptions(connection.getOptions(), expected);
        RedisChannelWriter writer = connection.getChannelWriter();
        assertThat(writer instanceof CommandExpiryWriter).isEqualTo(expected.getTimeoutOptions().isTimeoutCommands());
        if (writer instanceof CommandExpiryWriter) {
            assertThat((Object) ReflectionTestUtils.getField(writer, "source"))
                    .isSameAs(expected.getTimeoutOptions().getSource());
            writer = ((CommandExpiryWriter) writer).getDelegate();
        }

        DefaultEndpoint endpoint = (DefaultEndpoint) writer;
        assertOptions(ReflectionTestUtils.getField(endpoint, "clientOptions"), expected);
        Channel channel = ReflectionTestUtils.getField(endpoint, "channel");
        assertOptions(ReflectionTestUtils.getField(channel.pipeline().get(CommandHandler.class), "clientOptions"), expected);

        ConnectionState state;
        if (connection instanceof StatefulRedisSentinelConnectionImpl) {
            StatefulRedisSentinelConnectionImpl<String, String> sentinel = (StatefulRedisSentinelConnectionImpl<String, String>) connection;
            state = sentinel.getConnectionState();
            assertThat(sentinel.sync().ping()).isEqualTo("PONG");
            assertThat((Object) ReflectionTestUtils.getField(sentinel.reactive(), "parser")).isSameAs(expected.getJsonParser());
        } else {
            StatefulRedisConnectionImpl<String, String> standalone = (StatefulRedisConnectionImpl<String, String>) connection;
            state = standalone.getConnectionState();
            assertThat(standalone.sync().ping()).isEqualTo("PONG");
            Object authHandler = ReflectionTestUtils.getField(standalone, "authHandler");
            assertThat(authHandler.getClass() == RedisAuthenticationHandler.class).isEqualTo(
                    expected.getReauthenticateBehaviour() == ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS);
            if (!(connection instanceof StatefulRedisPubSubConnectionImpl)) {
                assertThat((Object) ReflectionTestUtils.getField(standalone.async(), "parser"))
                        .isSameAs(expected.getJsonParser());
                assertThat((Object) ReflectionTestUtils.getField(standalone.reactive(), "parser"))
                        .isSameAs(expected.getJsonParser());
            }
        }
        assertThat(state.getNegotiatedProtocolVersion()).isEqualTo(expected.getConfiguredProtocolVersion());
    }

    private static void assertOptions(ClientOptions actual, ClientOptions expected) {
        assertThat(actual.getConfiguredProtocolVersion()).isEqualTo(expected.getConfiguredProtocolVersion());
        assertThat(actual.isAutoReconnect()).isEqualTo(expected.isAutoReconnect());
        assertThat(actual.getRequestQueueSize()).isEqualTo(expected.getRequestQueueSize());
        assertThat(actual.getTimeoutOptions()).isSameAs(expected.getTimeoutOptions());
        assertThat(actual.getJsonParser()).isSameAs(expected.getJsonParser());
    }

    enum ConnectionType {
        STANDALONE, PUBSUB, SENTINEL
    }

    enum Stage {
        BEFORE_ENDPOINT, ENDPOINT, CONNECTION
    }

    private static class SentinelLookupRedisClient extends RedisClient {

        final CompletableFuture<Boolean> lookupStarted = new CompletableFuture<>();

        final CompletableFuture<Boolean> lookupReady = new CompletableFuture<>();

        final List<ClientOptions> discoveryOptions = new CopyOnWriteArrayList<>();

        final List<ProtocolVersion> discoveryProtocols = new CopyOnWriteArrayList<>();

        SentinelLookupRedisClient() {
            super(null, uri());
        }

        @Override
        protected Mono<SocketAddress> getSocketAddress(RedisURI redisURI) {
            Mono<SocketAddress> address = super.getSocketAddress(redisURI);
            if (!redisURI.getSentinels().isEmpty()) {
                return Mono.defer(() -> {
                    lookupStarted.complete(true);
                    return Mono.fromFuture(lookupReady).then(address);
                });
            }
            return address;
        }

        @Override
        protected <K, V> StatefulRedisSentinelConnectionImpl<K, V> newStatefulRedisSentinelConnection(RedisChannelWriter writer,
                RedisCodec<K, V> codec, Duration timeout, ClientOptions clientOptions) {
            return new StatefulRedisSentinelConnectionImpl<K, V>(writer, codec, timeout, clientOptions.getJsonParser()) {

                @Override
                public RedisSentinelReactiveCommands<K, V> reactive() {
                    discoveryOptions.add(getOptions());
                    discoveryProtocols.add(getConnectionState().getNegotiatedProtocolVersion());
                    RedisChannelWriter endpoint = getChannelWriter();
                    if (endpoint instanceof CommandExpiryWriter) {
                        endpoint = ((CommandExpiryWriter) endpoint).getDelegate();
                    }
                    assertOptions(ReflectionTestUtils.getField(endpoint, "clientOptions"), getOptions());
                    Channel channel = ReflectionTestUtils.getField(endpoint, "channel");
                    assertOptions(ReflectionTestUtils.getField(channel.pipeline().get(CommandHandler.class), "clientOptions"),
                            getOptions());

                    // Only the Sentinel response is stubbed; discovery still performs a real Redis handshake.
                    return new RedisSentinelReactiveCommandsImpl<K, V>(this, codec, clientOptions.getJsonParser()) {

                        @Override
                        public Mono<SocketAddress> getMasterAddrByName(K key) {
                            return Mono.just(new InetSocketAddress(TestSettings.host(), TestSettings.port()));
                        }

                    };
                }

            };
        }

        void assertDiscoveryOptions(ClientOptions expected, int count) {
            assertThat(discoveryOptions).hasSize(count).allSatisfy(options -> assertOptions(options, expected));
            assertThat(discoveryProtocols).hasSize(count).containsOnly(expected.getConfiguredProtocolVersion());
        }

    }

    private static class PausingRedisClient extends RedisClient {

        final CountDownLatch paused = new CountDownLatch(1);

        final CountDownLatch resume = new CountDownLatch(1);

        final AtomicBoolean pause = new AtomicBoolean(true);

        final Stage stage;

        int endpointCalls;

        int connectionCalls;

        int handshakeCalls;

        PausingRedisClient(Stage stage) {
            super(null, uri());
            this.stage = stage;
        }

        void pauseAt(Stage currentStage) {
            if (stage == currentStage && pause.compareAndSet(true, false)) {
                paused.countDown();
                try {
                    assertThat(resume.await(10, SECONDS)).isTrue();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
        }

        @Override
        protected DefaultEndpoint createEndpoint() {
            pauseAt(Stage.BEFORE_ENDPOINT);
            DefaultEndpoint endpoint = super.createEndpoint();
            endpointCalls++;
            pauseAt(Stage.ENDPOINT);
            return endpoint;
        }

        @Override
        protected <K, V> PubSubEndpoint<K, V> createPubSubEndpoint() {
            pauseAt(Stage.BEFORE_ENDPOINT);
            PubSubEndpoint<K, V> endpoint = super.createPubSubEndpoint();
            endpointCalls++;
            pauseAt(Stage.ENDPOINT);
            return endpoint;
        }

        @Override
        protected <K, V> StatefulRedisConnectionImpl<K, V> newStatefulRedisConnection(RedisChannelWriter writer,
                PushHandler pushHandler, RedisCodec<K, V> codec, Duration timeout) {
            StatefulRedisConnectionImpl<K, V> connection = super.newStatefulRedisConnection(writer, pushHandler, codec,
                    timeout);
            connectionCalls++;
            pauseAt(Stage.CONNECTION);
            return connection;
        }

        @Override
        protected <K, V> StatefulRedisPubSubConnectionImpl<K, V> newStatefulRedisPubSubConnection(PubSubEndpoint<K, V> endpoint,
                RedisChannelWriter writer, RedisCodec<K, V> codec, Duration timeout) {
            StatefulRedisPubSubConnectionImpl<K, V> connection = super.newStatefulRedisPubSubConnection(endpoint, writer, codec,
                    timeout);
            connectionCalls++;
            pauseAt(Stage.CONNECTION);
            return connection;
        }

        @Override
        protected <K, V> StatefulRedisSentinelConnectionImpl<K, V> newStatefulRedisSentinelConnection(RedisChannelWriter writer,
                RedisCodec<K, V> codec, Duration timeout) {
            StatefulRedisSentinelConnectionImpl<K, V> connection = super.newStatefulRedisSentinelConnection(writer, codec,
                    timeout);
            connectionCalls++;
            pauseAt(Stage.CONNECTION);
            return connection;
        }

        @Override
        protected RedisHandshake createHandshake(ConnectionState state) {
            handshakeCalls++;
            return super.createHandshake(state);
        }

    }

    private static class OptionsAwareRedisClient extends PausingRedisClient {

        OptionsAwareRedisClient(Stage stage) {
            super(stage);
        }

        @Override
        protected DefaultEndpoint createEndpoint(ClientOptions clientOptions) {
            pauseAt(Stage.BEFORE_ENDPOINT);
            DefaultEndpoint endpoint = new DefaultEndpoint(clientOptions, getResources());
            endpointCalls++;
            pauseAt(Stage.ENDPOINT);
            return endpoint;
        }

        @Override
        protected <K, V> PubSubEndpoint<K, V> createPubSubEndpoint(ClientOptions clientOptions) {
            pauseAt(Stage.BEFORE_ENDPOINT);
            PubSubEndpoint<K, V> endpoint = new PubSubEndpoint<>(clientOptions, getResources());
            endpointCalls++;
            pauseAt(Stage.ENDPOINT);
            return endpoint;
        }

        @Override
        protected <K, V> StatefulRedisConnectionImpl<K, V> newStatefulRedisConnection(RedisChannelWriter writer,
                PushHandler pushHandler, RedisCodec<K, V> codec, Duration timeout, ClientOptions clientOptions) {
            StatefulRedisConnectionImpl<K, V> connection = new StatefulRedisConnectionImpl<>(writer, pushHandler, codec,
                    timeout, clientOptions.getJsonParser());
            connectionCalls++;
            pauseAt(Stage.CONNECTION);
            return connection;
        }

        @Override
        protected <K, V> StatefulRedisPubSubConnectionImpl<K, V> newStatefulRedisPubSubConnection(PubSubEndpoint<K, V> endpoint,
                RedisChannelWriter writer, RedisCodec<K, V> codec, Duration timeout, ClientOptions clientOptions) {
            StatefulRedisPubSubConnectionImpl<K, V> connection = new StatefulRedisPubSubConnectionImpl<>(endpoint, writer,
                    codec, timeout);
            connectionCalls++;
            pauseAt(Stage.CONNECTION);
            return connection;
        }

        @Override
        protected <K, V> StatefulRedisSentinelConnectionImpl<K, V> newStatefulRedisSentinelConnection(RedisChannelWriter writer,
                RedisCodec<K, V> codec, Duration timeout, ClientOptions clientOptions) {
            StatefulRedisSentinelConnectionImpl<K, V> connection = new StatefulRedisSentinelConnectionImpl<>(writer, codec,
                    timeout, clientOptions.getJsonParser());
            connectionCalls++;
            pauseAt(Stage.CONNECTION);
            return connection;
        }

        @Override
        protected RedisHandshake createHandshake(ConnectionState state, ClientOptions clientOptions) {
            handshakeCalls++;
            return new RedisHandshake(clientOptions.getConfiguredProtocolVersion(),
                    clientOptions.isPingBeforeActivateConnection(), state,
                    clientOptions.getMaintNotificationsConfig().maintNotificationsEnabled()
                            ? clientOptions.getMaintNotificationsConfig().getEndpointTypeSource()
                            : null);
        }

    }

}
