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
package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
public class AsyncConnectionTest extends AbstractRedisClientTest {
    private RedisAsyncConnection<String, String> async;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void openAsyncConnection() throws Exception {
        async = client.connectAsync();
    }

    @After
    public void closeAsyncConnection() throws Exception {
        async.close();
    }

    @Test(timeout = 10000)
    public void multi() throws Exception {
        assertThat(async.multi().get()).isEqualTo("OK");
        Future<String> set = async.set(key, value);
        Future<Long> rpush = async.rpush("list", "1", "2");
        Future<List<String>> lrange = async.lrange("list", 0, -1);

        assertThat(!set.isDone() && !rpush.isDone() && !rpush.isDone()).isTrue();
        assertThat(async.exec().get()).isEqualTo(list("OK", 2L, list("1", "2")));

        assertThat(set.get()).isEqualTo("OK");
        assertThat((long) rpush.get()).isEqualTo(2L);
        assertThat(lrange.get()).isEqualTo(list("1", "2"));
    }

    @Test(timeout = 10000)
    public void watch() throws Exception {
        assertThat(async.watch(key).get()).isEqualTo("OK");

        redis.set(key, value + "X");

        async.multi();
        Future<String> set = async.set(key, value);
        Future<Long> append = async.append(key, "foo");
        assertThat(async.exec().get()).isEqualTo(list());
        assertThat(set.get()).isNull();
        assertThat(append.get()).isNull();
    }

    @Test(timeout = 10000)
    public void futureListener() throws Exception {

        final List<Object> run = new ArrayList<>();

        Runnable listener = new Runnable() {
            @Override
            public void run() {
                run.add(new Object());
            }
        };

        for (int i = 0; i < 1000; i++) {
            redis.lpush(key, "" + i);
        }

        RedisAsyncConnection<String, String> connection = client.connectAsync();

        Long len = connection.llen(key).get();
        assertThat(len.intValue()).isEqualTo(1000);

        RedisFuture<List<String>> sort = connection.sort(key);
        assertThat(sort.isCancelled()).isFalse();

        sort.thenRun(listener);

        sort.get();
        Thread.sleep(100);

        assertThat(run).hasSize(1);

        connection.close();

    }

    @Test(timeout = 1000)
    public void futureListenerCompleted() throws Exception {

        final List<Object> run = new ArrayList<>();

        Runnable listener = new Runnable() {
            @Override
            public void run() {
                run.add(new Object());
            }
        };

        RedisAsyncConnection<String, String> connection = client.connectAsync();

        RedisFuture<String> set = connection.set(key, value);
        set.get();

        set.thenRun(listener);

        assertThat(run).hasSize(1);

        connection.close();
    }

    @Test(timeout = 500)
    public void discardCompletesFutures() throws Exception {
        async.multi();
        Future<String> set = async.set(key, value);
        async.discard();
        assertThat(set.get()).isNull();
    }

    @Test(timeout = 10000)
    public void awaitAll() throws Exception {
        Future<String> get1 = async.get(key);
        Future<String> set = async.set(key, value);
        Future<String> get2 = async.get(key);
        Future<Long> append = async.append(key, value);

        assertThat(LettuceFutures.awaitAll(1, TimeUnit.SECONDS, get1, set, get2, append)).isTrue();

        assertThat(get1.get()).isNull();
        assertThat(set.get()).isEqualTo("OK");
        assertThat(get2.get()).isEqualTo(value);
        assertThat((long) append.get()).isEqualTo(value.length() * 2);
    }

    @Test(timeout = 500)
    public void awaitAllTimeout() throws Exception {
        Future<KeyValue<String, String>> blpop = async.blpop(1, key);
        assertThat(LettuceFutures.awaitAll(1, TimeUnit.NANOSECONDS, blpop)).isFalse();
    }

}
