package com.lambdaworks.redis.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.ConnectionTestUtil;
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
public class AtLeastOnceTest extends AbstractRedisClientTest {

    protected final Utf8StringCodec CODEC = new Utf8StringCodec();
    protected String key = "key";

    @Before
    public void before() throws Exception {
        client.setOptions(ClientOptions.builder().autoReconnect(true).build());

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

        assertThat(ConnectionTestUtil.getConnectionState(connection)).isEqualTo("CONNECTED");

        connection.close();
    }

    @Test
    public void reconnectIsActiveHandler() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();

        ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection.getStatefulConnection());
        assertThat(connectionWatchdog).isNotNull();
        assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();
        assertThat(connectionWatchdog.isReconnectSuspended()).isFalse();

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

        RedisCommands<String, String> connection = client.connect().sync();

        connection.set(key, "1");

        assertThat(ConnectionTestUtil.getQueue(connection.getStatefulConnection())).isEmpty();
        assertThat(ConnectionTestUtil.getCommandBuffer(connection.getStatefulConnection())).isEmpty();

        connection.getStatefulConnection().close();
    }

    @Test
    public void commandIsExecutedOnce() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();

        connection.set(key, "1");
        connection.incr(key);
        assertThat(connection.get(key)).isEqualTo("2");

        connection.incr(key);
        assertThat(connection.get(key)).isEqualTo("3");

        connection.incr(key);
        assertThat(connection.get(key)).isEqualTo("4");

        connection.getStatefulConnection().close();
    }

    @Test
    public void commandFailsWhenFailOnEncode() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();
        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection.getStatefulConnection());
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        connection.set(key, "1");
        AsyncCommand<String, String, String> working = new AsyncCommand<>(
                new Command<>(CommandType.INCR, new IntegerOutput(CODEC), new CommandArgs<>(CODEC).addKey(key)));
        channelWriter.write(working);
        assertThat(working.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(connection.get(key)).isEqualTo("2");

        AsyncCommand<String, String, Object> command = new AsyncCommand(
                new Command<>(CommandType.INCR, new IntegerOutput(CODEC), new CommandArgs<>(CODEC).addKey(key))) {

            @Override
            public void encode(ByteBuf buf) {
                throw new IllegalStateException("I want to break free");
            }
        };

        channelWriter.write(command);

        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isFalse();
        assertThat(getException(command)).isInstanceOf(EncoderException.class);

        assertThat(verificationConnection.get(key)).isEqualTo("2");

        assertThat(ConnectionTestUtil.getQueue(connection.getStatefulConnection())).isNotEmpty();

        connection.getStatefulConnection().close();
    }

    @Test
    public void commandNotFailedChannelClosesWhileFlush() throws Exception {

        assumeTrue(Version.identify().get("netty-transport").artifactVersion().startsWith("4.0.2"));

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> verificationConnection = client.connect().sync();
        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection);

        RedisCommands<String, String> sync = connection.sync();
        sync.set(key, "1");
        assertThat(verificationConnection.get(key)).isEqualTo("1");

        final CountDownLatch block = new CountDownLatch(1);

        ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection);

        AsyncCommand<String, String, Object> command = getBlockOnEncodeCommand(block);

        channelWriter.write(command);

        connectionWatchdog.setReconnectSuspended(true);

        Channel channel = ConnectionTestUtil.getChannel(connection);
        channel.unsafe().disconnect(channel.newPromise());

        assertThat(channel.isOpen()).isFalse();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isFalse();
        block.countDown();
        assertThat(command.await(2, TimeUnit.SECONDS)).isFalse();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isFalse();

        assertThat(verificationConnection.get(key)).isEqualTo("1");

        assertThat(ConnectionTestUtil.getQueue(connection)).isEmpty();
        assertThat(ConnectionTestUtil.getCommandBuffer(connection)).isNotEmpty().contains(command);

        connection.close();
    }

    @Test
    public void commandRetriedChannelClosesWhileFlush() throws Exception {

        assumeTrue(Version.identify().get("netty-transport").artifactVersion().startsWith("4.0.2"));

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> sync = connection.sync();
        RedisCommands<String, String> verificationConnection = client.connect().sync();
        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection);

        sync.set(key, "1");
        assertThat(verificationConnection.get(key)).isEqualTo("1");

        final CountDownLatch block = new CountDownLatch(1);

        ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(sync.getStatefulConnection());

        AsyncCommand<String, String, Object> command = getBlockOnEncodeCommand(block);

        channelWriter.write(command);

        connectionWatchdog.setReconnectSuspended(true);

        Channel channel = ConnectionTestUtil.getChannel(sync.getStatefulConnection());
        channel.unsafe().disconnect(channel.newPromise());

        assertThat(channel.isOpen()).isFalse();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isFalse();
        block.countDown();
        assertThat(command.await(2, TimeUnit.SECONDS)).isFalse();

        connectionWatchdog.setReconnectSuspended(false);
        connectionWatchdog.scheduleReconnect();

        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isTrue();

        assertThat(verificationConnection.get(key)).isEqualTo("2");

        assertThat(ConnectionTestUtil.getQueue(sync.getStatefulConnection())).isEmpty();
        assertThat(ConnectionTestUtil.getCommandBuffer(sync.getStatefulConnection())).isEmpty();

        sync.getStatefulConnection().close();
        verificationConnection.getStatefulConnection().close();
    }

    protected AsyncCommand<String, String, Object> getBlockOnEncodeCommand(final CountDownLatch block) {
        return new AsyncCommand<String, String, Object>(
                new Command<>(CommandType.INCR, new IntegerOutput(CODEC), new CommandArgs<>(CODEC).addKey(key))) {

            @Override
            public void encode(ByteBuf buf) {
                try {
                    block.await();
                } catch (InterruptedException e) {
                }
                super.encode(buf);
            }
        };
    }

    @Test
    public void commandFailsDuringDecode() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();
        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection.getStatefulConnection());
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        connection.set(key, "1");

        AsyncCommand<String, String, String> command = new AsyncCommand(
                new Command<>(CommandType.INCR, new StatusOutput<>(CODEC), new CommandArgs<>(CODEC).addKey(key)));

        channelWriter.write(command);

        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isTrue();
        assertThat(getException(command)).isInstanceOf(IllegalStateException.class);

        assertThat(verificationConnection.get(key)).isEqualTo("2");
        assertThat(connection.get(key)).isEqualTo("2");

        connection.getStatefulConnection().close();
        verificationConnection.getStatefulConnection().close();
    }

    @Test
    public void commandCancelledOverSyncAPIAfterConnectionIsDisconnected() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> sync = connection.sync();
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        sync.set(key, "1");

        ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(sync.getStatefulConnection());
        connectionWatchdog.setListenOnChannelInactive(false);

        sync.quit();
        Wait.untilTrue(() -> !sync.getStatefulConnection().isOpen()).waitOrTimeout();

        try {
            sync.incr(key);
        } catch (RedisException e) {
            assertThat(e).isExactlyInstanceOf(RedisCommandTimeoutException.class);
        }

        assertThat(verificationConnection.get("key")).isEqualTo("1");

        assertThat(ConnectionTestUtil.getQueue(connection)).isEmpty();
        assertThat(ConnectionTestUtil.getCommandBuffer(connection).size()).isGreaterThan(0);

        connectionWatchdog.setListenOnChannelInactive(true);
        connectionWatchdog.scheduleReconnect();

        while (!ConnectionTestUtil.getCommandBuffer(connection).isEmpty() || !ConnectionTestUtil.getQueue(connection).isEmpty()) {
            Thread.sleep(10);
        }

        assertThat(sync.get(key)).isEqualTo("1");

        sync.getStatefulConnection().close();
        verificationConnection.getStatefulConnection().close();
    }

    @Test
    public void retryAfterConnectionIsDisconnected() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        connection.sync().set(key, "1");

        ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection);
        connectionWatchdog.setListenOnChannelInactive(false);

        connection.async().quit();
        while (connection.isOpen()) {
            Thread.sleep(100);
        }

        assertThat(connection.async().incr(key).await(1, TimeUnit.SECONDS)).isFalse();

        assertThat(verificationConnection.get("key")).isEqualTo("1");

        assertThat(ConnectionTestUtil.getQueue(connection)).isEmpty();
        assertThat(ConnectionTestUtil.getCommandBuffer(connection).size()).isGreaterThan(0);

        connectionWatchdog.setListenOnChannelInactive(true);
        connectionWatchdog.scheduleReconnect();

        while (!ConnectionTestUtil.getCommandBuffer(connection).isEmpty() || !ConnectionTestUtil.getQueue(connection).isEmpty()) {
            Thread.sleep(10);
        }

        assertThat(connection.sync().get(key)).isEqualTo("2");
        assertThat(verificationConnection.get(key)).isEqualTo("2");

        connection.close();
        verificationConnection.getStatefulConnection().close();
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
