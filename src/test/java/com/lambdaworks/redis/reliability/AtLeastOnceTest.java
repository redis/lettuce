/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.reliability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.Connections;
import com.lambdaworks.Wait;
import com.lambdaworks.redis.*;
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
        connection.close();
    }

    @Test
    public void connectionIsConnectedAfterConnect() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();

        assertThat(getConnectionState(getRedisChannelHandler(connection)));

        connection.close();
    }

    @Test
    public void reconnectIsActiveHandler() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();

        ConnectionWatchdog connectionWatchdog = Connections.getConnectionWatchdog(connection.getStatefulConnection());
        assertThat(connectionWatchdog).isNotNull();
        assertThat(connectionWatchdog.isListenOnChannelInactive()).isTrue();
        assertThat(connectionWatchdog.isReconnectSuspended()).isFalse();

        connection.close();
    }

    @Test
    public void basicOperations() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();

        connection.set(key, "1");
        assertThat(connection.get("key")).isEqualTo("1");

        connection.close();
    }

    @Test
    public void noBufferedCommandsAfterExecute() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();

        connection.set(key, "1");

        assertThat(getDisconnectedBuffer(getRedisChannelHandler(connection))).isEmpty();
        assertThat(getCommandBuffer(getRedisChannelHandler(connection))).isEmpty();

        connection.close();
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

        connection.close();
    }

    @Test
    public void commandFailsWhenFailOnEncode() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();
        RedisChannelWriter<String, String> channelWriter = getRedisChannelHandler(connection).getChannelWriter();
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        connection.set(key, "1");
        AsyncCommand<String, String, String> working = new AsyncCommand<>(new Command<>(CommandType.INCR, new IntegerOutput(
                CODEC), new CommandArgs<>(CODEC).addKey(key)));
        channelWriter.write(working);
        assertThat(working.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(connection.get(key)).isEqualTo("2");

        AsyncCommand<String, String, Object> command = new AsyncCommand(new Command<>(CommandType.INCR,
                new IntegerOutput(CODEC), new CommandArgs<>(CODEC).addKey(key))) {

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

        assertThat(getStack(getRedisChannelHandler(connection))).isNotEmpty();
        assertThat(getDisconnectedBuffer(getRedisChannelHandler(connection))).isEmpty();

        connection.close();
    }

    @Test
    public void commandNotFailedChannelClosesWhileFlush() throws Exception {

        assumeTrue(Version.identify().get("netty-transport").artifactVersion().startsWith("4.0.2"));

        RedisCommands<String, String> connection = client.connect().sync();
        RedisCommands<String, String> verificationConnection = client.connect().sync();
        RedisChannelWriter<String, String> channelWriter = getRedisChannelHandler(connection).getChannelWriter();

        connection.set(key, "1");
        assertThat(verificationConnection.get(key)).isEqualTo("1");

        final CountDownLatch block = new CountDownLatch(1);

        ConnectionWatchdog connectionWatchdog = Connections.getConnectionWatchdog(connection.getStatefulConnection());

        AsyncCommand<String, String, Object> command = getBlockOnEncodeCommand(block);

        channelWriter.write(command);

        connectionWatchdog.setReconnectSuspended(true);

        Channel channel = getChannel(getRedisChannelHandler(connection));
        channel.unsafe().disconnect(channel.newPromise());

        assertThat(channel.isOpen()).isFalse();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isFalse();
        block.countDown();
        assertThat(command.await(2, TimeUnit.SECONDS)).isFalse();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isFalse();

        assertThat(verificationConnection.get(key)).isEqualTo("1");

        assertThat(getDisconnectedBuffer(getRedisChannelHandler(connection))).isEmpty();
        assertThat(getCommandBuffer(getRedisChannelHandler(connection))).isNotEmpty().contains(command);

        connection.close();
    }

    @Test
    public void commandRetriedChannelClosesWhileFlush() throws Exception {

        assumeTrue(Version.identify().get("netty-transport").artifactVersion().startsWith("4.0.2"));

        RedisCommands<String, String> connection = client.connect().sync();
        RedisCommands<String, String> verificationConnection = client.connect().sync();
        RedisChannelWriter<String, String> channelWriter = getRedisChannelHandler(connection).getChannelWriter();

        connection.set(key, "1");
        assertThat(verificationConnection.get(key)).isEqualTo("1");

        final CountDownLatch block = new CountDownLatch(1);

        ConnectionWatchdog connectionWatchdog = Connections.getConnectionWatchdog(connection.getStatefulConnection());

        AsyncCommand<String, String, Object> command = getBlockOnEncodeCommand(block);

        channelWriter.write(command);

        connectionWatchdog.setReconnectSuspended(true);

        Channel channel = getChannel(getRedisChannelHandler(connection));
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

        assertThat(getDisconnectedBuffer(getRedisChannelHandler(connection))).isEmpty();
        assertThat(getCommandBuffer(getRedisChannelHandler(connection))).isEmpty();

        connection.close();
        verificationConnection.close();
    }

    protected AsyncCommand<String, String, Object> getBlockOnEncodeCommand(final CountDownLatch block) {
        return new AsyncCommand<String, String, Object>(new Command<>(CommandType.INCR, new IntegerOutput(CODEC),
                new CommandArgs<>(CODEC).addKey(key))) {

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
        RedisChannelWriter<String, String> channelWriter = getRedisChannelHandler(connection).getChannelWriter();
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        connection.set(key, "1");

        AsyncCommand<String, String, String> command = new AsyncCommand(new Command<>(CommandType.INCR, new StatusOutput<>(
                CODEC), new CommandArgs<>(CODEC).addKey(key)));

        channelWriter.write(command);

        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isTrue();
        assertThat(getException(command)).isInstanceOf(IllegalStateException.class);

        assertThat(verificationConnection.get(key)).isEqualTo("2");
        assertThat(connection.get(key)).isEqualTo("2");

        connection.close();
        verificationConnection.close();
    }

    @Test
    public void commandCancelledOverSyncAPIAfterConnectionIsDisconnected() throws Exception {

        RedisCommands<String, String> connection = client.connect().sync();
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        connection.set(key, "1");

        ConnectionWatchdog connectionWatchdog = Connections.getConnectionWatchdog(connection.getStatefulConnection());
        connectionWatchdog.setListenOnChannelInactive(false);

        connection.quit();
        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

        try {
            connection.incr(key);
        } catch (RedisException e) {
            assertThat(e).isExactlyInstanceOf(RedisCommandTimeoutException.class);
        }

        assertThat(verificationConnection.get("key")).isEqualTo("1");

        assertThat(getStack(getRedisChannelHandler(connection))).isEmpty();
        assertThat(getDisconnectedBuffer(getRedisChannelHandler(connection)).size()).isGreaterThan(0);

        connectionWatchdog.setListenOnChannelInactive(true);
        connectionWatchdog.scheduleReconnect();

        while (!getCommandBuffer(getRedisChannelHandler(connection)).isEmpty()
                || !getDisconnectedBuffer(getRedisChannelHandler(connection)).isEmpty()) {
            Thread.sleep(10);
        }

        assertThat(connection.get(key)).isEqualTo("1");

        connection.close();
        verificationConnection.close();
    }

    @Test
    public void retryAfterConnectionIsDisconnected() throws Exception {

        RedisAsyncConnection<String, String> connection = client.connectAsync();
        RedisChannelHandler<String, String> redisChannelHandler = (RedisChannelHandler) connection.getStatefulConnection();
        RedisCommands<String, String> verificationConnection = client.connect().sync();

        connection.set(key, "1").get();

        ConnectionWatchdog connectionWatchdog = Connections.getConnectionWatchdog(connection.getStatefulConnection());
        connectionWatchdog.setListenOnChannelInactive(false);

        connection.quit();
        while (connection.isOpen()) {
            Thread.sleep(100);
        }

        assertThat(connection.incr(key).await(1, TimeUnit.SECONDS)).isFalse();

        assertThat(verificationConnection.get("key")).isEqualTo("1");

        assertThat(getStack(redisChannelHandler)).isEmpty();
        assertThat(getDisconnectedBuffer(redisChannelHandler)).isNotEmpty();
        assertThat(getCommandBuffer(redisChannelHandler)).isEmpty();

        connectionWatchdog.setListenOnChannelInactive(true);
        connectionWatchdog.scheduleReconnect();

        while (!getCommandBuffer(redisChannelHandler).isEmpty() || !getDisconnectedBuffer(redisChannelHandler).isEmpty()) {
            Thread.sleep(10);
        }

        assertThat(connection.get(key).get()).isEqualTo("2");
        assertThat(verificationConnection.get(key)).isEqualTo("2");

        connection.close();
        verificationConnection.close();
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

    private <K, V> RedisChannelHandler<K, V> getRedisChannelHandler(RedisConnection<K, V> sync) {

        InvocationHandler invocationHandler = Proxy.getInvocationHandler(sync);
        return (RedisChannelHandler<K, V>) ReflectionTestUtils.getField(invocationHandler, "connection");
    }

    private <T> T getHandler(Class<T> handlerType, RedisChannelHandler<?, ?> channelHandler) {
        Channel channel = getChannel(channelHandler);
        return (T) channel.pipeline().get((Class) handlerType);
    }

    private Channel getChannel(RedisChannelHandler<?, ?> channelHandler) {
        return (Channel) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "channel");
    }

    private Queue<Object> getDisconnectedBuffer(RedisChannelHandler<?, ?> channelHandler) {
        return (Queue<Object>) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "disconnectedBuffer");
    }

    private Queue<Object> getCommandBuffer(RedisChannelHandler<?, ?> channelHandler) {
        return (Queue<Object>) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "commandBuffer");
    }

    private Queue<Object> getStack(RedisChannelHandler<?, ?> channelHandler) {
        return (Queue<Object>) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "stack");
    }

    private String getConnectionState(RedisChannelHandler<?, ?> channelHandler) {
        return ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "lifecycleState").toString();
    }
}
