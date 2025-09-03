package io.lettuce.core.protocol;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.connection.ReconnectFailedEvent;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.NettyCustomizer;
import io.lettuce.test.*;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.server.RandomResponseServer;
import io.lettuce.test.settings.TestSettings;
import io.netty.channel.Channel;
import io.netty.channel.local.LocalAddress;

/**
 * @author Mark Paluch
 * @author Hari Mani
 */
@Tag(INTEGRATION_TEST)
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

        ClientOptions clientOptions = ClientOptions.builder().suspendReconnectOnProtocolFailure(true)
                .timeoutOptions(TimeoutOptions.builder().timeoutCommands(false).build()).build();
        client.setOptions(clientOptions);

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).build();
        redisUri.setTimeout(Duration.ofSeconds(5));

        try (StatefulRedisConnection<String, String> cnxn = client.connect(redisUri)) {
            RedisAsyncCommands<String, String> commands = cnxn.async();
            ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(cnxn);

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();
            assertThat(connectionWatchdog.isReconnectSuspended()).isFalse();
            assertThat(clientOptions.isSuspendReconnectOnProtocolFailure()).isTrue();
            assertThat(connectionWatchdog.getReconnectionHandler().getClientOptions()).isSameAs(clientOptions);

            redisUri.setPort(TestSettings.nonexistentPort());

            commands.quit();
            Wait.untilTrue(connectionWatchdog::isReconnectSuspended).waitOrTimeout();

            assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();

            assertThatThrownBy(() -> TestFutures.awaitOrTimeout(commands.info())).hasRootCauseInstanceOf(RedisException.class)
                    .hasMessageContaining("Invalid first byte");
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

        client.setOptions(ClientOptions.builder().suspendReconnectOnProtocolFailure(false)
                .timeoutOptions(TimeoutOptions.builder().timeoutCommands(false).build()).build());

        RandomResponseServer ts = getRandomResponseServer();

        RedisURI redisUri = RedisURI.create(defaultRedisUri.toURI());
        redisUri.setTimeout(Duration.ofSeconds(5));

        try (StatefulRedisConnection<String, String> connection = client.connect(redisUri)) {
            final BlockingQueue<ConnectionEvents.Reconnect> events = new LinkedBlockingDeque<>();

            RedisAsyncCommands<String, String> commands = connection.async();
            ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection);

            ReconnectionListener reconnectionListener = events::offer;

            Field field = ConnectionWatchdog.class.getDeclaredField("reconnectionListener");
            field.setAccessible(true);
            field.set(connectionWatchdog, reconnectionListener);

            redisUri.setPort(TestSettings.nonexistentPort());

            commands.quit();
            Wait.untilTrue(() -> events.size() > 1).waitOrTimeout();

            ConnectionEvents.Reconnect event1 = events.take();
            assertThat(event1.getAttempt()).isEqualTo(1);

            ConnectionEvents.Reconnect event2 = events.take();
            assertThat(event2.getAttempt()).isEqualTo(2);

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

        client.setOptions(
                ClientOptions.builder().timeoutOptions(TimeoutOptions.builder().timeoutCommands(false).build()).build());

        try (StatefulRedisConnection<String, String> connection = client.connect(redisUri)) {
            RedisAsyncCommandsImpl<String, String> commands = (RedisAsyncCommandsImpl<String, String>) connection.async();
            ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection);

            redisUri.setPort(TestSettings.nonexistentPort());

            client.getResources().eventBus().get().subscribe(queue::add);

            commands.quit();
            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

            connectionWatchdog.run(0);
            Delay.delay(Duration.ofMillis(500));

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
        client.setOptions(ClientOptions.builder().pingBeforeActivateConnection(true)
                .timeoutOptions(TimeoutOptions.builder().timeoutCommands(false).build()).build());

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
        client.setOptions(
                ClientOptions.builder().timeoutOptions(TimeoutOptions.builder().timeoutCommands(false).build()).build());

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
