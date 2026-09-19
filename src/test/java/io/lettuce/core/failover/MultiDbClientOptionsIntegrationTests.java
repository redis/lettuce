package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.api.BaseRedisMultiDbConnection;
import io.lettuce.core.failover.api.DatabaseConfig;
import io.lettuce.core.failover.api.MultiDbOptions;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.failover.health.HealthCheckStrategySupplier;
import io.lettuce.core.protocol.CommandExpiryWriter;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import io.netty.channel.Channel;

/**
 * Connection options for individual databases and their health-check connections.
 *
 * @author Ali Takavci
 */
@Tag(INTEGRATION_TEST)
class MultiDbClientOptionsIntegrationTests {

    private static final RedisURI URI1 = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();

    private static final RedisURI URI2 = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port() + 1).build();

    private static final ClientOptions FIRST = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2)
            .autoReconnect(false).requestQueueSize(10).timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5)))
            .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(5)).build()).build();

    private static final ClientOptions SECOND = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP3)
            .autoReconnect(true).requestQueueSize(20)
            .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(10)).build())
            .jsonParser(() -> ClientOptions.DEFAULT_JSON_PARSER.get()).build();

    private MultiDbClientImpl client;

    @AfterEach
    void tearDown() {
        if (client != null) {
            FastShutdown.shutdown((MultiDbClient) client);
        }
    }

    @Test
    void syncConnectShouldRespectPerDatabaseOptions() {
        client = new MultiDbClientImpl(databases(), MultiDbOptions.create());

        try (StatefulRedisMultiDbConnection<String, String> connection = client.connect(StringCodec.UTF8)) {
            assertDatabaseOptions(connection);
        }
    }

    @Test
    void asyncConnectShouldRespectPerDatabaseOptions() throws Exception {
        client = new MultiDbClientImpl(databases(), MultiDbOptions.create());

        try (StatefulRedisMultiDbConnection<String, String> connection = client.connectAsync(StringCodec.UTF8).get(10,
                SECONDS)) {
            assertDatabaseOptions(connection);
        }
    }

    @Test
    void parallelAsyncConnectsShouldKeepPerDatabaseOptions() throws Exception {
        client = new MultiDbClientImpl(databases(), MultiDbOptions.create());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<CompletableFuture<StatefulRedisMultiDbConnection<String, String>>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 5; i++) {
                futures.add(CompletableFuture.supplyAsync(() -> client.connectAsync(StringCodec.UTF8), executor)
                        .thenCompose(future -> future.toCompletableFuture()));
            }

            for (CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future : futures) {
                try (StatefulRedisMultiDbConnection<String, String> connection = future.get(10, SECONDS)) {
                    assertDatabaseOptions(connection);
                }
            }
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }
    }

    @Test
    void pubSubConnectShouldRespectPerDatabaseOptions() throws Exception {
        client = new MultiDbClientImpl(databases(), MultiDbOptions.create());

        try (StatefulRedisMultiDbPubSubConnection<String, String> connection = client.connectPubSubAsync(StringCodec.UTF8)
                .get(10, SECONDS)) {
            assertDatabaseOptions(connection);
        }
    }

    @Test
    void healthCheckConnectionsShouldUseTheirDatabaseOptions() {
        client = new MultiDbClientImpl(databases(), MultiDbOptions.create());
        ClientOptions defaults = client.getOptions();

        try (StatefulRedisConnection<?, ?> first = new RawConnectionFactoryImpl(FIRST, client).create(URI1);
                StatefulRedisConnection<?, ?> second = new RawConnectionFactoryImpl(SECOND, client).create(URI2)) {
            assertConnectionOptions(first, FIRST);
            assertConnectionOptions(second, SECOND);
        }

        assertThat(client.getOptions()).isSameAs(defaults);
    }

    @Test
    void failedHealthCheckConnectionShouldNotAffectTheNextConnection() {
        AtomicBoolean fail = new AtomicBoolean(true);
        client = new MultiDbClientImpl(databases(), MultiDbOptions.create()) {

            @Override
            protected DefaultEndpoint createEndpoint(ClientOptions clientOptions) {
                if (fail.compareAndSet(true, false)) {
                    throw new IllegalStateException("Endpoint creation failed");
                }
                return super.createEndpoint(clientOptions);
            }

        };
        ClientOptions defaults = client.getOptions();

        assertThatThrownBy(() -> new RawConnectionFactoryImpl(FIRST, client).create(URI1))
                .isInstanceOf(IllegalStateException.class).hasMessage("Endpoint creation failed");

        try (StatefulRedisConnection<?, ?> connection = new RawConnectionFactoryImpl(SECOND, client).create(URI2)) {
            assertConnectionOptions(connection, SECOND);
        }

        assertThat(client.getOptions()).isSameAs(defaults);
    }

    @Test
    void databaseOptionsShouldSurviveClientOptionsChangesDuringConnectionCreation() throws Exception {
        CountDownLatch paused = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        client = new MultiDbClientImpl(databases(), MultiDbOptions.create()) {

            @Override
            protected DefaultEndpoint createEndpoint(ClientOptions clientOptions) {
                if (clientOptions.getConfiguredProtocolVersion() == ProtocolVersion.RESP2) {
                    paused.countDown();
                    try {
                        assertThat(resume.await(10, SECONDS)).isTrue();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                }
                return super.createEndpoint(clientOptions);
            }

        };
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = CompletableFuture
                    .supplyAsync(() -> client.connectAsync(StringCodec.UTF8), executor)
                    .thenCompose(connection -> connection.toCompletableFuture());

            assertThat(paused.await(10, SECONDS)).isTrue();
            client.setOptions(SECOND);
            resume.countDown();

            try (StatefulRedisMultiDbConnection<String, String> connection = future.get(10, SECONDS)) {
                assertDatabaseOptions(connection);
            }

            assertThat(client.getOptions()).isSameAs(SECOND);
        } finally {
            resume.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }
    }

    private static List<DatabaseConfig> databases() {
        return Arrays.asList(
                DatabaseConfig.builder(URI1).weight(1.0f).clientOptions(FIRST)
                        .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build(),
                DatabaseConfig.builder(URI2).weight(0.5f).clientOptions(SECOND)
                        .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build());
    }

    private static void assertDatabaseOptions(BaseRedisMultiDbConnection connection) {
        MultiDbTestSupport.waitForEndpoints(connection, 2, 10);
        assertConnectionOptions(((RedisDatabaseImpl<?>) connection.getDatabase(URI1)).getConnection(), FIRST);
        assertConnectionOptions(((RedisDatabaseImpl<?>) connection.getDatabase(URI2)).getConnection(), SECOND);
    }

    private static void assertConnectionOptions(StatefulRedisConnection<?, ?> connection, ClientOptions expected) {
        assertThat(connection.isOpen()).isTrue();
        assertOptions(connection.getOptions(), expected);

        StatefulRedisConnectionImpl<?, ?> stateful = (StatefulRedisConnectionImpl<?, ?>) connection;
        assertThat(stateful.getConnectionState().getNegotiatedProtocolVersion())
                .isEqualTo(expected.getConfiguredProtocolVersion());
        assertThat(connection.sync().ping()).isEqualTo("PONG");

        if (!(connection instanceof StatefulRedisPubSubConnection)) {
            assertThat((Object) ReflectionTestUtils.getField(connection.async(), "parser")).isSameAs(expected.getJsonParser());
            assertThat((Object) ReflectionTestUtils.getField(connection.reactive(), "parser"))
                    .isSameAs(expected.getJsonParser());
        }

        RedisChannelWriter writer = stateful.getChannelWriter();
        assertThat(writer instanceof CommandExpiryWriter).isEqualTo(expected.getTimeoutOptions().isTimeoutCommands());
        if (writer instanceof CommandExpiryWriter) {
            writer = ((CommandExpiryWriter) writer).getDelegate();
        }
        DefaultEndpoint endpoint = (DefaultEndpoint) writer;
        assertOptions(ReflectionTestUtils.getField(endpoint, "clientOptions"), expected);
        Channel channel = ReflectionTestUtils.getField(endpoint, "channel");
        assertOptions(ReflectionTestUtils.getField(channel.pipeline().get(CommandHandler.class), "clientOptions"), expected);
    }

    private static void assertOptions(ClientOptions actual, ClientOptions expected) {
        assertThat(actual.getConfiguredProtocolVersion()).isEqualTo(expected.getConfiguredProtocolVersion());
        assertThat(actual.isAutoReconnect()).isEqualTo(expected.isAutoReconnect());
        assertThat(actual.getRequestQueueSize()).isEqualTo(expected.getRequestQueueSize());
        assertThat(actual.getSocketOptions().getConnectTimeout()).isEqualTo(expected.getSocketOptions().getConnectTimeout());
        assertThat(actual.getTimeoutOptions()).isSameAs(expected.getTimeoutOptions());
        assertThat(actual.getJsonParser()).isSameAs(expected.getJsonParser());
    }

}
