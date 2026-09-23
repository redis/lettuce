package io.lettuce.core.cluster;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionBuilder;
import io.lettuce.core.ConnectionEvents;
import io.lettuce.core.ConnectionState;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisAuthenticationHandler;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.protocol.CommandExpiryWriter;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.PushHandler;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import io.netty.channel.Channel;
import reactor.core.publisher.Mono;

@Tag(INTEGRATION_TEST)
class RedisClusterClientOptionsRaceIntegrationTests {

    private static final ClusterClientOptions FIRST = ClusterClientOptions.builder().protocolVersion(ProtocolVersion.RESP2)
            .autoReconnect(false).requestQueueSize(10).maxRedirects(2)
            .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5))).build();

    private static final ClusterClientOptions SECOND = secondOptions();

    private static ClusterClientOptions secondOptions() {
        ClusterClientOptions.Builder builder = ClusterClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
                .autoReconnect(true).requestQueueSize(20).maxRedirects(7)
                .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS);
        builder.jsonParser(() -> ClientOptions.DEFAULT_JSON_PARSER.get());
        return builder.build();
    }

    @ParameterizedTest
    @EnumSource(ConnectionType.class)
    void shouldKeepOptionsConsistentDuringConnectionCreation(ConnectionType type) throws Exception {
        assertConsistentOptions(type, Stage.BUILDER);
    }

    @ParameterizedTest
    @EnumSource(value = ConnectionType.class, names = { "CLUSTER", "NODE" })
    void shouldRetainOptionsAndFactoryOverrides(ConnectionType type) throws Exception {
        assertConsistentOptions(type, Stage.FACTORY);
    }

    private static void assertConsistentOptions(ConnectionType type, Stage stage) throws Exception {

        PausingRedisClusterClient client = new PausingRedisClusterClient(stage);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        client.setOptions(FIRST);

        try {
            CompletableFuture<RedisChannelHandler<String, String>> pending = CompletableFuture
                    .supplyAsync(() -> connect(client, type), executor).thenCompose(future -> future);

            assertThat(client.paused.await(10, SECONDS)).isTrue();
            client.setOptions(SECOND);
            assertThat(client.getOptions()).isSameAs(SECOND);
            client.resume.countDown();

            try (RedisChannelHandler<String, String> first = pending.get(10, SECONDS)) {
                assertConnectionOptions(first, FIRST);
            }

            try (RedisChannelHandler<String, String> second = CompletableFuture
                    .supplyAsync(() -> connect(client, type), executor).thenCompose(future -> future).get(10, SECONDS)) {
                assertConnectionOptions(second, SECOND);
            }

            if (type == ConnectionType.CLUSTER || type == ConnectionType.NODE) {
                assertThat(client.factoryOptions).containsExactly(FIRST, SECOND);
                assertThat(client.legacyFactoryParsers).containsExactly(FIRST.getJsonParser(), SECOND.getJsonParser());
            }
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

    @ParameterizedTest
    @EnumSource(value = ConnectionType.class, names = { "CLUSTER", "PUBSUB" })
    void retryShouldRetainOptionsAcrossAsyncFailure(ConnectionType type) throws Exception {

        CompletableFuture<SocketAddress> firstAddress = new CompletableFuture<>();
        AtomicBoolean firstAttempt = new AtomicBoolean(true);
        RedisURI uri = RedisURI.create(TestSettings.host(), TestSettings.port());
        RedisClusterClient client = new RedisClusterClient(null, Collections.singletonList(uri)) {

            @Override
            protected Mono<SocketAddress> getSocketAddressSupplier(Supplier<Partitions> partitionsSupplier,
                    Function<Partitions, Collection<RedisClusterNode>> sortFunction) {
                return Mono.defer(() -> firstAttempt.compareAndSet(true, false) ? Mono.fromFuture(firstAddress)
                        : Mono.just(new InetSocketAddress(TestSettings.host(), TestSettings.port())));
            }

        };
        Partitions partitions = new Partitions();
        for (String nodeId : new String[] { "first", "second" }) {
            partitions.add(new RedisClusterNode(uri, nodeId, true, null, 0, 0, 0, Collections.emptyList(),
                    Collections.singleton(RedisClusterNode.NodeFlag.UPSTREAM)));
        }
        client.setPartitions(partitions);
        client.setOptions(FIRST);

        try {
            CompletableFuture<RedisChannelHandler<String, String>> pending = connect(client, type);
            assertThat(pending).isNotDone();
            client.setOptions(SECOND);
            firstAddress.completeExceptionally(new IllegalStateException("First cluster node unavailable"));

            try (RedisChannelHandler<String, String> first = pending.get(10, SECONDS)) {
                assertConnectionOptions(first, FIRST);
            }
            try (RedisChannelHandler<String, String> second = connect(client, type).get(10, SECONDS)) {
                assertConnectionOptions(second, SECOND);
            }
        } finally {
            firstAddress.cancel(false);
            FastShutdown.shutdown(client);
        }
    }

    private static CompletableFuture<RedisChannelHandler<String, String>> connect(RedisClusterClient client,
            ConnectionType type) {

        Mono<SocketAddress> address = Mono.just(new InetSocketAddress(TestSettings.host(), TestSettings.port()));

        switch (type) {
            case CLUSTER:
                return client.connectAsync(StringCodec.UTF8)
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection);
            case PUBSUB:
                return client.connectPubSubAsync(StringCodec.UTF8)
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection);
            case NODE:
                return client.connectToNodeAsync(StringCodec.UTF8, "node", null, address)
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection).toCompletableFuture();
            case PUBSUB_NODE:
                return client.connectPubSubToNodeAsync(StringCodec.UTF8, "node", address)
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection).toCompletableFuture();
            default:
                throw new IllegalArgumentException("Unknown connection type: " + type);
        }
    }

    private static void assertConnectionOptions(RedisChannelHandler<String, String> connection, ClusterClientOptions expected) {

        assertOptions(connection.getOptions(), expected);
        RedisChannelWriter writer = connection.getChannelWriter();

        if (writer instanceof ClusterDistributionChannelWriter) {
            assertOptions(ReflectionTestUtils.getField(writer, "clientOptions"), expected);
            assertThat((int) ReflectionTestUtils.getField(writer, "executionLimit")).isEqualTo(expected.getMaxRedirects());
            ClusterConnectionProvider provider = ((ClusterDistributionChannelWriter) writer).getClusterConnectionProvider();
            assertOptions(ReflectionTestUtils.getField(provider, "options"), expected);
            writer = ReflectionTestUtils.getField(writer, "defaultWriter");
        }

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
        if (connection instanceof StatefulRedisClusterConnectionImpl) {
            StatefulRedisClusterConnectionImpl<String, String> cluster = (StatefulRedisClusterConnectionImpl<String, String>) connection;
            state = cluster.getConnectionState();
            assertThat(cluster.sync().ping()).isEqualTo("PONG");
            assertThat((Object) ReflectionTestUtils.getField(cluster.async(), "parser")).isSameAs(expected.getJsonParser());
        } else {
            StatefulRedisConnectionImpl<String, String> node = (StatefulRedisConnectionImpl<String, String>) connection;
            state = node.getConnectionState();
            assertThat(node.sync().ping()).isEqualTo("PONG");
            Object authHandler = ReflectionTestUtils.getField(node, "authHandler");
            assertThat(authHandler.getClass() == RedisAuthenticationHandler.class).isEqualTo(
                    expected.getReauthenticateBehaviour() == ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS);
        }
        assertThat(state.getNegotiatedProtocolVersion()).isEqualTo(expected.getConfiguredProtocolVersion());
    }

    private static void assertOptions(ClientOptions actual, ClusterClientOptions expected) {
        assertThat(actual.getConfiguredProtocolVersion()).isEqualTo(expected.getConfiguredProtocolVersion());
        assertThat(actual.isAutoReconnect()).isEqualTo(expected.isAutoReconnect());
        assertThat(actual.getRequestQueueSize()).isEqualTo(expected.getRequestQueueSize());
        assertThat(actual.getTimeoutOptions()).isSameAs(expected.getTimeoutOptions());
        assertThat(actual.getJsonParser()).isSameAs(expected.getJsonParser());
    }

    enum ConnectionType {
        CLUSTER, PUBSUB, NODE, PUBSUB_NODE
    }

    enum Stage {
        BUILDER, FACTORY
    }

    private static class PausingRedisClusterClient extends RedisClusterClient {

        final CountDownLatch paused = new CountDownLatch(1);

        final CountDownLatch resume = new CountDownLatch(1);

        final AtomicBoolean pause = new AtomicBoolean(true);

        final List<ClientOptions> factoryOptions = new ArrayList<>();

        final List<Supplier<JsonParser>> legacyFactoryParsers = new ArrayList<>();

        final Stage stage;

        PausingRedisClusterClient(Stage stage) {
            super(null, Collections.singletonList(RedisURI.create(TestSettings.host(), TestSettings.port())));
            this.stage = stage;
            // These tests exercise connection initialization without discovering a cluster topology.
            setPartitions(new Partitions());
        }

        @Override
        protected void connectionBuilder(Mono<SocketAddress> socketAddressSupplier, ConnectionBuilder connectionBuilder,
                ConnectionEvents connectionEvents, RedisURI redisURI) {

            super.connectionBuilder(socketAddressSupplier, connectionBuilder, connectionEvents, redisURI);
            pauseAt(Stage.BUILDER);
        }

        @Override
        protected <K, V> StatefulRedisConnectionImpl<K, V> newStatefulRedisConnection(ClientOptions clientOptions,
                RedisChannelWriter channelWriter, PushHandler pushHandler, RedisCodec<K, V> codec, Duration timeout) {
            factoryOptions.add(clientOptions);
            return super.newStatefulRedisConnection(clientOptions, channelWriter, pushHandler, codec, timeout);
        }

        @Override
        protected <K, V> StatefulRedisConnectionImpl<K, V> newStatefulRedisConnection(RedisChannelWriter channelWriter,
                PushHandler pushHandler, RedisCodec<K, V> codec, Duration timeout, Supplier<JsonParser> parser) {
            legacyFactoryParsers.add(parser);
            pauseAt(Stage.FACTORY);
            return super.newStatefulRedisConnection(channelWriter, pushHandler, codec, timeout, parser);
        }

        @Override
        protected <K, V> StatefulRedisClusterConnectionImpl<K, V> newStatefulRedisClusterConnection(ClientOptions clientOptions,
                RedisChannelWriter channelWriter, ClusterPushHandler pushHandler, RedisCodec<K, V> codec, Duration timeout) {
            factoryOptions.add(clientOptions);
            return super.newStatefulRedisClusterConnection(clientOptions, channelWriter, pushHandler, codec, timeout);
        }

        @Override
        protected <V, K> StatefulRedisClusterConnectionImpl<K, V> newStatefulRedisClusterConnection(
                RedisChannelWriter channelWriter, ClusterPushHandler pushHandler, RedisCodec<K, V> codec, Duration timeout,
                Supplier<JsonParser> parser) {
            legacyFactoryParsers.add(parser);
            pauseAt(Stage.FACTORY);
            return super.newStatefulRedisClusterConnection(channelWriter, pushHandler, codec, timeout, parser);
        }

        private void pauseAt(Stage currentStage) {
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

    }

}
