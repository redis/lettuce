// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

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
        assertEquals("OK", async.multi().get());
        Future<String> set = async.set(key, value);
        Future<Long> rpush = async.rpush("list", "1", "2");
        Future<List<String>> lrange = async.lrange("list", 0, -1);

        assertTrue(!set.isDone() && !rpush.isDone() && !rpush.isDone());
        assertEquals(list("OK", 2L, list("1", "2")), async.exec().get());

        assertEquals("OK", set.get());
        assertEquals(2L, (long) rpush.get());
        assertEquals(list("1", "2"), lrange.get());
    }

    @Test(timeout = 10000)
    public void watch() throws Exception {
        assertEquals("OK", async.watch(key).get());

        redis.set(key, value + "X");

        async.multi();
        Future<String> set = async.set(key, value);
        Future<Long> append = async.append(key, "foo");
        assertEquals(list(), async.exec().get());
        assertNull(set.get());
        assertNull(append.get());
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

        for (int i = 0; i < 100; i++) {
            redis.lpush(key, "" + i);
        }
        ListeningExecutorService executor = MoreExecutors.sameThreadExecutor();

        RedisAsyncConnection<String, String> connection = client.connectAsync();

        Long len = connection.llen(key).get();
        assertEquals(100, len.intValue());

        RedisFuture<List<String>> sort = connection.sort(key);
        assertFalse(sort.isDone());
        assertFalse(sort.isCancelled());

        sort.addListener(listener, executor);

        while (!sort.isDone()) {
            Thread.sleep(50);
        }

        assertEquals(1, run.size());

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

        assertEquals(1, run.size());

    }

    @Test(timeout = 100)
    public void discardCompletesFutures() throws Exception {
        async.multi();
        Future<String> set = async.set(key, value);
        async.discard();
        assertNull(set.get());
    }

    @Test(timeout = 10000)
    public void awaitAll() throws Exception {
        Future<String> get1 = async.get(key);
        Future<String> set = async.set(key, value);
        Future<String> get2 = async.get(key);
        Future<Long> append = async.append(key, value);

        assertTrue(LettuceFutures.awaitAll(1, TimeUnit.SECONDS, get1, set, get2, append));

        assertNull(get1.get());
        assertEquals("OK", set.get());
        assertEquals(value, get2.get());
        assertEquals(value.length() * 2, (long) append.get());
    }

    @Test(timeout = 100)
    public void awaitAllTimeout() throws Exception {
        Future<KeyValue<String, String>> blpop = async.blpop(1, key);
        assertFalse(LettuceFutures.awaitAll(1, TimeUnit.NANOSECONDS, blpop));
    }
}
