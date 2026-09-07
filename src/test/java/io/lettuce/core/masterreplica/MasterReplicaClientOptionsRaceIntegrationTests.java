package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(INTEGRATION_TEST)
class MasterReplicaClientOptionsRaceIntegrationTests {

    private static final ClientOptions FIRST = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2)
            .autoReconnect(false).requestQueueSize(10).build();

    private static final ClientOptions SECOND = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
            .autoReconnect(true).requestQueueSize(20).jsonParser(() -> ClientOptions.DEFAULT_JSON_PARSER.get()).build();

    @ParameterizedTest
    @EnumSource(ConnectionType.class)
    void shouldRetainOptionsWhileDiscoveringTopology(ConnectionType type) throws Exception {
        PausingRedisClient client = new PausingRedisClient();
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
            }

            try (StatefulRedisMasterReplicaConnection<String, String> connection = connect(client, type).get(10, SECONDS)) {
                assertOptions(connection, SECOND);
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
    }

    private static RedisURI uri() {
        return RedisURI.create(TestSettings.host(), TestSettings.port());
    }

    enum ConnectionType {
        AUTODISCOVERY, STATIC, SENTINEL
    }

    private static class PausingRedisClient extends RedisClient {

        final CountDownLatch paused = new CountDownLatch(1);

        final CountDownLatch resume = new CountDownLatch(1);

        private final AtomicBoolean pause = new AtomicBoolean(true);

        PausingRedisClient() {
            super(null, uri());
        }

        @Override
        public <K, V> ConnectionFuture<StatefulRedisConnection<K, V>> connectAsync(RedisCodec<K, V> codec, RedisURI redisURI) {
            pauseFirstConnection();
            return super.connectAsync(codec, redisURI);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, V> CompletableFuture<StatefulRedisSentinelConnection<K, V>> connectSentinelAsync(RedisCodec<K, V> codec,
                RedisURI redisURI) {
            pauseFirstConnection();

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

        private void pauseFirstConnection() {
            if (pause.compareAndSet(true, false)) {
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
