package com.lambdaworks.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.base.Stopwatch;

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
    public void releaseConnectionWithClose() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();
        c1.close();

        RedisConnection<String, String> c2 = pool.allocateConnection();
        assertSame(c1, c2);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedAuthOnPooledConnection() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        pool.allocateConnection().auth("");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedSelectOnPooledConnection() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        pool.allocateConnection().select(99);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedQuitOnPooledConnection() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        pool.allocateConnection().quit();
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

    @Test
    public void testResourceCleaning() throws Exception {

        RedisClient redisClient = getRedisClient();

        assertEquals(0, redisClient.getChannelCount());
        assertEquals(0, redisClient.getResourceCount());

        RedisConnectionPool<RedisAsyncConnection<String, String>> pool1 = redisClient.asyncPool();

        assertEquals(0, redisClient.getChannelCount());
        assertEquals(1, redisClient.getResourceCount());

        pool1.allocateConnection();

        assertEquals(1, redisClient.getChannelCount());
        assertEquals(2, redisClient.getResourceCount());

        RedisConnectionPool<RedisConnection<String, String>> pool2 = redisClient.pool();

        assertEquals(3, redisClient.getResourceCount());

        pool2.allocateConnection();

        assertEquals(4, redisClient.getResourceCount());

        redisClient.pool().close();
        assertEquals(4, redisClient.getResourceCount());

        redisClient.shutdown();

        assertEquals(0, redisClient.getChannelCount());
        assertEquals(0, redisClient.getResourceCount());

    }

    @Test
    public void syncPoolPerformanceTest() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();

        Stopwatch stopwatch = Stopwatch.createStarted();

        for (int i = 0; i < 1000; i++) {
            c1.ping();
        }

        long elapsed = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);

        System.out.println("syncPoolPerformanceTest Duration: " + elapsed + "ms");

    }

    @Test
    public void asyncPoolPerformanceTest() throws Exception {

        RedisConnectionPool<RedisAsyncConnection<String, String>> pool = client.asyncPool();
        RedisAsyncConnection<String, String> c1 = pool.allocateConnection();

        Stopwatch stopwatch = Stopwatch.createStarted();

        for (int i = 0; i < 1000; i++) {
            c1.ping();
        }

        long elapsed = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);

        System.out.println("asyncPoolPerformanceTest Duration: " + elapsed + "ms");

    }

}
