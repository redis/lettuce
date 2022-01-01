/*
 * Copyright 2011-2022 the original author or authors.
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
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.internal.Futures;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class AsyncConnectionIntegrationTests extends TestSupport {

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final RedisAsyncCommands<String, String> async;

    @Inject
    AsyncConnectionIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.connection = connection;
        this.async = connection.async();
        this.connection.sync().flushall();
    }

    @Test
    void multi() {
        assertThat(TestFutures.getOrTimeout(async.multi())).isEqualTo("OK");
        Future<String> set = async.set(key, value);
        Future<Long> rpush = async.rpush("list", "1", "2");
        Future<List<String>> lrange = async.lrange("list", 0, -1);

        assertThat(!set.isDone() && !rpush.isDone() && !rpush.isDone()).isTrue();
        assertThat(TestFutures.getOrTimeout(async.exec())).contains("OK", 2L, list("1", "2"));

        assertThat(TestFutures.getOrTimeout(set)).isEqualTo("OK");
        assertThat(TestFutures.getOrTimeout(rpush)).isEqualTo(2L);
        assertThat(TestFutures.getOrTimeout(lrange)).isEqualTo(list("1", "2"));
    }

    @Test
    void watch() {
        assertThat(TestFutures.getOrTimeout(async.watch(key))).isEqualTo("OK");

        async.set(key, value + "X");

        async.multi();
        Future<String> set = async.set(key, value);
        Future<Long> append = async.append(key, "foo");
        assertThat(TestFutures.getOrTimeout(async.exec())).isEmpty();
        assertThat(TestFutures.getOrTimeout(set)).isNull();
        assertThat(TestFutures.getOrTimeout(append)).isNull();
    }

    @Test
    void futureListener() {

        final List<Object> run = new ArrayList<>();

        Runnable listener = () -> run.add(new Object());

        List<RedisFuture<?>> futures = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            futures.add(async.lpush(key, "" + i));
        }

        TestFutures.awaitOrTimeout(futures);

        RedisAsyncCommands<String, String> connection = client.connect().async();

        Long len = TestFutures.getOrTimeout(connection.llen(key));
        assertThat(len.intValue()).isEqualTo(1000);

        RedisFuture<List<String>> sort = connection.sort(key);
        assertThat(sort.isCancelled()).isFalse();

        sort.thenRun(listener);

        TestFutures.awaitOrTimeout(sort);
        Delay.delay(Duration.ofMillis(100));

        assertThat(run).hasSize(1);

        connection.getStatefulConnection().close();
    }

    @Test
    void futureListenerCompleted() {

        final List<Object> run = new ArrayList<>();

        Runnable listener = new Runnable() {
            @Override
            public void run() {
                run.add(new Object());
            }
        };

        RedisAsyncCommands<String, String> connection = client.connect().async();

        RedisFuture<String> set = connection.set(key, value);
        TestFutures.awaitOrTimeout(set);

        set.thenRun(listener);

        assertThat(run).hasSize(1);

        connection.getStatefulConnection().close();
    }

    @Test
    void discardCompletesFutures() {
        async.multi();
        Future<String> set = async.set(key, value);
        async.discard();
        assertThat(TestFutures.getOrTimeout(set)).isNull();
    }

    @Test
    void awaitAll() {

        Future<String> get1 = async.get(key);
        Future<String> set = async.set(key, value);
        Future<String> get2 = async.get(key);
        Future<Long> append = async.append(key, value);

        assertThat(Futures.awaitAll(1, TimeUnit.SECONDS, get1, set, get2, append)).isTrue();

        assertThat(TestFutures.getOrTimeout(get1)).isNull();
        assertThat(TestFutures.getOrTimeout(set)).isEqualTo("OK");
        assertThat(TestFutures.getOrTimeout(get2)).isEqualTo(value);
        assertThat(TestFutures.getOrTimeout(append).longValue()).isEqualTo(value.length() * 2);
    }

    @Test
    void awaitAllTimeout() {
        Future<KeyValue<String, String>> blpop = async.blpop(1, key);
        assertThat(Futures.await(1, TimeUnit.NANOSECONDS, blpop)).isFalse();
    }
}
