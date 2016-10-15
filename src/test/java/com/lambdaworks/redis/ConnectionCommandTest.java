// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.assertj.core.api.Assertions;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.protocol.BaseRedisCommandBuilder;
import com.lambdaworks.redis.protocol.CommandHandler;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectionCommandTest extends AbstractRedisClientTest {
    @Test
    public void auth() throws Exception {
        new WithPasswordRequired() {
            @Override
            public void run(RedisClient client) {
                RedisConnection<String, String> connection = client.connect().sync();
                try {
                    connection.ping();
                    fail("Server doesn't require authentication");
                } catch (RedisException e) {
                    assertThat(e.getMessage()).isEqualTo("NOAUTH Authentication required.");
                    assertThat(connection.auth(passwd)).isEqualTo("OK");
                    assertThat(connection.set(key, value)).isEqualTo("OK");
                }

                RedisURI redisURI = RedisURI.Builder.redis(host, port).withDatabase(2).withPassword(passwd).build();
                RedisClient redisClient = new RedisClient(redisURI);
                RedisConnection<String, String> authConnection = redisClient.connect().sync();
                authConnection.ping();
                authConnection.close();
                FastShutdown.shutdown(redisClient);
            }
        };
    }

    @Test
    public void echo() throws Exception {
        assertThat(redis.echo("hello")).isEqualTo("hello");
    }

    @Test
    public void ping() throws Exception {
        assertThat(redis.ping()).isEqualTo("PONG");
    }

    @Test
    public void select() throws Exception {
        redis.set(key, value);
        assertThat(redis.select(1)).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void authNull() throws Exception {
        redis.auth(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void authEmpty() throws Exception {
        redis.auth("");
    }

    @Test
    public void authReconnect() throws Exception {
        new WithPasswordRequired() {
            @Override
            public void run(RedisClient client) {
                RedisConnection<String, String> connection = client.connect().sync();
                assertThat(connection.auth(passwd)).isEqualTo("OK");
                assertThat(connection.set(key, value)).isEqualTo("OK");
                connection.quit();
                assertThat(connection.get(key)).isEqualTo(value);
            }
        };
    }

    @Test
    public void selectReconnect() throws Exception {
        redis.select(1);
        redis.set(key, value);
        redis.quit();
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void isValid() throws Exception {

        assertThat(Connections.isValid(redis)).isTrue();
        RedisAsyncCommandsImpl<String, String> asyncConnection = (RedisAsyncCommandsImpl<String, String>) client.connectAsync();
        RedisChannelHandler<String, String> channelHandler = (RedisChannelHandler<String, String>) asyncConnection
                .getStatefulConnection();

        assertThat(Connections.isValid(asyncConnection)).isTrue();
        assertThat(Connections.isOpen(asyncConnection)).isTrue();
        assertThat(asyncConnection.isOpen()).isTrue();
        assertThat(channelHandler.isClosed()).isFalse();

        CommandHandler<String, String> channelWriter = (CommandHandler<String, String>) channelHandler.getChannelWriter();
        assertThat(channelWriter.isClosed()).isFalse();
        assertThat(channelWriter.isSharable()).isTrue();

        Connections.close(asyncConnection);
        assertThat(Connections.isOpen(asyncConnection)).isFalse();
        assertThat(Connections.isValid(asyncConnection)).isFalse();

        assertThat(asyncConnection.isOpen()).isFalse();
        assertThat(channelHandler.isClosed()).isTrue();

        assertThat(channelWriter.isClosed()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void isValidAsyncExceptions() throws Exception {

        RedisAsyncConnection<?, ?> connection = mock(RedisAsyncConnection.class);
        RedisFuture<String> future = mock(RedisFuture.class);
        when(connection.ping()).thenReturn(future);

        when(future.get()).thenThrow(new ExecutionException(new RuntimeException()));
        assertThat(Connections.isValid(connection)).isFalse();

    }

    @Test
    public void isValidSyncExceptions() throws Exception {

        RedisConnection<?, ?> connection = mock(RedisConnection.class);

        when(connection.ping()).thenThrow(new RuntimeException());
        assertThat(Connections.isValid(connection)).isFalse();
    }

    @Test
    public void closeExceptions() throws Exception {

        RedisConnection<?, ?> connection = mock(RedisConnection.class);
        doThrow(new RuntimeException()).when(connection).close();
        Connections.close(connection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void isValidWrongObject() throws Exception {
        Connections.isValid(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void isOpenWrongObject() throws Exception {
        Connections.isOpen(new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void closeWrongObject() throws Exception {
        Connections.close(new Object());
    }

    @Test
    public void getSetReconnect() throws Exception {
        redis.set(key, value);
        redis.quit();
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void authInvalidPassword() throws Exception {
        RedisAsyncConnection<String, String> async = client.connectAsync();
        try {
            async.auth("invalid");
            fail("Authenticated with invalid password");
        } catch (RedisException e) {
            assertThat(e.getMessage()).isEqualTo("ERR Client sent AUTH, but no password is set");
            StatefulRedisConnection<String, String> statefulRedisConnection = (StatefulRedisConnection<String, String>) ReflectionTestUtils
                    .getField(async, "connection");
            assertThat(ReflectionTestUtils.getField(statefulRedisConnection, "password")).isNull();
        } finally {
            async.close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void selectInvalid() throws Exception {
        RedisAsyncConnection<String, String> async = client.connectAsync();
        try {
            async.select(1024);
            fail("Selected invalid db index");
        } catch (RedisException e) {
            assertThat(e.getMessage()).startsWith("ERR");
            StatefulRedisConnection<String, String> statefulRedisConnection = (StatefulRedisConnection<String, String>) ReflectionTestUtils
                    .getField(async, "connection");
            assertThat(ReflectionTestUtils.getField(statefulRedisConnection, "db")).isEqualTo(0);
        } finally {
            async.close();
        }
    }

    @Test
    public void testDoubleToString() throws Exception {

        assertThat(LettuceStrings.string(1.1)).isEqualTo("1.1");
        assertThat(LettuceStrings.string(Double.POSITIVE_INFINITY)).isEqualTo("+inf");
        assertThat(LettuceStrings.string(Double.NEGATIVE_INFINITY)).isEqualTo("-inf");

    }
}
