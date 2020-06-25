/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.IntegerOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.lettuce.test.ConnectionTestUtil;
import io.lettuce.test.Delay;
import io.lettuce.test.Wait;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.EncoderException;
import io.netty.util.Version;

/**
 * @author Mark Paluch
 */
class AtLeastOnceTest extends AbstractRedisClientTest {

    private String key = "key";

    @BeforeEach
    void before() {
        client.setOptions(ClientOptions.builder().autoReconnect(true).build());

        // needs to be increased on slow systems...perhaps...
        client.setDefaultTimeout(3, TimeUnit.SECONDS);

        RedisCommands<String, String> connection = client.connect().sync();
        connection.flushall();
        connection.flushdb();
        connection.getStatefulConnection().close();
    }

    @Test
    void connectionIsConnectedAfterConnect() {

        StatefulRedisConnection<String, String> connection = client.connect();

        assertThat(ConnectionTestUtil.getConnectionState(connection)).isEqualTo("CONNECTED");

        connection.close();
    }

    @Test
    void reconnectIsActiveHandler() {

        RedisCommands<String, String> connection = client.connect().sync();

        ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection.getStatefulConnection());
        assertThat(connectionWatchdog).isNotNull();
        assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();
        assertThat(connectionWatchdog.isReconnectSuspended()).isFalse();

        connection.getStatefulConnection().close();
    }

    @Test
    void basicOperations() {

        RedisCommands<String, String> connection = client.connect().sync();

        connection.set(key, "1");
        assertThat(connection.get("key")).isEqualTo("1");

        connection.getStatefulConnection().close();
    }

    @Test
    void noBufferedCommandsAfterExecute() {

        RedisCommands<String, String> connection = client.connect().sync();

        connection.set(key, "1");

        assertThat(ConnectionTestUtil.getStack(connection.getStatefulConnection())).isEmpty();
        assertThat(ConnectionTestUtil.getCommandBuffer(connection.getStatefulConnection())).isEmpty();

        connection.getStatefulConnection().close();
    }

    @Test
    void commandIsExecutedOnce() {

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
    void commandFailsWhenFailOnEncode() {

        RedisCommands<String, String> connection = client.connect().sync();
        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection.getStatefulConnection());
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        connection.set(key, "1");
        AsyncCommand<String, String, String> working = new AsyncCommand<>(new Command<>(CommandType.INCR,
                new IntegerOutput(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).addKey(key)));
        channelWriter.write(working);
        assertThat(working.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(connection.get(key)).isEqualTo("2");

        AsyncCommand<String, String, Object> command = new AsyncCommand(new Command<>(CommandType.INCR,
                new IntegerOutput(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).addKey(key))) {

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

        assertThat(ConnectionTestUtil.getStack(connection.getStatefulConnection())).isNotEmpty();

        connection.getStatefulConnection().close();
    }

    @Test
    void commandNotFailedChannelClosesWhileFlush() {

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

        assertThat(ConnectionTestUtil.getStack(connection)).isEmpty();
        assertThat(ConnectionTestUtil.getCommandBuffer(connection)).isNotEmpty().contains(command);

        connection.close();
    }

    @Test
    void commandRetriedChannelClosesWhileFlush() {

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

        assertThat(ConnectionTestUtil.getStack(sync.getStatefulConnection())).isEmpty();
        assertThat(ConnectionTestUtil.getCommandBuffer(sync.getStatefulConnection())).isEmpty();

        sync.getStatefulConnection().close();
        verificationConnection.getStatefulConnection().close();
    }

    AsyncCommand<String, String, Object> getBlockOnEncodeCommand(final CountDownLatch block) {
        return new AsyncCommand<String, String, Object>(new Command<>(CommandType.INCR, new IntegerOutput(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey(key))) {

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
    void commandFailsDuringDecode() {

        RedisCommands<String, String> connection = client.connect().sync();
        RedisChannelWriter channelWriter = ConnectionTestUtil.getChannelWriter(connection.getStatefulConnection());
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        connection.set(key, "1");

        AsyncCommand<String, String, String> command = new AsyncCommand(new Command<>(CommandType.INCR,
                new StatusOutput<>(StringCodec.UTF8), new CommandArgs<>(StringCodec.UTF8).addKey(key)));

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
    void commandCancelledOverSyncAPIAfterConnectionIsDisconnected() {

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

        assertThat(ConnectionTestUtil.getDisconnectedBuffer(connection).size()).isGreaterThan(0);
        assertThat(ConnectionTestUtil.getCommandBuffer(connection)).isEmpty();

        connectionWatchdog.setListenOnChannelInactive(true);
        connectionWatchdog.scheduleReconnect();

        while (!ConnectionTestUtil.getCommandBuffer(connection).isEmpty()
                || !ConnectionTestUtil.getDisconnectedBuffer(connection).isEmpty()) {
            Delay.delay(Duration.ofMillis(10));
        }

        assertThat(sync.get(key)).isEqualTo("1");

        sync.getStatefulConnection().close();
        verificationConnection.getStatefulConnection().close();
    }

    @Test
    void retryAfterConnectionIsDisconnected() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        connection.sync().set(key, "1");

        ConnectionWatchdog connectionWatchdog = ConnectionTestUtil.getConnectionWatchdog(connection);
        connectionWatchdog.setListenOnChannelInactive(false);

        connection.async().quit();
        while (connection.isOpen()) {
            Delay.delay(Duration.ofMillis(100));
        }

        assertThat(connection.async().incr(key).await(1, TimeUnit.SECONDS)).isFalse();

        assertThat(verificationConnection.get("key")).isEqualTo("1");

        assertThat(ConnectionTestUtil.getDisconnectedBuffer(connection).size()).isGreaterThan(0);
        assertThat(ConnectionTestUtil.getCommandBuffer(connection)).isEmpty();

        connectionWatchdog.setListenOnChannelInactive(true);
        connectionWatchdog.scheduleReconnect();

        while (!ConnectionTestUtil.getCommandBuffer(connection).isEmpty()
                || !ConnectionTestUtil.getDisconnectedBuffer(connection).isEmpty()) {
            Delay.delay(Duration.ofMillis(10));
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
