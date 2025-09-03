package io.lettuce.core.reliability;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.test.ConnectionTestUtil.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.test.ConnectionTestUtil;
import io.lettuce.test.Delay;
import io.lettuce.test.TestFutures;
import io.lettuce.test.Wait;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.EncoderException;
import io.netty.util.Version;

/**
 * @author Mark Paluch
 * @author Hari Mani
 */
@Tag(INTEGRATION_TEST)
@SuppressWarnings("rawtypes")
class AtMostOnceIntegrationTests {

    private static final String key = "key";

    private final RedisClient client;

    public AtMostOnceIntegrationTests() {
        // needs to be increased on slow systems...perhaps...
        final RedisURI uri = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port()).withTimeout(Duration.ofSeconds(3))
                .build();
        this.client = RedisClient.create(uri);
        this.client.setOptions(ClientOptions.builder().autoReconnect(false)
                .timeoutOptions(TimeoutOptions.builder().timeoutCommands(false).build()).build());
    }

    @BeforeEach
    void before() {
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisCommands<String, String> command = connection.sync();
            command.flushall();
            command.flushdb();
        }
    }

    @AfterEach
    void tearDown() {
        FastShutdown.shutdown(client);
    }

    @Test
    void connectionIsConnectedAfterConnect() {

        StatefulRedisConnection<String, String> connection = client.connect();

        assertThat(ConnectionTestUtil.getConnectionState(connection));

        connection.close();
    }

    @Test
    void noReconnectHandler() {

        StatefulRedisConnection<String, String> connection = client.connect();

        assertThat(ConnectionTestUtil.getConnectionWatchdog(connection)).isNull();

        connection.close();
    }

    @Test
    void basicOperations() {
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisCommands<String, String> command = connection.sync();

            command.set(key, "1");
            assertThat(command.get("key")).isEqualTo("1");
        }
    }

    @Test
    void noBufferedCommandsAfterExecute() {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> sync = connection.sync();

        sync.set(key, "1");

        assertThat(getStack(connection)).isEmpty();
        assertThat(getCommandBuffer(connection)).isEmpty();

        connection.close();
    }

    @Test
    void commandIsExecutedOnce() {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> sync = connection.sync();

        sync.set(key, "1");
        sync.incr(key);
        assertThat(sync.get(key)).isEqualTo("2");

        sync.incr(key);
        assertThat(sync.get(key)).isEqualTo("3");

        sync.incr(key);
        assertThat(sync.get(key)).isEqualTo("4");

        connection.close();
    }

    @Test
    void commandNotExecutedFailsOnEncode() {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> sync = client.connect().sync();
        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection);

        sync.set(key, "1");
        AsyncCommand<String, String, String> working = new AsyncCommand<>(new Command<String, String, String>(CommandType.INCR,
                new IntegerOutput(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).addKey(key)));
        channelWriter.write(working);
        assertThat(working.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(sync.get(key)).isEqualTo("2");

        AsyncCommand<String, String, Object> command = new AsyncCommand<String, String, Object>(
                new Command<String, String, Object>(CommandType.INCR, new IntegerOutput(StringCodec.UTF8),
                        new CommandArgs<>(StringCodec.UTF8).addKey(key))) {

            @Override
            public void encode(ByteBuf buf) {
                throw new IllegalStateException("I want to break free");
            }

        };

        channelWriter.write(command);

        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isFalse();
        assertThat(getException(command)).isInstanceOf(EncoderException.class);

        Wait.untilTrue(() -> !ConnectionTestUtil.getStack(connection).isEmpty()).waitOrTimeout();

        assertThat(ConnectionTestUtil.getStack(connection)).isNotEmpty();
        ConnectionTestUtil.getStack(connection).clear();

        assertThat(sync.get(key)).isEqualTo("2");

        assertThat(ConnectionTestUtil.getStack(connection)).isEmpty();
        assertThat(ConnectionTestUtil.getCommandBuffer(connection)).isEmpty();

        connection.close();
    }

    @Test
    void commandNotExecutedChannelClosesWhileFlush() {

        assumeTrue(Version.identify().get("netty-transport").artifactVersion().startsWith("4.0.2"));

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> sync = connection.sync();
        RedisCommands<String, String> verificationConnection = client.connect().sync();
        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection);

        sync.set(key, "1");
        assertThat(verificationConnection.get(key)).isEqualTo("1");

        final CountDownLatch block = new CountDownLatch(1);

        AsyncCommand<String, String, Object> command = new AsyncCommand<String, String, Object>(new Command<>(CommandType.INCR,
                new IntegerOutput(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).addKey(key))) {

            @Override
            public void encode(ByteBuf buf) {
                try {
                    block.await();
                } catch (InterruptedException e) {
                }
                super.encode(buf);
            }

        };

        channelWriter.write(command);

        Channel channel = ConnectionTestUtil.getChannel(connection);
        channel.unsafe().disconnect(channel.newPromise());

        assertThat(channel.isOpen()).isFalse();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isFalse();
        block.countDown();
        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isTrue();

        assertThat(verificationConnection.get(key)).isEqualTo("1");

        assertThat(getStack(connection)).isEmpty();
        assertThat(getCommandBuffer(connection)).isEmpty();

        connection.close();
    }

    @Test
    void commandFailsDuringDecode() {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> sync = connection.sync();
        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection);
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        sync.set(key, "1");

        AsyncCommand<String, String, String> command = new AsyncCommand<>(new Command<>(CommandType.INCR,
                new StatusOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).addKey(key)));

        channelWriter.write(command);

        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isFalse();
        assertThat(getException(command)).isInstanceOf(UnsupportedOperationException.class);

        assertThat(verificationConnection.get(key)).isEqualTo("2");
        assertThat(sync.get(key)).isEqualTo("2");

        connection.close();
    }

    @Test
    void noCommandsExecutedAfterConnectionIsDisconnected() {

        StatefulRedisConnection<String, String> connection = client.connect();
        connection.sync().quit();

        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

        try {
            connection.sync().incr(key);
        } catch (RedisException e) {
            assertThat(e).isInstanceOf(RedisException.class);
        }

        connection.close();

        StatefulRedisConnection<String, String> connection2 = client.connect();
        connection2.async().quit();
        Delay.delay(Duration.ofMillis(100));

        try {

            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

            connection2.sync().incr(key);
        } catch (Exception e) {
            assertThat(e).isExactlyInstanceOf(RedisException.class).hasMessageContaining("not connected");
        }

        connection2.close();
    }

    @Test
    void commandsCancelledOnDisconnect() {
        try (StatefulRedisConnection<String, String> connection = client.connect()) {

            RedisAsyncCommands<String, String> async = connection.async();
            connection.setAutoFlushCommands(false);
            async.quit();

            RedisFuture<Long> incr = async.incr(key);

            connection.flushCommands();

            TestFutures.awaitOrTimeout(incr);

        } catch (Exception e) {
            assertThat(e).hasRootCauseInstanceOf(RedisException.class).hasMessageContaining("Connection disconnected");
        }
    }

    private Throwable getException(RedisFuture<?> command) {
        try {
            command.get();
        } catch (InterruptedException e) {
            return e;
        } catch (ExecutionException e) {
            return e.getCause();
        }
        return null;
    }

}
