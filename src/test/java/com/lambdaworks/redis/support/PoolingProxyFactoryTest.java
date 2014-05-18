package com.lambdaworks.redis.support;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.lambdaworks.redis.AbstractCommandTest;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionPool;

public class PoolingProxyFactoryTest extends AbstractCommandTest {

    @Test
    public void testCreateDefault() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        RedisConnection<String, String> connection = PoolingProxyFactory.create(pool);

        connection.set("a", "b");
        connection.set("x", "y");

        pool.close();
    }

    @Test
    public void testCreate() throws Exception {

        RedisConnection<String, String> connection = PoolingProxyFactory.create(client.pool(), 100, TimeUnit.MILLISECONDS);

        connection.set("a", "b");
        connection.close();
        Thread.sleep(110);

        connection.set("x", "y");
    }
}
