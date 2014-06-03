package com.lambdaworks.redis.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.lambdaworks.redis.AbstractCommandTest;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionPool;

public class WithConnectionTest extends AbstractCommandTest {

    @Test
    public void testPooling() throws Exception {
        final RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();

        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());

        new WithConnection<RedisConnection<String, String>>(pool) {

            @Override
            protected void run(RedisConnection<String, String> connection) {
                connection.set("key", "value");
                String result = connection.get("key");
                assertEquals("value", result);

                assertEquals(1, pool.getNumActive());
                assertEquals(0, pool.getNumIdle());
            }
        };

        assertEquals(0, pool.getNumActive());
        assertEquals(1, pool.getNumIdle());

    }

    @Test
    public void testPoolingWithException() throws Exception {
        final RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();

        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());

        try {
            new WithConnection<RedisConnection<String, String>>(pool) {

                @Override
                protected void run(RedisConnection<String, String> connection) {
                    connection.set("key", "value");
                    throw new IllegalStateException("test");
                }
            };

            fail("Missing Exception");
        } catch (Exception e) {
        }

        assertEquals(0, pool.getNumActive());
        assertEquals(1, pool.getNumIdle());

    }
}
