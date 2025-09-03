package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.inject.New;
import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.NettyCustomizer;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.FastShutdown;
import io.netty.channel.Channel;

/**
 * @author Will Glozer
 * @author Mark Paluch
 * @author Hari Mani
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class ClientIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final StatefulRedisConnection<String, String> connection;

    private final RedisCommands<String, String> redis;

    @Inject
    ClientIntegrationTests(@New final RedisClient client, @New final StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.connection = connection;
        this.redis = connection.sync();
        this.redis.flushall();
    }

    @Test
    void close() {
        connection.close();
        assertThatThrownBy(() -> connection.sync().get(key)).isInstanceOf(RedisException.class);
    }

    @Test
    void propagatesChannelInitFailure() {

        ClientResources handshakeFailure = ClientResources.builder().nettyCustomizer(new NettyCustomizer() {

            @Override
            public void afterChannelInitialized(Channel channel) {
                throw new NoSuchElementException();
            }

        }).build();
        RedisURI uri = RedisURI.create(host, port);
        RedisClient customClient = RedisClient.create(handshakeFailure, uri);
        assertThatException().isThrownBy(customClient::connect).withRootCauseInstanceOf(NoSuchElementException.class);

        FastShutdown.shutdown(customClient);
        FastShutdown.shutdown(handshakeFailure);
    }

    @Test
    void timeout() {

        connection.setTimeout(Duration.ofNanos(100));
        assertThatThrownBy(() -> redis.blpop(1, "unknown")).isInstanceOf(RedisCommandTimeoutException.class);

        connection.setTimeout(Duration.ofSeconds(60));
    }

    @Test
    void reconnect() {

        redis.set(key, value);

        redis.quit();
        Delay.delay(Duration.ofMillis(100));
        assertThat(redis.get(key)).isEqualTo(value);
        redis.quit();
        Delay.delay(Duration.ofMillis(100));
        assertThat(redis.get(key)).isEqualTo(value);
        redis.quit();
        Delay.delay(Duration.ofMillis(100));
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    void reconnectNotifiesListener() throws InterruptedException {

        class MyListener implements RedisConnectionStateListener {

            final AtomicInteger connect = new AtomicInteger();

            final AtomicInteger disconnect = new AtomicInteger();

            @Override
            public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
                connect.incrementAndGet();
            }

            @Override
            public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
                disconnect.incrementAndGet();
            }

        }

        MyListener listener = new MyListener();

        connection.addListener(listener);
        redis.quit();
        Thread.sleep(100);

        Wait.untilTrue(connection::isOpen).waitOrTimeout();

        assertThat(listener.connect).hasValueGreaterThan(0);
        assertThat(listener.disconnect).hasValueGreaterThan(0);
    }

    @Test
    void interrupt() {
        Thread.currentThread().interrupt();
        assertThatThrownBy(() -> connection.sync().blpop(0, key)).isInstanceOf(RedisCommandInterruptedException.class);
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    @Inject
    void connectFailure(ClientResources clientResources) {

        RedisClient client = RedisClient.create(clientResources, "redis://invalid");

        assertThatThrownBy(client::connect).isInstanceOf(RedisConnectionException.class)
                .hasMessageContaining("Unable to connect");

        FastShutdown.shutdown(client);
    }

    @Test
    @Inject
    void connectPubSubFailure(ClientResources clientResources) {

        RedisClient client = RedisClient.create(clientResources, "redis://invalid");

        assertThatThrownBy(client::connectPubSub).isInstanceOf(RedisConnectionException.class)
                .hasMessageContaining("Unable to connect");
        FastShutdown.shutdown(client);
    }

    @Test
    void emptyClient() {

        try {
            client.connect();
        } catch (IllegalStateException e) {
            assertThat(e).hasMessageContaining("RedisURI");
        }

        try {
            client.connect().async();
        } catch (IllegalStateException e) {
            assertThat(e).hasMessageContaining("RedisURI");
        }

        try {
            client.connect((RedisURI) null);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("RedisURI");
        }
    }

    @Test
    void testExceptionWithCause() {
        RedisException e = new RedisException(new RuntimeException());
        assertThat(e).hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    void standaloneConnectionShouldSetClientName() {
        final RedisURI redisURI = RedisURI.create(host, port);
        redisURI.setClientName("my-client");
        try (StatefulRedisConnection<String, String> connection = client.connect(redisURI)) {

            assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

            connection.sync().quit();
            Delay.delay(Duration.ofMillis(100));
            Wait.untilTrue(connection::isOpen).waitOrTimeout();

            assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());
        }
    }

    @Test
    void pubSubConnectionShouldSetClientName() {
        final RedisURI redisURI = RedisURI.create(host, port);
        redisURI.setClientName("my-client");
        try (StatefulRedisConnection<String, String> connection = client.connectPubSub(redisURI)) {

            assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());

            connection.sync().quit();
            Delay.delay(Duration.ofMillis(100));
            Wait.untilTrue(connection::isOpen).waitOrTimeout();

            assertThat(connection.sync().clientGetname()).isEqualTo(redisURI.getClientName());
        }
    }

}
