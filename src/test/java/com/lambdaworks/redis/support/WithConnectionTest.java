/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.lambdaworks.redis.api.sync.RedisCommands;
import org.junit.Test;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisConnectionPool;

public class WithConnectionTest extends AbstractRedisClientTest {

    @Test
    public void testPooling() throws Exception {
        final RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();

        assertThat(pool.getNumActive()).isEqualTo(0);
        assertThat(pool.getNumIdle()).isEqualTo(0);

        new WithConnection<RedisCommands<String, String>>(pool) {

            @Override
            protected void run(RedisCommands<String, String> connection) {
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
        final RedisConnectionPool<RedisCommands<String, String>> pool = client.pool();

        assertThat(pool.getNumActive()).isEqualTo(0);
        assertThat(pool.getNumIdle()).isEqualTo(0);

        try {
            new WithConnection<RedisCommands<String, String>>(pool) {

                @Override
                protected void run(RedisCommands<String, String> connection) {
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
