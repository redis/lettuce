// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.assertj.core.api.Assertions;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectionCommandTest extends AbstractRedisClientTest {
    @Test
    public void auth() throws Exception {
        new WithPasswordRequired() {
            @Override
            public void run(RedisClient client) {
                RedisCommands<String, String> connection = client.connect().sync();
                try {
                    connection.ping();
                    fail("Server doesn't require authentication");
                } catch (RedisException e) {
                    assertThat(e.getMessage()).isEqualTo("NOAUTH Authentication required.");
                    assertThat(connection.auth(passwd)).isEqualTo("OK");
                    assertThat(connection.set(key, value)).isEqualTo("OK");
                }

                RedisURI redisURI = RedisURI.Builder.redis(host, port).withDatabase(2).withPassword(passwd).build();
                RedisClient redisClient = DefaultRedisClient.get();
                RedisCommands<String, String> authConnection = redisClient.connect(redisURI).sync();
                authConnection.ping();
                authConnection.getStatefulConnection().close();
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
                RedisCommands<String, String> connection = client.connect().sync();
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
    public void getSetReconnect() throws Exception {
        redis.set(key, value);
        redis.quit();
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void authInvalidPassword() throws Exception {
        RedisAsyncCommands<String, String> async = client.connect().async();
        try {
            async.auth("invalid");
            fail("Authenticated with invalid password");
        } catch (RedisException e) {
            assertThat(e.getMessage()).isEqualTo("ERR Client sent AUTH, but no password is set");
            StatefulRedisConnection<String, String> statefulRedisCommands = async.getStatefulConnection();
            assertThat(ReflectionTestUtils.getField(statefulRedisCommands, "password")).isNull();
        } finally {
            async.getStatefulConnection().close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void selectInvalid() throws Exception {
        RedisAsyncCommands<String, String> async = client.connect().async();
        try {
            async.select(1024);
            fail("Selected invalid db index");
        } catch (RedisException e) {
            assertThat(e.getMessage()).isEqualTo("ERR invalid DB index");
            StatefulRedisConnection<String, String> statefulRedisCommands = async.getStatefulConnection();
            assertThat(ReflectionTestUtils.getField(statefulRedisCommands, "db")).isEqualTo(0);
        } finally {
            async.getStatefulConnection().close();
        }
    }

    @Test
    public void testDoubleToString() throws Exception {

        assertThat(LettuceStrings.string(1.1)).isEqualTo("1.1");
        assertThat(LettuceStrings.string(Double.POSITIVE_INFINITY)).isEqualTo("+inf");
        assertThat(LettuceStrings.string(Double.NEGATIVE_INFINITY)).isEqualTo("-inf");

    }
}
