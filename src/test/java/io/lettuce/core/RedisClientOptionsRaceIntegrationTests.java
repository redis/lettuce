package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandExpiryWriter;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.PushHandler;
import io.lettuce.core.pubsub.PubSubEndpoint;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnectionImpl;
import io.lettuce.core.sentinel.SentinelTestSettings;
import io.lettuce.core.sentinel.StatefulRedisSentinelConnectionImpl;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;
import io.netty.channel.Channel;

@Tag(INTEGRATION_TEST)
class RedisClientOptionsRaceIntegrationTests {

    private static final ClientOptions FIRST = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2)
            .autoReconnect(false).requestQueueSize(10).timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5))).build();

    private static final ClientOptions SECOND = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
            .autoReconnect(true).requestQueueSize(20).timeoutOptions(TimeoutOptions.create())
            .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS)
            .jsonParser(ClientOptions.DEFAULT_JSON_PARSER::get).build();

    static Stream<Arguments> connectionStages() {
        return Arrays.stream(ConnectionType.values())
                .flatMap(type -> Arrays.stream(Stage.values()).map(stage -> Arguments.of(type, stage)));
    }

    @ParameterizedTest
    @MethodSource("connectionStages")
    void shouldKeepOptionsConsistentDuringConnectionCreation(ConnectionType type, Stage stage) throws Exception {

        PausingRedisClient client = new PausingRedisClient(stage);
        client.setOptions(FIRST);

        try {
            CompletableFuture<RedisChannelHandler<String, String>> pending = CompletableFuture
                    .supplyAsync(() -> connect(client, type)).thenCompose(future -> future);

            assertThat(client.paused.await(10, SECONDS)).isTrue();
            client.setOptions(SECOND);
            client.resume.countDown();

            try (RedisChannelHandler<String, String> first = pending.get(10, SECONDS)) {
                assertConnectionOptions(first, FIRST);
            }

            try (RedisChannelHandler<String, String> second = connect(client, type).get(10, SECONDS)) {
                assertConnectionOptions(second, SECOND);
            }
        } finally {
            client.resume.countDown();
            FastShutdown.shutdown(client);
        }
    }

    @ParameterizedTest
    @EnumSource(ConnectionType.class)
    void shouldUseOptionsOfCustomEndpoint(ConnectionType type) throws Exception {

        RedisClient client = new RedisClient(TestClientResources.get(), uri()) {

            @Override
            protected DefaultEndpoint createEndpoint() {
                return new DefaultEndpoint(FIRST, getResources());
            }

            @Override
            protected <K, V> PubSubEndpoint<K, V> createPubSubEndpoint() {
                return new PubSubEndpoint<>(FIRST, getResources());
            }

        };
        client.setOptions(SECOND);

        try (RedisChannelHandler<String, String> connection = connect(client, type).get(10, SECONDS)) {
            assertConnectionOptions(connection, FIRST);
        } finally {
            FastShutdown.shutdown(client);
        }
    }

    private static RedisURI uri() {
        return RedisURI.create(TestSettings.host(), TestSettings.port());
    }

    private static CompletableFuture<RedisChannelHandler<String, String>> connect(RedisClient client, ConnectionType type) {
        switch (type) {
            case STANDALONE:
                return client.connectAsync(StringCodec.UTF8, uri())
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection).toCompletableFuture();
            case PUBSUB:
                return client.connectPubSubAsync(StringCodec.UTF8, uri())
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection).toCompletableFuture();
            case SENTINEL:
                return client.connectSentinelAsync(StringCodec.UTF8, SentinelTestSettings.SENTINEL_URI)
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection);
            default:
                throw new IllegalArgumentException("Unsupported connection type: " + type);
        }
    }

    private static void assertConnectionOptions(RedisChannelHandler<String, String> connection, ClientOptions expected) {

        RedisChannelWriter writer = connection.getChannelWriter();
        assertThat(writer instanceof CommandExpiryWriter).isEqualTo(expected.getTimeoutOptions().isTimeoutCommands());
        if (writer instanceof CommandExpiryWriter) {
            writer = ((CommandExpiryWriter) writer).getDelegate();
        }

        DefaultEndpoint endpoint = (DefaultEndpoint) writer;
        assertThat(endpoint.getClientOptions()).isSameAs(expected);

        Channel channel = ReflectionTestUtils.getField(endpoint, "channel");
        CommandHandler commandHandler = channel.pipeline().get(CommandHandler.class);
        assertThat((Object) ReflectionTestUtils.getField(commandHandler, "clientOptions")).isSameAs(expected);

        ConnectionState state;
        if (connection instanceof StatefulRedisSentinelConnectionImpl) {
            state = ((StatefulRedisSentinelConnectionImpl<String, String>) connection).getConnectionState();

            // Sentinel connections are configured with a copy of the options.
            ClientOptions copy = connection.getOptions();
            assertThat(copy.getConfiguredProtocolVersion()).isEqualTo(expected.getConfiguredProtocolVersion());
            assertThat(copy.isAutoReconnect()).isEqualTo(expected.isAutoReconnect());
            assertThat(copy.getRequestQueueSize()).isEqualTo(expected.getRequestQueueSize());
        } else {
            StatefulRedisConnectionImpl<String, String> standalone = (StatefulRedisConnectionImpl<String, String>) connection;
            state = standalone.getConnectionState();
            assertThat(connection.getOptions()).isSameAs(expected);

            boolean reauthenticate = expected
                    .getReauthenticateBehaviour() == ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS;
            Object authHandler = ReflectionTestUtils.getField(standalone, "authHandler");
            assertThat(authHandler.getClass() == RedisAuthenticationHandler.class).isEqualTo(reauthenticate);

            if (!(connection instanceof StatefulRedisPubSubConnectionImpl)) {
                assertThat((Object) ReflectionTestUtils.getField(standalone, "parser")).isSameAs(expected.getJsonParser());
            }
        }

        assertThat(state.getNegotiatedProtocolVersion()).isEqualTo(expected.getConfiguredProtocolVersion());
        assertThat(state.getClientOptions()).isSameAs(expected);
    }

    enum ConnectionType {
        STANDALONE, PUBSUB, SENTINEL
    }

    enum Stage {
        ENDPOINT, CONNECTION
    }

    private static class PausingRedisClient extends RedisClient {

        final CountDownLatch paused = new CountDownLatch(1);

        final CountDownLatch resume = new CountDownLatch(1);

        final Stage stage;

        PausingRedisClient(Stage stage) {
            super(TestClientResources.get(), uri());
            this.stage = stage;
        }

        void pauseAt(Stage currentStage) {
            if (stage == currentStage) {
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
            DefaultEndpoint endpoint = super.createEndpoint();
            pauseAt(Stage.ENDPOINT);
            return endpoint;
        }

        @Override
        protected <K, V> PubSubEndpoint<K, V> createPubSubEndpoint() {
            PubSubEndpoint<K, V> endpoint = super.createPubSubEndpoint();
            pauseAt(Stage.ENDPOINT);
            return endpoint;
        }

        @Override
        protected <K, V> StatefulRedisConnectionImpl<K, V> newStatefulRedisConnection(RedisChannelWriter writer,
                PushHandler pushHandler, RedisCodec<K, V> codec, Duration timeout) {
            StatefulRedisConnectionImpl<K, V> connection = super.newStatefulRedisConnection(writer, pushHandler, codec,
                    timeout);
            pauseAt(Stage.CONNECTION);
            return connection;
        }

        @Override
        protected <K, V> StatefulRedisPubSubConnectionImpl<K, V> newStatefulRedisPubSubConnection(PubSubEndpoint<K, V> endpoint,
                RedisChannelWriter writer, RedisCodec<K, V> codec, Duration timeout) {
            StatefulRedisPubSubConnectionImpl<K, V> connection = super.newStatefulRedisPubSubConnection(endpoint, writer, codec,
                    timeout);
            pauseAt(Stage.CONNECTION);
            return connection;
        }

        @Override
        protected <K, V> StatefulRedisSentinelConnectionImpl<K, V> newStatefulRedisSentinelConnection(RedisChannelWriter writer,
                RedisCodec<K, V> codec, Duration timeout) {
            StatefulRedisSentinelConnectionImpl<K, V> connection = super.newStatefulRedisSentinelConnection(writer, codec,
                    timeout);
            pauseAt(Stage.CONNECTION);
            return connection;
        }

    }

}
