package com.lambdaworks.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PoolConnectionTest extends AbstractCommandTest {

    @Test
    public void twoConnections() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();
        RedisConnection<String, String> c2 = pool.allocateConnection();

        String result1 = c1.ping();
        String result2 = c2.ping();
        assertEquals("PONG", result1);
        assertEquals("PONG", result2);

    }

    @Test
    public void sameConnectionAfterFree() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();
        pool.freeConnection(c1);

        RedisConnection<String, String> c2 = pool.allocateConnection();
        assertSame(c1, c2);
    }

    @Test
    public void differentConnectionAfterClosedConnection() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();

        c1.ping();
        c1.close();

        try {
            c1.ping();
            fail("Missing Exception: Connection closed");
        } catch (Exception e) {
        }

        pool.freeConnection(c1);

        RedisConnection<String, String> c2 = pool.allocateConnection();
        assertNotSame(c1, c2);
    }

    @Test
    public void connectionsClosedAfterPoolClose() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();
        pool.freeConnection(c1);
        pool.close();

        try {
            c1.ping();
            fail("Missing Exception: Connection closed");
        } catch (Exception e) {
        }
    }

    @Test
    public void connectionNotClosedWhenBorrowed() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();
        pool.close();

        c1.ping();
    }

    @Test
    public void connectionNotClosedWhenBorrowed2() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();
        pool.freeConnection(c1);
        c1 = pool.allocateConnection();
        pool.close();

        c1.ping();
    }

}
