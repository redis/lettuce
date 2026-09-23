package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.protocol.CommandExpiryWriter;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import io.netty.channel.Channel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(INTEGRATION_TEST)
class MasterReplicaClientOptionsRaceIntegrationTests {

    private static final ClientOptions FIRST = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2)
            .autoReconnect(false).requestQueueSize(10).build();

    private static final ClientOptions SECOND = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
            .autoReconnect(true).requestQueueSize(20).jsonParser(() -> ClientOptions.DEFAULT_JSON_PARSER.get()).build();

    @ParameterizedTest
    @MethodSource("connectionStages")
    void shouldRetainOptionsWhileDiscoveringTopology(ConnectionType type, Stage stage) throws Exception {
        PausingRedisClient client = new PausingRedisClient(stage);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        client.setOptions(FIRST);

        try {
            CompletableFuture<StatefulRedisMasterReplicaConnection<String, String>> pending = CompletableFuture
                    .supplyAsync(() -> connect(client, type), executor).thenCompose(future -> future);

            assertThat(client.paused.await(10, SECONDS)).isTrue();
            client.setOptions(SECOND);
            client.resume.countDown();

            try (StatefulRedisMasterReplicaConnection<String, String> connection = pending.get(10, SECONDS)) {
                assertOptions(connection, FIRST);
                client.assertConnectionOptions(FIRST);
            }

            client.clearConnections();
            try (StatefulRedisMasterReplicaConnection<String, String> connection = connect(client, type).get(10, SECONDS)) {
                assertOptions(connection, SECOND);
                client.assertConnectionOptions(SECOND);
            }

            assertThat(client.getOptions()).isSameAs(SECOND);
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
    @EnumSource(ConnectionType.class)
    void shouldRetainOptionsWhenOpeningNodeAfterTopologyChange(ConnectionType type) throws Exception {
        PausingRedisClient client = new PausingRedisClient(Stage.OPTIONS_CAPTURED);
        client.resume.countDown();
        client.setOptions(FIRST);

        try (StatefulRedisMasterReplicaConnection<String, String> connection = connect(client, type).get(10, SECONDS)) {
            assertOptions(connection, FIRST);
            client.clearConnections();
            client.setOptions(SECOND);

            StatefulRedisMasterReplicaConnectionImpl<String, String> stateful = (StatefulRedisMasterReplicaConnectionImpl<String, String>) connection;
            MasterReplicaConnectionProvider<?, ?> provider = ((MasterReplicaChannelWriter) stateful.getChannelWriter())
                    .getUpstreamReplicaConnectionProvider();
            RedisNodeDescription master = provider.getMaster();
            provider.setKnownNodes(Collections.emptyList());
            provider.setKnownNodes(Collections.singletonList(master));

            assertOptions(connection, FIRST);
            client.assertConnectionOptions(FIRST);
            assertThat(client.getOptions()).isSameAs(SECOND);
        } finally {
            FastShutdown.shutdown(client);
        }
    }

    private static Stream<Arguments> connectionStages() {
        return Stream.of(ConnectionType.values())
                .flatMap(type -> Stream.of(Stage.values()).map(stage -> Arguments.of(type, stage)));
    }

    private static CompletableFuture<StatefulRedisMasterReplicaConnection<String, String>> connect(RedisClient client,
            ConnectionType type) {
        switch (type) {
            case AUTODISCOVERY:
                return MasterReplica.connectAsync(client, StringCodec.UTF8, uri());
            case STATIC:
                return MasterReplica.connectAsync(client, StringCodec.UTF8, Collections.singletonList(uri()));
            case SENTINEL:
                RedisURI sentinel = RedisURI.Builder.sentinel(TestSettings.host(), TestSettings.port(), "mymaster").build();
                return MasterReplica.connectAsync(client, StringCodec.UTF8, sentinel);
            default:
                throw new IllegalArgumentException("Unsupported connection type: " + type);
        }
    }

    private static void assertOptions(StatefulRedisMasterReplicaConnection<String, String> connection, ClientOptions expected) {
        assertThat(connection.getOptions()).isSameAs(expected);
        StatefulRedisMasterReplicaConnectionImpl<String, String> stateful = (StatefulRedisMasterReplicaConnectionImpl<String, String>) connection;
        assertThat((Object) ReflectionTestUtils.getField(stateful.getChannelWriter(), "clientOptions")).isSameAs(expected);
        assertThat((Object) ReflectionTestUtils.getField(connection.async(), "parser")).isSameAs(expected.getJsonParser());
        assertThat((Object) ReflectionTestUtils.getField(connection.reactive(), "parser")).isSameAs(expected.getJsonParser());
        assertThat(connection.sync().ping()).isEqualTo("PONG");
        MasterReplicaChannelWriter writer = (MasterReplicaChannelWriter) stateful.getChannelWriter();
        StatefulRedisConnection<?, ?> node = writer.getUpstreamReplicaConnectionProvider()
                .getConnection(ConnectionIntent.WRITE);
        assertNodeOptions(node, expected);
    }

    private static void assertNodeOptions(StatefulRedisConnection<?, ?> node, ClientOptions expected) {
        assertClientOptions(node.getOptions(), expected);
        assertThat(((StatefulRedisConnectionImpl<?, ?>) node).getConnectionState().getNegotiatedProtocolVersion())
                .isEqualTo(expected.getProtocolVersion());
        RedisChannelWriter writer = ((StatefulRedisConnectionImpl<?, ?>) node).getChannelWriter();
        if (writer instanceof CommandExpiryWriter) {
            writer = ((CommandExpiryWriter) writer).getDelegate();
        }
        DefaultEndpoint endpoint = (DefaultEndpoint) writer;
        assertClientOptions(ReflectionTestUtils.getField(endpoint, "clientOptions"), expected);
        if (node.isOpen()) {
            Channel channel = ReflectionTestUtils.getField(endpoint, "channel");
            assertClientOptions(ReflectionTestUtils.getField(channel.pipeline().get(CommandHandler.class), "clientOptions"),
                    expected);
        }
    }

    private static void assertClientOptions(ClientOptions actual, ClientOptions expected) {
        assertThat(actual.getProtocolVersion()).isEqualTo(expected.getProtocolVersion());
        assertThat(actual.isAutoReconnect()).isEqualTo(expected.isAutoReconnect());
        assertThat(actual.getRequestQueueSize()).isEqualTo(expected.getRequestQueueSize());
        assertThat(actual.getJsonParser()).isSameAs(expected.getJsonParser());
    }

    private static RedisURI uri() {
        return RedisURI.create(TestSettings.host(), TestSettings.port());
    }

    enum ConnectionType {
        AUTODISCOVERY, STATIC, SENTINEL
    }

    enum Stage {
        OPTIONS_CAPTURED, TOPOLOGY
    }

    private static class PausingRedisClient extends RedisClient {

        final CountDownLatch paused = new CountDownLatch(1);

        final CountDownLatch resume = new CountDownLatch(1);

        private final AtomicBoolean pause = new AtomicBoolean(true);

        private final Stage stage;

        private final List<StatefulRedisConnection<?, ?>> connections = new CopyOnWriteArrayList<>();

        private final List<ClientOptions> sentinelOptions = new CopyOnWriteArrayList<>();

        PausingRedisClient(Stage stage) {
            super(null, uri());
            this.stage = stage;
        }

        @Override
        public ClientOptions getOptions() {
            ClientOptions options = super.getOptions();
            pauseAt(Stage.OPTIONS_CAPTURED);
            return options;
        }

        @Override
        public <K, V> ConnectionFuture<StatefulRedisConnection<K, V>> connectAsync(RedisCodec<K, V> codec, RedisURI redisURI,
                ClientOptions clientOptions) {
            return super.connectAsync(codec, redisURI, clientOptions).thenApply(connection -> {
                connections.add(connection);
                pauseAt(Stage.TOPOLOGY);
                return connection;
            });
        }

        @Override
        public <K, V> ConnectionFuture<StatefulRedisPubSubConnection<K, V>> connectPubSubAsync(RedisCodec<K, V> codec,
                RedisURI redisURI, ClientOptions clientOptions) {
            return super.connectPubSubAsync(codec, redisURI, clientOptions).thenApply(connection -> {
                connections.add(connection);
                return connection;
            });
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, V> CompletableFuture<StatefulRedisSentinelConnection<K, V>> connectSentinelAsync(RedisCodec<K, V> codec,
                RedisURI redisURI, ClientOptions clientOptions) {
            sentinelOptions.add(clientOptions);
            pauseAt(Stage.TOPOLOGY);

            // Supply one master through Sentinel discovery; node connections and Pub/Sub use the test Redis server.
            StatefulRedisSentinelConnection<String, String> sentinel = mock(StatefulRedisSentinelConnection.class);
            RedisSentinelReactiveCommands<String, String> commands = mock(RedisSentinelReactiveCommands.class);
            Map<String, String> master = new HashMap<>();
            master.put("ip", TestSettings.host());
            master.put("port", Integer.toString(TestSettings.port()));
            when(sentinel.reactive()).thenReturn(commands);
            when(sentinel.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
            when(commands.master("mymaster")).thenReturn(Mono.just(master));
            when(commands.replicas("mymaster")).thenReturn(Flux.empty());

            return CompletableFuture.completedFuture((StatefulRedisSentinelConnection<K, V>) sentinel);
        }

        void assertConnectionOptions(ClientOptions expected) {
            assertThat(connections).isNotEmpty();
            connections.forEach(connection -> assertNodeOptions(connection, expected));
            sentinelOptions.forEach(options -> assertThat(options).isSameAs(expected));
        }

        void clearConnections() {
            connections.clear();
            sentinelOptions.clear();
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
