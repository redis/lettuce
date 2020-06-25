/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.core.dynamic.batch.BatchException;
import io.lettuce.core.dynamic.batch.BatchExecutor;
import io.lettuce.core.dynamic.batch.BatchSize;
import io.lettuce.core.dynamic.batch.CommandBatching;
import io.lettuce.test.Futures;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class RedisCommandsBatchingIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    RedisCommandsBatchingIntegrationTests(StatefulRedisConnection<String, String> connection) {
        this.redis = connection.sync();
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void selectiveBatching() {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        SelectiveBatching api = factory.getCommands(SelectiveBatching.class);

        api.set("k1", value);
        assertThat(redis.get("k1")).isEqualTo(value);

        api.set("k2", value, CommandBatching.queue());
        api.set("k3", value, CommandBatching.queue());
        assertThat(redis.get("k2")).isNull();
        assertThat(redis.get("k3")).isNull();

        api.set("k4", value, CommandBatching.flush());

        assertThat(redis.get("k2")).isEqualTo(value);
        assertThat(redis.get("k3")).isEqualTo(value);
        assertThat(redis.get("k4")).isEqualTo(value);
    }

    @Test
    void selectiveBatchingShouldHandleErrors() {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        SelectiveBatching api = factory.getCommands(SelectiveBatching.class);

        api.set("k1", value, CommandBatching.queue());
        api.llen("k1", CommandBatching.queue());

        try {
            api.flush();
            fail("Missing BatchException");
        } catch (BatchException e) {
            assertThat(redis.get("k1")).isEqualTo(value);
            assertThat(e.getFailedCommands()).hasSize(1);
        }
    }

    @Test
    void shouldExecuteBatchingSynchronously() {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        Batching api = factory.getCommands(Batching.class);

        api.set("k1", value);
        api.set("k2", value);
        api.set("k3", value);
        api.set("k4", value);

        assertThat(redis.get("k1")).isNull();
        api.set("k5", value);

        assertThat(redis.get("k1")).isEqualTo(value);
    }

    @Test
    void shouldHandleSynchronousBatchErrors() {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        Batching api = factory.getCommands(Batching.class);

        api.set("k1", value);
        api.set("k2", value);
        api.llen("k2");
        api.set("k4", value);

        assertThat(redis.get("k1")).isNull();

        try {
            api.llen("k4");
            fail("Missing BatchException");
        } catch (BatchException e) {

            assertThat(redis.get("k1")).isEqualTo(value);
            assertThat(redis.get("k4")).isEqualTo(value);

            assertThat(e).isInstanceOf(BatchException.class);
            assertThat(e.getSuppressed()).hasSize(2);
            assertThat(e.getFailedCommands()).hasSize(2);
        }
    }

    @Test
    void shouldExecuteBatchingAynchronously() {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        Batching api = factory.getCommands(Batching.class);

        api.setAsync("k1", value);
        api.setAsync("k2", value);
        api.setAsync("k3", value);
        api.setAsync("k4", value);

        assertThat(redis.get("k1")).isNull();
        assertThat(Futures.get(api.setAsync("k5", value))).isEqualTo("OK");

        assertThat(redis.get("k1")).isEqualTo(value);
    }

    @Test
    void shouldHandleAsynchronousBatchErrors() throws Exception {

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());

        Batching api = factory.getCommands(Batching.class);

        api.setAsync("k1", value);
        api.setAsync("k2", value);
        api.llen("k2");
        api.setAsync("k4", value);

        assertThat(redis.get("k1")).isNull();

        RedisFuture<Long> llen = api.llenAsync("k4");
        llen.await(1, TimeUnit.SECONDS);

        assertThat(redis.get("k1")).isEqualTo(value);
        assertThat(redis.get("k4")).isEqualTo(value);

        try {
            LettuceFutures.awaitAll(1, TimeUnit.SECONDS, llen);
            fail("Missing RedisCommandExecutionException");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RedisCommandExecutionException.class);
        }
    }

    @BatchSize(5)
    static interface Batching extends Commands {

        void set(String key, String value);

        void llen(String key);

        @Command("SET")
        RedisFuture<String> setAsync(String key, String value);

        @Command("LLEN")
        RedisFuture<Long> llenAsync(String key);

    }

    static interface SelectiveBatching extends Commands, BatchExecutor {

        void set(String key, String value);

        void set(String key, String value, CommandBatching commandBatching);

        void llen(String key, CommandBatching commandBatching);

    }

}
