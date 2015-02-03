package com.lambdaworks.redis.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.Test;

import com.lambdaworks.redis.AbstractCommandTest;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionPool;
import com.lambdaworks.redis.RedisException;

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
    public void testCloseReturnsConnection() throws Exception {

        RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();
        assertThat(pool.getNumActive()).isEqualTo(0);
        RedisConnection<String, String> connection = pool.allocateConnection();
        assertThat(pool.getNumActive()).isEqualTo(1);
        connection.close();
        assertThat(pool.getNumActive()).isEqualTo(0);
    }

    @Test
    public void testCreate() throws Exception {

        RedisConnection<String, String> connection = PoolingProxyFactory
                .create(client.pool());

        connection.set("a", "b");
        connection.close();

        try {
            connection.set("x", "y");
            fail("missing exception");
        } catch (RedisException e) {
            assertThat(e.getMessage()).isEqualTo("Connection pool is closed");

        }
    }
}
