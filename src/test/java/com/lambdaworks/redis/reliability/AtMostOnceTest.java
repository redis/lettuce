package com.lambdaworks.redis.reliability;

import static com.lambdaworks.Connections.getCommandBuffer;
import static com.lambdaworks.Connections.getQueue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.Connections;
import com.lambdaworks.Wait;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.IntegerOutput;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.EncoderException;
import io.netty.util.Version;

/**
 * @author Mark Paluch
 */
@SuppressWarnings("rawtypes")
public class AtMostOnceTest extends AbstractRedisClientTest {

    protected final Utf8StringCodec CODEC = new Utf8StringCodec();
    protected String key = "key";

    @Before
    public void before() throws Exception {
        client.setOptions(ClientOptions.builder().autoReconnect(false).build());

        // needs to be increased on slow systems...perhaps...
        client.setDefaultTimeout(3, TimeUnit.SECONDS);

        RedisCommands<String, String> connection = client.connect().sync();
        connection.flushall();
        connection.flushdb();
        connection.getStatefulConnection().close();
    }

    @Test
    public void connectionIsConnectedAfterConnect() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();

        assertThat(Connections.getConnectionState(connection));

        connection.close();
    }

    @Test
    public void noReconnectHandler() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();

        ConnectionWatchdog connectionWatchdog = Connections.getConnectionWatchdog(connection.getStatefulConnection());
        assertThat(connectionWatchdog).isNull();

        connection.getStatefulConnection().close();
    }

    @Test
    public void basicOperations() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();

        connection.set(key, "1");
        assertThat(connection.get("key")).isEqualTo("1");

        connection.getStatefulConnection().close();
    }

    @Test
    public void noBufferedCommandsAfterExecute() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> sync = connection.sync();

        sync.set(key, "1");

        assertThat(getQueue(connection)).isEmpty();
        assertThat(getCommandBuffer(connection)).isEmpty();

        connection.close();
    }

    @Test
    public void commandIsExecutedOnce() throws Exception {

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
    public void commandNotExecutedFailsOnEncode() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> sync = connection.sync();
        RedisChannelWriter<String, String> channelWriter = Connections.getChannelWriter(connection);

        sync.set(key, "1");
        AsyncCommand<String, String, String> working = new AsyncCommand<>(new Command<String, String, String>(CommandType.INCR,
                new IntegerOutput(CODEC), new CommandArgs<String, String>(CODEC).addKey(key)));
        channelWriter.write(working);
        assertThat(working.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(sync.get(key)).isEqualTo("2");

        AsyncCommand<String, String, Object> command = new AsyncCommand<String, String, Object>(
                new Command<String, String, Object>(CommandType.INCR, new IntegerOutput(CODEC),
                        new CommandArgs<String, String>(CODEC).addKey(key))) {

            @Override
            public void encode(ByteBuf buf) {
                throw new IllegalStateException("I want to break free");
            }
        };

        channelWriter.write(command);

        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isFalse();
        assertThat(getException(command)).isInstanceOf(EncoderException.class);
        assertThat(getQueue(connection)).isNotEmpty();
        getQueue(connection).clear();

        assertThat(sync.get(key)).isEqualTo("2");

        assertThat(getQueue(connection)).isEmpty();
        assertThat(getCommandBuffer(connection)).isEmpty();

        connection.close();
    }

    @Test
    public void commandNotExecutedChannelClosesWhileFlush() throws Exception {

        assumeTrue(Version.identify().get("netty-transport").artifactVersion().startsWith("4.0.2"));

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> sync = connection.sync();
        RedisCommands<String, String> verificationConnection = client.connect().sync();
        RedisChannelWriter<String, String> channelWriter = Connections.getChannelWriter(connection);

        sync.set(key, "1");
        assertThat(verificationConnection.get(key)).isEqualTo("1");

        final CountDownLatch block = new CountDownLatch(1);

        AsyncCommand<String, String, Object> command = new AsyncCommand<String, String, Object>(new Command<>(CommandType.INCR,
                new IntegerOutput(CODEC), new CommandArgs<>(CODEC).addKey(key))) {

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

        Channel channel = Connections.getChannel(connection);
        channel.unsafe().disconnect(channel.newPromise());

        assertThat(channel.isOpen()).isFalse();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isFalse();
        block.countDown();
        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isTrue();

        assertThat(verificationConnection.get(key)).isEqualTo("1");

        assertThat(getQueue(connection)).isEmpty();
        assertThat(getCommandBuffer(connection)).isEmpty();

        connection.close();
    }

    @Test
    public void commandFailsDuringDecode() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> sync = connection.sync();
        RedisChannelWriter<String, String> channelWriter = Connections.getChannelWriter(connection);
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        sync.set(key, "1");

        AsyncCommand<String, String, String> command = new AsyncCommand<>(new Command<>(CommandType.INCR, new StatusOutput<>(
                CODEC), new CommandArgs<>(CODEC).addKey(key)));

        channelWriter.write(command);

        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isFalse();
        assertThat(getException(command)).isInstanceOf(IllegalStateException.class);

        assertThat(verificationConnection.get(key)).isEqualTo("2");
        assertThat(sync.get(key)).isEqualTo("2");

        connection.close();
    }

    @Test
    public void noCommandsExecutedAfterConnectionIsDisconnected() throws Exception {

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

        try {

            Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

            connection2.sync().incr(key);
        } catch (Exception e) {
            assertThat(e).isExactlyInstanceOf(RedisException.class).hasMessageContaining("not connected");
        }

        connection2.close();
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
