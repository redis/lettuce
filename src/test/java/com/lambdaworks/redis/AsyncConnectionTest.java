// Copyright (C) 2011 - Will Glozer.  All rights reserved.

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

import com.lambdaworks.redis.api.async.RedisAsyncCommands;

public class AsyncConnectionTest extends AbstractRedisClientTest {
    private RedisAsyncCommands<String, String> async;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void openAsyncConnection() throws Exception {
        async = client.connect().async();
    }

    @After
    public void closeAsyncConnection() throws Exception {
        async.getStatefulConnection().close();
    }

    @Test(timeout = 10000)
    public void multi() throws Exception {
        assertThat(async.multi().get()).isEqualTo("OK");
        Future<String> set = async.set(key, value);
        Future<Long> rpush = async.rpush("list", "1", "2");
        Future<List<String>> lrange = async.lrange("list", 0, -1);

        assertThat(!set.isDone() && !rpush.isDone() && !rpush.isDone()).isTrue();
        assertThat(async.exec().get()).contains("OK", 2L, list("1", "2"));

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
        assertThat(async.exec().get()).isEmpty();
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

        RedisAsyncCommands<String, String> connection = client.connect().async();

        Long len = connection.llen(key).get();
        assertThat(len.intValue()).isEqualTo(1000);

        RedisFuture<List<String>> sort = connection.sort(key);
        assertThat(sort.isCancelled()).isFalse();

        sort.thenRun(listener);

        sort.get();
        Thread.sleep(100);

        assertThat(run).hasSize(1);

        connection.getStatefulConnection().close();

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

        RedisAsyncCommands<String, String> connection = client.connect().async();

        RedisFuture<String> set = connection.set(key, value);
        set.get();

        set.thenRun(listener);

        assertThat(run).hasSize(1);

        connection.getStatefulConnection().close();
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
