package com.lambdaworks.redis.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.Test;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionPool;

public class WithConnectionTest extends AbstractRedisClientTest {

    @Test
    public void testPooling() throws Exception {
        final RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();

        assertThat(pool.getNumActive()).isEqualTo(0);
        assertThat(pool.getNumIdle()).isEqualTo(0);

        new WithConnection<RedisConnection<String, String>>(pool) {

            @Override
            protected void run(RedisConnection<String, String> connection) {
                connection.set("key", "value");
                String result = connection.get("key");
                assertThat(result).isEqualTo("value");

                assertThat(pool.getNumActive()).isEqualTo(1);
                assertThat(pool.getNumIdle()).isEqualTo(0);
            }
        };

        assertThat(pool.getNumActive()).isEqualTo(0);
        assertThat(pool.getNumIdle()).isEqualTo(1);

    }

    @Test
    public void testPoolingWithException() throws Exception {
        final RedisConnectionPool<RedisConnection<String, String>> pool = client.pool();

        assertThat(pool.getNumActive()).isEqualTo(0);
        assertThat(pool.getNumIdle()).isEqualTo(0);

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

        assertThat(pool.getNumActive()).isEqualTo(0);
        assertThat(pool.getNumIdle()).isEqualTo(1);

    }
}
