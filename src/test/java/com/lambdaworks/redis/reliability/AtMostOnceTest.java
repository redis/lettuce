package com.lambdaworks.redis.reliability;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.lambdaworks.redis.output.StatusOutput;
import io.netty.handler.codec.EncoderException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.AbstractCommandTest;
import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.IntegerOutput;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandOutput;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 03.07.15 13:40
 */
public class AtMostOnceTest extends AbstractCommandTest {

    protected final Utf8StringCodec CODEC = new Utf8StringCodec();
    protected String key = "key";

    @Before
    public void before() throws Exception {
        client.setOptions(new ClientOptions.Builder().autoReconnect(false).build());

        // needs to be increased on slow systems...perhaps...
        client.setDefaultTimeout(3, TimeUnit.SECONDS);

        RedisConnection<String, String> connection = client.connect();
        connection.flushall();
        connection.flushdb();
        connection.close();
    }

    @Test
    public void connectionIsConnectedAfterConnect() throws Exception {

        RedisConnection<String, String> connection = client.connect();

        assertThat(getConnectionState(getRedisChannelHandler(connection)));

        connection.close();
    }

    @Test
    public void noReconnectHandler() throws Exception {

        RedisConnection<String, String> connection = client.connect();

        assertThat(getHandler(RedisChannelWriter.class, getRedisChannelHandler(connection))).isNotNull();
        assertThat(getHandler(ConnectionWatchdog.class, getRedisChannelHandler(connection))).isNull();

        connection.close();
    }

    @Test
    public void basicOperations() throws Exception {

        RedisConnection<String, String> connection = client.connect();

        connection.set(key, "1");
        assertThat(connection.get("key")).isEqualTo("1");

        connection.close();
    }

    @Test
    public void noBufferedCommandsAfterExecute() throws Exception {

        RedisConnection<String, String> connection = client.connect();

        connection.set(key, "1");

        assertThat(getQueue(getRedisChannelHandler(connection))).isEmpty();
        assertThat(getCommandBuffer(getRedisChannelHandler(connection))).isEmpty();

        connection.close();
    }

    @Test
    public void commandIsExecutedOnce() throws Exception {

        RedisConnection<String, String> connection = client.connect();

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
    public void commandNotExecutedFailsOnEncode() throws Exception {

        RedisConnection<String, String> connection = client.connect();
        RedisChannelWriter<String, String> channelWriter = getRedisChannelHandler(connection).getChannelWriter();

        connection.set(key, "1");
        Command<String, String, String> working = new Command<String, String, String>(CommandType.INCR,
                new IntegerOutput(CODEC), new CommandArgs<String, String>(CODEC).addKey(key));
        channelWriter.write(working);
        assertThat(working.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(connection.get(key)).isEqualTo("2");

        Command<String, String, Object> command = new Command<String, String, Object>(CommandType.INCR,
                new IntegerOutput(CODEC), new CommandArgs<String, String>(CODEC).addKey(key)) {

            @Override
            public void encode(ByteBuf buf) {
                throw new IllegalStateException("I want to break free");
            }
        };

        channelWriter.write(command);

        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isTrue();
        assertThat(command.isDone()).isTrue();
        assertThat(command.getException()).isInstanceOf(EncoderException.class);

        assertThat(connection.get(key)).isEqualTo("2");

        assertThat(getQueue(getRedisChannelHandler(connection))).isEmpty();
        assertThat(getCommandBuffer(getRedisChannelHandler(connection))).isEmpty();

        connection.close();
    }

    @Test
    public void commandNotExecutedChannelClosesWhileFlush() throws Exception {

        RedisConnection<String, String> connection = client.connect();
        RedisConnection<String, String> verificationConnection = client.connect();
        RedisChannelWriter<String, String> channelWriter = getRedisChannelHandler(connection).getChannelWriter();

        connection.set(key, "1");
        assertThat(verificationConnection.get(key)).isEqualTo("1");

        final CountDownLatch block = new CountDownLatch(1);

        Command<String, String, Object> command = new Command<String, String, Object>(CommandType.INCR,
                new IntegerOutput(CODEC), new CommandArgs<String, String>(CODEC).addKey(key)) {

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

        Channel channel = getChannel(getRedisChannelHandler(connection));
        channel.unsafe().disconnect(channel.newPromise());

        assertThat(channel.isOpen()).isFalse();
        assertThat(command.isCancelled()).isFalse();
        assertThat(command.isDone()).isFalse();
        block.countDown();
        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isTrue();
        assertThat(command.isDone()).isTrue();

        assertThat(verificationConnection.get(key)).isEqualTo("1");

        assertThat(getQueue(getRedisChannelHandler(connection))).isEmpty();
        assertThat(getCommandBuffer(getRedisChannelHandler(connection))).isEmpty();

        connection.close();
    }

    @Test
    public void commandFailsDuringDecode() throws Exception {

        RedisConnection<String, String> connection = client.connect();
        RedisChannelWriter<String, String> channelWriter = getRedisChannelHandler(connection).getChannelWriter();
        RedisConnection<String, String> verificationConnection = client.connect();

        connection.set(key, "1");

        Command<String, String, String> command = new Command<String, String, String>(CommandType.INCR,
                new StatusOutput<String, String>(CODEC), new CommandArgs<String, String>(CODEC).addKey(key));

        channelWriter.write(command);

        assertThat(command.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(command.isCancelled()).isTrue();
        assertThat(command.isDone()).isTrue();
        assertThat(command.getException()).isInstanceOf(IllegalStateException.class);

        assertThat(verificationConnection.get(key)).isEqualTo("2");
        assertThat(connection.get(key)).isEqualTo("2");

        connection.close();
    }

    @Test
    public void noCommandsExecutedAfterConnectionIsDisconnected() throws Exception {

        RedisConnection<String, String> connection = client.connect();

        connection.quit();

        try {
            connection.incr(key);
        } catch (RedisException e) {
            assertThat(e).isInstanceOf(RedisException.class);
        }

        connection.close();

        connection = client.connect();
        connection.quit();

        try {

            while (connection.isOpen()) {
                Thread.sleep(100);
            }
            connection.incr(key);
        } catch (Exception e) {
            assertThat(e).isExactlyInstanceOf(RedisException.class).hasMessageContaining("reconnect is disabled");
        }

        connection.close();
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

    private Queue<?> getQueue(RedisChannelHandler<?, ?> channelHandler) {
        return (Queue<?>) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "queue");
    }

    private Queue<?> getCommandBuffer(RedisChannelHandler<?, ?> channelHandler) {
        return (Queue<?>) ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "commandBuffer");
    }

    private String getConnectionState(RedisChannelHandler<?, ?> channelHandler) {
        return ReflectionTestUtils.getField(channelHandler.getChannelWriter(), "lifecycleState").toString();
    }
}
