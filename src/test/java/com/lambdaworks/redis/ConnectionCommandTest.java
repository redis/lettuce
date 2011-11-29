// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Test;

import static org.junit.Assert.*;

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
                    assertEquals("ERR operation not permitted", e.getMessage());
                    assertEquals("OK", connection.auth(passwd));
                    assertEquals("OK", connection.set(key, value));
                }
            }
        };
    }

    @Test
    public void echo() throws Exception {
        assertEquals("hello", redis.echo("hello"));
    }

    @Test
    public void ping() throws Exception {
        assertEquals("PONG", redis.ping());
    }

    @Test
    public void select() throws Exception {
        redis.set(key, value);
        assertEquals("OK", redis.select(1));
        assertNull(redis.get(key));
    }

    @Test
    public void authReconnect() throws Exception {
        new WithPasswordRequired() {
            @Override
            public void run(RedisClient client) {
                RedisConnection<String, String> connection = client.connect();
                assertEquals("OK", connection.auth(passwd));
                assertEquals("OK", connection.set(key, value));
                connection.quit();
                assertEquals(value, connection.get(key));
            }
        };
    }

    @Test
    public void selectReconnect() throws Exception {
        redis.select(1);
        redis.set(key, value);
        redis.quit();
        assertEquals(value, redis.get(key));
    }

    @Test(expected = RedisException.class)
    public void selectInvalid() throws Exception {
        redis.select(1024);
    }
}
