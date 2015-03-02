// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class AsyncConnectionTest extends AbstractCommandTest {
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

        assertThat(!set.isDone() && !rpush.isDone()).isTrue();
        assertThat(async.exec().get()).isEqualTo(list("OK", 2L, list("1", "2")));

        assertThat(set.get()).isEqualTo("OK");
        assertThat((long) rpush.get()).isEqualTo(2L);
        assertThat(lrange.get()).isEqualTo(list("1", "2"));
    }

    @Test(timeout = 1000000)
    public void multiError() throws Exception {
        assertThat(async.multi().get()).isEqualTo("OK");
        String k = "brokenkey";
        Future<String> set = async.set(k, value);
        RedisFuture<String> rpush = async.lpop(k);
        Future<String> set2 = async.set(k, "2");

        assertThat(!set.isDone() && !rpush.isDone() && !set2.isDone()).isTrue();
        List<Object> actual = async.exec().get();

        assertThat(actual.get(0)).isEqualTo("OK");
        assertThat(actual.get(1)).isInstanceOf(RedisCommandExecutionException.class);
        assertThat(actual.get(2)).isEqualTo("OK");


        assertThat(set.get()).isEqualTo("OK");
        assertThat(set2.get()).isEqualTo("OK");
        try{
            rpush.get();
            fail();
        }catch(ExecutionException expected){}
        assertThat(rpush.getError()).isEqualTo("WRONGTYPE Operation against a key holding the wrong kind of value");
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

        final List<Object> run = new ArrayList<Object>();

        Runnable listener = new Runnable() {
            @Override
            public void run() {
                run.add(new Object());
            }
        };

        for (int i = 0; i < 1000; i++) {
            redis.lpush(key, "" + i);
        }
        ListeningExecutorService executor = MoreExecutors.sameThreadExecutor();

        RedisAsyncConnection<String, String> connection = client.connectAsync();

        Long len = connection.llen(key).get();
        assertThat(len.intValue()).isEqualTo(1000);

        RedisFuture<List<String>> sort = connection.sort(key);
        assertThat(sort.isCancelled()).isFalse();

        sort.addListener(listener, executor);

        sort.get();
        Thread.sleep(100);

        assertThat(run).hasSize(1);

    }

    @Test(timeout = 1000)
    public void futureListenerCompleted() throws Exception {

        final List<Object> run = new ArrayList<Object>();

        Runnable listener = new Runnable() {
            @Override
            public void run() {
                run.add(new Object());
            }
        };

        ListeningExecutorService executor = MoreExecutors.sameThreadExecutor();

        RedisAsyncConnection<String, String> connection = client.connectAsync();

        RedisFuture<String> set = connection.set(key, value);
        set.get();

        set.addListener(listener, executor);

        assertThat(run).hasSize(1);

    }

    @Test(timeout = 100)
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

    @Test(timeout = 100)
    public void awaitAllTimeout() throws Exception {
        Future<KeyValue<String, String>> blpop = async.blpop(1, key);
        assertThat(LettuceFutures.awaitAll(1, TimeUnit.NANOSECONDS, blpop)).isFalse();
    }

}
