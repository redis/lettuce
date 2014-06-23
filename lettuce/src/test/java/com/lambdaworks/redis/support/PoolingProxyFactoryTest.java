package com.lambdaworks.redis.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.lambdaworks.redis.AbstractCommandTest;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionPool;
import com.lambdaworks.redis.RedisException;

public class PoolingProxyFactoryTest extends AbstractCommandTest {

    @Test
    public void testCreateDefault() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        RedisConnection<String, String> connection = (RedisConnection<String, String>) PoolingProxyFactory.create(pool);

        connection.set("a", "b");
        connection.set("x", "y");

        pool.close();
    }

    @Test
    public void testCloseReturnsConnection() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        assertEquals(0, pool.getNumActive());
        RedisConnection<String, String> connection = pool.allocateConnection();
        assertEquals(1, pool.getNumActive());
        connection.close();
        assertEquals(0, pool.getNumActive());
    }

    @Test
    public void testCreate() throws Exception {

        RedisConnection<String, String> connection = (RedisConnection<String, String>) PoolingProxyFactory
                .create(client.pool());

        connection.set("a", "b");
        connection.close();

        try {
            connection.set("x", "y");
            fail("missing exception");
        } catch (RedisException e) {
            assertEquals("Connection pool is closed", e.getMessage());

        }
    }
}
