// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class AsyncConnectionTest extends AbstractCommandTest {
    private RedisAsyncConnection<String,String> async;

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

    @Test
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

    @Test
    public void watch() throws Exception {
        assertEquals("OK", async.watch(key).get());

        redis.set(key, value + "X");

        async.multi();
        Future<Long> append = async.append(key, "foo");
        assertEquals(list(), async.exec().get());
        assertNull(append.get());
    }

    @Test
    public void awaitAll() throws Exception {
        Future<String> get1 = async.get(key);
        Future<String> set = async.set(key, value);
        Future<String> get2 = async.get(key);
        Future<Long> append = async.append(key, value);

        assertTrue(async.awaitAll(get1, set, get2, append));

        assertNull(get1.get());
        assertEquals("OK", set.get());
        assertEquals(value, get2.get());
        assertEquals(value.length() * 2, (long) append.get());
    }

    @Test(timeout = 100)
    public void awaitAllTimeout() throws Exception {
        Future<KeyValue<String, String>> blpop = async.blpop(1, key);
        assertFalse(async.awaitAll(1, TimeUnit.NANOSECONDS, blpop));
    }
}
