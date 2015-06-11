package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;

public class PoolConnectionTest extends AbstractRedisClientTest {

    @Test
    public void twoConnections() throws Exception {

        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        RedisCommands<String, String> c1 = pool.allocateConnection();
        RedisConnection<String, String> c2 = pool.allocateConnection();

        String result1 = c1.ping();
        String result2 = c2.ping();
        assertThat(result1).isEqualTo("PONG");
        assertThat(result2).isEqualTo("PONG");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getStatefulConnection() throws Exception {

        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        RedisCommands<String, String> c1 = pool.allocateConnection();

        c1.getStatefulConnection();
    }

    @Test
    public void sameConnectionAfterFree() throws Exception {
        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        RedisCommands<String, String> c1 = pool.allocateConnection();
        pool.freeConnection(c1);
        assertConnectionStillThere(c1);

        RedisConnection<String, String> c2 = pool.allocateConnection();
        assertThat(c2).isSameAs(c1);
    }

    @Test
    public void connectionCloseDoesNotClose() throws Exception {
        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();
        c1.close();
        RedisConnection<String, String> actualConnection1 = assertConnectionStillThere(c1);

        RedisConnection<String, String> c2 = pool.allocateConnection();
        assertThat(c2).isSameAs(c1);

        RedisConnection<String, String> actualConnection2 = assertConnectionStillThere(c2);
        assertThat(actualConnection1).isSameAs(actualConnection2);
    }

    @SuppressWarnings("unchecked")
    private RedisConnection<String, String> assertConnectionStillThere(RedisConnection<String, String> c1) {
        // unwrap code from RedisConnectionPool destroyObject
        if (Proxy.isProxyClass(c1.getClass())) {
            RedisConnectionPool.PooledConnectionInvocationHandler<RedisConnection<String, String>> invocationHandler;
            invocationHandler = (RedisConnectionPool.PooledConnectionInvocationHandler<RedisConnection<String, String>>) Proxy
                    .getInvocationHandler(c1);

            RedisConnection<String, String> connection = invocationHandler.getConnection();
            assertThat(connection).isNotNull();
            return connection;
        }
        return null;
    }

    @Test
    public void releaseConnectionWithClose() throws Exception {

        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();
        assertThat(pool.getNumActive()).isEqualTo(1);
        c1.close();
        assertThat(pool.getNumActive()).isEqualTo(0);

        pool.allocateConnection();
        assertThat(pool.getNumActive()).isEqualTo(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedAuthOnPooledConnection() throws Exception {

        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        pool.allocateConnection().auth("");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedSelectOnPooledConnection() throws Exception {

        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        pool.allocateConnection().select(99);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedQuitOnPooledConnection() throws Exception {

        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        pool.allocateConnection().quit();
    }

    @Test
    public void connectionsClosedAfterPoolClose() throws Exception {

        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        RedisCommands<String, String> c1 = pool.allocateConnection();
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

        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();
        pool.close();

        c1.ping();
    }

    @Test
    public void connectionNotClosedWhenBorrowed2() throws Exception {

        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        RedisCommands<String, String> c1 = pool.allocateConnection();
        pool.freeConnection(c1);
        c1 = pool.allocateConnection();
        pool.close();

        c1.ping();
    }

    @Test
    public void testResourceCleaning() throws Exception {

        RedisClient redisClient = getRedisClient();

        assertThat(redisClient.getChannelCount()).isEqualTo(0);
        assertThat(redisClient.getResourceCount()).isEqualTo(0);

        RedisConnectionPool<RedisAsyncCommands<String, String>> pool1 = redisClient.asyncPool();

        assertThat(redisClient.getChannelCount()).isEqualTo(0);
        assertThat(redisClient.getResourceCount()).isEqualTo(1);

        pool1.allocateConnection();

        assertThat(redisClient.getChannelCount()).isEqualTo(1);
        assertThat(redisClient.getResourceCount()).isEqualTo(3);

        RedisConnectionPool<RedisCommands<String, String>> pool2 = redisClient.pool();

        assertThat(redisClient.getResourceCount()).isEqualTo(4);

        pool2.allocateConnection();

        assertThat(redisClient.getResourceCount()).isEqualTo(6);

        redisClient.pool().close();
        assertThat(redisClient.getResourceCount()).isEqualTo(6);

        redisClient.shutdown();

        assertThat(redisClient.getChannelCount()).isEqualTo(0);
        assertThat(redisClient.getResourceCount()).isEqualTo(0);

    }

    @Test
    public void syncPoolPerformanceTest() throws Exception {

        RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();
        RedisConnection<String, String> c1 = pool.allocateConnection();

        c1.ping();
        Stopwatch stopwatch = Stopwatch.createStarted();

        for (int i = 0; i < 1000; i++) {
            c1.ping();
        }

        long elapsed = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);

        log.info("syncPoolPerformanceTest Duration: " + elapsed + "ms");

    }

    @Test
    public void asyncPoolPerformanceTest() throws Exception {

        RedisConnectionPool<RedisAsyncCommands<String, String>> pool = client.asyncPool();
        RedisAsyncConnection<String, String> c1 = pool.allocateConnection();

        c1.ping();
        Stopwatch stopwatch = Stopwatch.createStarted();

        for (int i = 0; i < 1000; i++) {
            c1.ping();
        }

        long elapsed = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);

        log.info("asyncPoolPerformanceTest Duration: " + elapsed + "ms");

    }

}
