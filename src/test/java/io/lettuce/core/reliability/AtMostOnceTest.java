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

import static io.lettuce.test.ConnectionTestUtil.getCommandBuffer;
import static io.lettuce.test.ConnectionTestUtil.getStack;
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
import io.lettuce.test.Futures;
import io.lettuce.test.Wait;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.EncoderException;
import io.netty.util.Version;

/**
 * @author Mark Paluch
 */
@SuppressWarnings("rawtypes")
class AtMostOnceTest extends AbstractRedisClientTest {

    private String key = "key";

    @BeforeEach
    void before() {
        client.setOptions(ClientOptions.builder().autoReconnect(false).build());

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

        RedisCommands<String, String> connection = client.connect().sync();

        connection.set(key, "1");
        assertThat(connection.get("key")).isEqualTo("1");

        connection.getStatefulConnection().close();
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
        assertThat(getException(command)).isInstanceOf(IllegalStateException.class);

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

        StatefulRedisConnection<String, String> connection = client.connect();

        try {

            RedisAsyncCommands<String, String> async = connection.async();
            async.setAutoFlushCommands(false);
            async.quit();

            RedisFuture<Long> incr = async.incr(key);

            connection.flushCommands();

            Futures.await(incr);

        } catch (Exception e) {
            assertThat(e).hasRootCauseInstanceOf(RedisException.class).hasMessageContaining("Connection disconnected");
        }

        connection.close();
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
