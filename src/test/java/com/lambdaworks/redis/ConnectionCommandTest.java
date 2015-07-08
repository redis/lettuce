// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.lambdaworks.redis.protocol.CommandHandler;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectionCommandTest extends AbstractCommandTest {
    @Test
    public void auth() throws Exception {
        new WithPasswordRequired() {
            @Override
            public void run(RedisClient client) {
                RedisConnection<String, String> connection = client.connect();
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
                RedisConnection<String, String> authConnection = redisClient.connect();
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

    @Test
    public void authReconnect() throws Exception {
        new WithPasswordRequired() {
            @Override
            public void run(RedisClient client) {
                RedisConnection<String, String> connection = client.connect();
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

        RedisAsyncConnectionImpl<String, String> asyncConnection = (RedisAsyncConnectionImpl<String, String>) client
                .connectAsync();
        assertThat(Connections.isValid(asyncConnection)).isTrue();
        assertThat(Connections.isOpen(asyncConnection)).isTrue();
        assertThat(asyncConnection.isOpen()).isTrue();
        assertThat(asyncConnection.isClosed()).isFalse();

        CommandHandler<String, String> channelWriter = (CommandHandler<String, String>) asyncConnection.getChannelWriter();
        assertThat(channelWriter.isClosed()).isFalse();
        assertThat(channelWriter.isSharable()).isTrue();

        Connections.close(asyncConnection);
        assertThat(Connections.isOpen(asyncConnection)).isFalse();
        assertThat(Connections.isValid(asyncConnection)).isFalse();

        assertThat(asyncConnection.isOpen()).isFalse();
        assertThat(asyncConnection.isClosed()).isTrue();

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
    public void authInvalidPassword() throws Exception {
        RedisAsyncConnection<String, String> async = client.connectAsync();
        try {
            async.auth("invalid");
            fail("Authenticated with invalid password");
        } catch (RedisException e) {
            assertThat(e.getMessage()).isEqualTo("ERR Client sent AUTH, but no password is set");
            Field f = async.getClass().getDeclaredField("password");
            f.setAccessible(true);
            assertThat(f.get(async)).isNull();
        } finally {
            async.close();
        }
    }

    @Test
    public void selectInvalid() throws Exception {
        RedisAsyncConnection<String, String> async = client.connectAsync();
        try {
            async.select(1024);
            fail("Selected invalid db index");
        } catch (RedisException e) {
            assertThat(e.getMessage()).isEqualTo("ERR invalid DB index");
            Field f = async.getClass().getDeclaredField("db");
            f.setAccessible(true);
            assertThat(f.get(async)).isEqualTo(0);
        } finally {
            async.close();
        }
    }

    @Test
    public void string() throws Exception {

        assertThat(RedisAsyncConnectionImpl.string(1.1)).isEqualTo("1.1");
        assertThat(RedisAsyncConnectionImpl.string(Double.POSITIVE_INFINITY)).isEqualTo("+inf");
        assertThat(RedisAsyncConnectionImpl.string(Double.NEGATIVE_INFINITY)).isEqualTo("-inf");

    }
}
