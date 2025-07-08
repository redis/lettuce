/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.internal.Futures;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.enterprise.inject.New;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Will Glozer
 * @author Mark Paluch
 * @author Hari Mani
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class AsyncConnectionIntegrationTests extends TestSupport {

    private final RedisAsyncCommands<String, String> async;

    @Inject
    AsyncConnectionIntegrationTests(@New final StatefulRedisConnection<String, String> connection) {
        this.async = connection.async();
        connection.sync().flushall();
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
        // using 'key' causes issues for some strange reason so using a fresh key
        final String listKey = "list:" + key;
        final List<RedisFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            futures.add(async.lpush(listKey, "" + i));
        }
        TestFutures.awaitOrTimeout(futures);

        Long len = TestFutures.getOrTimeout(async.llen(listKey));
        assertThat(len.intValue()).isEqualTo(1000);

        RedisFuture<List<String>> sort = async.sort(listKey);
        assertThat(sort.isCancelled()).isFalse();

        final List<Object> run = new ArrayList<>();
        sort.thenRun(() -> run.add(new Object()));

        TestFutures.awaitOrTimeout(sort);
        Delay.delay(Duration.ofMillis(100));

        assertThat(run).hasSize(1);
    }

    @Test
    void futureListenerCompleted() {
        final RedisFuture<String> set = async.set(key, value);
        TestFutures.awaitOrTimeout(set);

        final List<Object> run = new ArrayList<>();
        set.thenRun(() -> run.add(new Object()));

        assertThat(run).hasSize(1);
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
