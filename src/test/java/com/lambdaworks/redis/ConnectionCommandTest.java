// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ConnectionCommandTest extends AbstractCommandTest {
    @Test
    public void auth() throws Exception {
        RedisClient authClient = new RedisClient(authHost, authPort);
        RedisConnection<String, String> authCon = authClient.connect();

        try {
            authCon.info();
            fail("Server doesn't require authentication");
        } catch (RedisException e) {
            assertEquals("ERR operation not permitted", e.getMessage());
            assertEquals("OK", authCon.auth(passwd));
            assertEquals("OK", redis.set(key, value));
        } finally {
            authCon.close();
        }
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
        RedisClient authClient = new RedisClient(authHost, authPort);
        RedisConnection<String, String> authCon = authClient.connect();
        authCon.auth(passwd);
        assertEquals("OK", authCon.set(key, value));
        authCon.quit();
        assertEquals(value, authCon.get(key));
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
