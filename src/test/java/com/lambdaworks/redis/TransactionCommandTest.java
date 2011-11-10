// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TransactionCommandTest extends AbstractCommandTest {
    @Test
    public void discard() throws Exception {
        assertEquals("OK", redis.multi());
        redis.set(key, value);
        assertEquals("OK", redis.discard());
        assertNull(redis.get(key));
    }

    @Test
    public void exec() throws Exception {
        assertEquals("OK", redis.multi());
        redis.set(key, value);
        assertEquals(list("OK"), redis.exec());
        assertEquals(value, redis.get(key));
    }

    @Test
    public void watch() throws Exception {
        assertEquals("OK", redis.watch(key));

        RedisConnection<String, String> redis2 = client.connect();
        redis2.set(key, value + "X");
        redis2.close();

        redis.multi();
        redis.append(key, "foo");
        assertEquals(list(), redis.exec());

    }

    @Test
    public void unwatch() throws Exception {
        assertEquals("OK", redis.unwatch());
    }

    @Test
    public void commandsReturnNullInMulti() throws Exception {
        assertEquals("OK", redis.multi());
        assertNull(redis.set(key, value));
        assertNull(redis.get(key));
        assertEquals(list("OK", value), redis.exec());
        assertEquals(value, redis.get(key));
    }

    @Test
    public void execmulti() throws Exception {
        redis.multi();
        redis.set("one", "1");
        redis.set("two", "2");
        redis.mget("one", "two");
        redis.llen(key);
        assertEquals(list("OK", "OK", list("1", "2"), 0L), redis.exec());
    }

    @Test
    public void errorInMulti() throws Exception {
        redis.multi();
        redis.set(key, value);
        redis.lpop(key);
        redis.get(key);
        List<Object> values = redis.exec();
        assertEquals("OK", values.get(0));
        assertTrue(values.get(1) instanceof RedisException);
        assertEquals(value, values.get(2));
    }

    protected List<Object> list(Object... args) {
        return Arrays.asList(args);
    }
}
