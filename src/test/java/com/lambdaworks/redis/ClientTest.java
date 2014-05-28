// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.TimeUnit;

public class ClientTest extends AbstractCommandTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test(expected = RedisException.class)
    public void close() throws Exception {
        redis.close();
        redis.get(key);
    }

    @Test(expected = RedisException.class)
    @Ignore
    public void shutdown() throws Exception {
        RedisClient client = new RedisClient(host);
        RedisConnection<String, String> connection = client.connect();

        assertTrue(connection.isOpen());
        client.shutdown();

        assertFalse(connection.isOpen());
        connection.get(key);
    }

    @Test
    public void listenerTest() throws Exception {

        TestConnectionListener listener = new TestConnectionListener();

        RedisClient client = new RedisClient(host);
        client.addListener(listener);

        assertNull(listener.onConnected);
        assertNull(listener.onDisconnected);
        assertNull(listener.onException);

        RedisAsyncConnection<String, String> connection = client.connectAsync();

        Thread.sleep(100);

        assertEquals(connection, listener.onConnected);
        assertNull(listener.onDisconnected);

        connection.set(key, value).get();
        connection.close();
        Thread.sleep(100);

        assertEquals(connection, listener.onConnected);
        assertEquals(connection, listener.onDisconnected);

    }

    @Test(expected = RedisException.class, timeout = 100)
    public void timeout() throws Exception {
        redis.setTimeout(0, TimeUnit.MICROSECONDS);
        redis.get(key);
    }

    @Test
    public void reconnect() throws Exception {
        redis.set(key, value);
        redis.quit();
        assertEquals(value, redis.get(key));
        redis.quit();
        assertEquals(value, redis.get(key));
        redis.quit();
        assertEquals(value, redis.get(key));
    }

    @Test(expected = RedisCommandInterruptedException.class, timeout = 10)
    public void interrupt() throws Exception {
        Thread.currentThread().interrupt();
        redis.blpop(0, key);
    }

    @Test
    public void connectFailure() throws Exception {
        RedisClient client = new RedisClient("invalid");
        exception.expect(RedisException.class);
        exception.expectMessage("Unable to connect");
        client.connect();
    }

    @Test
    public void connectPubSubFailure() throws Exception {
        RedisClient client = new RedisClient("invalid");
        exception.expect(RedisException.class);
        exception.expectMessage("Unable to connect");
        client.connectPubSub();
    }

    private class TestConnectionListener implements RedisConnectionStateListener {

        public RedisChannelHandler onConnected;
        public RedisChannelHandler onDisconnected;
        public RedisChannelHandler onException;

        @Override
        public void onRedisConnected(RedisChannelHandler connection) {
            onConnected = connection;
        }

        @Override
        public void onRedisDisconnected(RedisChannelHandler connection) {
            onDisconnected = connection;
        }

        @Override
        public void onRedisExceptionCaught(RedisChannelHandler connection, Throwable cause) {
            onException = connection;

        }
    }
}
