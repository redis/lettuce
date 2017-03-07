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
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

/**
 * @author Mark Paluch
 */
@SuppressWarnings("rawtypes")
public class PipeliningTest extends AbstractRedisClientTest {

    @Test
    public void basic() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();
        connection.setAutoFlushCommands(false);

        int iterations = 100;
        List<RedisFuture<?>> futures = triggerSet(connection.async(), iterations);

        verifyNotExecuted(iterations);

        connection.flushCommands();

        LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]));

        verifyExecuted(iterations);

        connection.close();
    }

    protected void verifyExecuted(int iterations) {
        for (int i = 0; i < iterations; i++) {
            assertThat(redis.get(key(i))).as("Key " + key(i) + " must be " + value(i)).isEqualTo(value(i));
        }
    }

    @Test
    public void setAutoFlushTrueDoesNotFlush() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();
        connection.setAutoFlushCommands(false);

        int iterations = 100;
        List<RedisFuture<?>> futures = triggerSet(connection.async(), iterations);

        verifyNotExecuted(iterations);

        connection.setAutoFlushCommands(true);

        verifyNotExecuted(iterations);

        connection.flushCommands();
        boolean result = LettuceFutures.awaitAll(5, TimeUnit.SECONDS, futures.toArray(new RedisFuture[futures.size()]));
        assertThat(result).isTrue();

        connection.close();
    }

    protected void verifyNotExecuted(int iterations) {
        for (int i = 0; i < iterations; i++) {
            assertThat(redis.get(key(i))).as("Key " + key(i) + " must be null").isNull();
        }
    }

    protected List<RedisFuture<?>> triggerSet(RedisAsyncCommands<String, String> connection, int iterations) {
        List<RedisFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            futures.add(connection.set(key(i), value(i)));
        }
        return futures;
    }

    protected String value(int i) {
        return value + "-" + i;
    }

    protected String key(int i) {
        return key + "-" + i;
    }
}
