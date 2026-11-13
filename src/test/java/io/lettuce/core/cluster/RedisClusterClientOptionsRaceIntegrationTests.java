package io.lettuce.core.cluster;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ConnectionState;
import io.lettuce.core.RedisAuthenticationHandler;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.core.protocol.CommandExpiryWriter;
import io.lettuce.core.protocol.CommandHandler;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnectionImpl;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.netty.channel.Channel;

@Tag(INTEGRATION_TEST)
class RedisClusterClientOptionsRaceIntegrationTests {

    private static final ClusterClientOptions FIRST = ClusterClientOptions.builder().protocolVersion(ProtocolVersion.RESP2)
            .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5))).build();

    private static final ClusterClientOptions SECOND = (ClusterClientOptions) ClusterClientOptions.builder()
            .protocolVersion(ProtocolVersion.RESP3).timeoutOptions(TimeoutOptions.create())
            .reauthenticateBehavior(ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS)
            .jsonParser(ClientOptions.DEFAULT_JSON_PARSER::get).build();

    private static final RedisURI NODE_URI = RedisURI.create(ClusterTestSettings.host, ClusterTestSettings.port1);

    @ParameterizedTest
    @EnumSource(ConnectionType.class)
    void shouldKeepOptionsConsistentDuringConnectionCreation(ConnectionType type) throws Exception {

        PausingRedisClusterClient client = new PausingRedisClusterClient();
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
    @EnumSource(value = ConnectionType.class, names = { "CLUSTER", "PUBSUB" })
    void retryShouldRetainOptionsAcrossAsyncFailure(ConnectionType type) throws Exception {

        CompletableFuture<SocketAddress> firstAddress = new CompletableFuture<>();
        AtomicBoolean firstAttempt = new AtomicBoolean(true);
        RedisClusterClient client = new RedisClusterClient(TestClientResources.get(), Collections.singletonList(NODE_URI)) {

            @Override
            protected Supplier<CompletionStage<SocketAddress>> getSocketAddressSupplier(Supplier<Partitions> partitionsSupplier,
                    Function<Partitions, Collection<RedisClusterNode>> sortFunction) {
                return () -> firstAttempt.compareAndSet(true, false) ? firstAddress : nodeAddress();
            }

        };
        // The client makes one connection attempt per known node.
        Partitions partitions = new Partitions();
        for (String nodeId : new String[] { "first", "second" }) {
            partitions.add(new RedisClusterNode(NODE_URI, nodeId, true, null, 0, 0, 0, Collections.emptyList(),
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

    private static CompletionStage<SocketAddress> nodeAddress() {
        return CompletableFuture.completedFuture(new InetSocketAddress(NODE_URI.getHost(), NODE_URI.getPort()));
    }

    private static CompletableFuture<RedisChannelHandler<String, String>> connect(RedisClusterClient client,
            ConnectionType type) {
        switch (type) {
            case CLUSTER:
                return client.connectAsync(StringCodec.UTF8)
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection);
            case PUBSUB:
                return client.connectPubSubAsync(StringCodec.UTF8)
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection);
            case NODE:
                return client.connectToNodeAsync(StringCodec.UTF8, "node", null, () -> nodeAddress())
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection).toCompletableFuture();
            case PUBSUB_NODE:
                return client.connectPubSubToNodeAsync(StringCodec.UTF8, "node", () -> nodeAddress())
                        .thenApply(connection -> (RedisChannelHandler<String, String>) connection).toCompletableFuture();
            default:
                throw new IllegalArgumentException("Unsupported connection type: " + type);
        }
    }

    private static void assertConnectionOptions(RedisChannelHandler<String, String> connection, ClusterClientOptions expected) {

        assertThat(connection.getOptions()).isSameAs(expected);
        RedisChannelWriter writer = connection.getChannelWriter();

        if (writer instanceof ClusterDistributionChannelWriter) {
            assertThat((Object) ReflectionTestUtils.getField(writer, "clientOptions")).isSameAs(expected);
            ClusterConnectionProvider provider = ((ClusterDistributionChannelWriter) writer).getClusterConnectionProvider();
            assertThat((Object) ReflectionTestUtils.getField(provider, "options")).isSameAs(expected);
            writer = ReflectionTestUtils.getField(writer, "defaultWriter");
        }

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
        if (connection instanceof StatefulRedisClusterConnectionImpl) {
            StatefulRedisClusterConnectionImpl<?, ?> cluster = (StatefulRedisClusterConnectionImpl<?, ?>) connection;
            state = cluster.getConnectionState();
            assertThat(cluster.parser).isSameAs(expected.getJsonParser());
        } else {
            StatefulRedisConnectionImpl<?, ?> node = (StatefulRedisConnectionImpl<?, ?>) connection;
            state = node.getConnectionState();

            boolean reauthenticate = expected
                    .getReauthenticateBehaviour() == ClientOptions.ReauthenticateBehavior.ON_NEW_CREDENTIALS;
            Object authHandler = ReflectionTestUtils.getField(node, "authHandler");
            assertThat(authHandler.getClass() == RedisAuthenticationHandler.class).isEqualTo(reauthenticate);

            if (!(node instanceof StatefulRedisPubSubConnectionImpl)) {
                assertThat((Object) ReflectionTestUtils.getField(node, "parser")).isSameAs(expected.getJsonParser());
            }
        }

        assertThat(state.getNegotiatedProtocolVersion()).isEqualTo(expected.getConfiguredProtocolVersion());
        assertThat(state.getClientOptions()).isSameAs(expected);
    }

    enum ConnectionType {
        CLUSTER, PUBSUB, NODE, PUBSUB_NODE
    }

    private static class PausingRedisClusterClient extends RedisClusterClient {

        final CountDownLatch paused = new CountDownLatch(1);

        final CountDownLatch resume = new CountDownLatch(1);

        PausingRedisClusterClient() {
            super(TestClientResources.get(), Collections.singletonList(NODE_URI));
            // These tests exercise connection initialization without discovering the cluster topology.
            setPartitions(new Partitions());
        }

        // Each connect method calls this right after it has read the client options and created the endpoint.
        @Override
        protected List<CommandListener> getCommandListeners() {
            paused.countDown();
            try {
                assertThat(resume.await(10, SECONDS)).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return super.getCommandListeners();
        }

    }

}
