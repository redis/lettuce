// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static com.lambdaworks.redis.SetArgs.Builder.ex;
import static com.lambdaworks.redis.SetArgs.Builder.nx;
import static com.lambdaworks.redis.SetArgs.Builder.px;
import static com.lambdaworks.redis.SetArgs.Builder.xx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.RedisCommandExecutionException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.ListStreamingAdapter;
import com.lambdaworks.redis.RedisException;

public class StringCommandTest extends AbstractRedisClientTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void append() throws Exception {
        assertEquals(value.length(), (long) redis.append(key, value));
        assertEquals(value.length() + 1, (long) redis.append(key, "X"));
    }

    @Test
    public void get() throws Exception {
        assertNull(redis.get(key));
        redis.set(key, value);
        Assert.assertEquals(value, redis.get(key));
    }

    @Test
    public void getbit() throws Exception {
        assertEquals(0, (long) redis.getbit(key, 0));
        redis.setbit(key, 0, 1);
        assertEquals(1, (long) redis.getbit(key, 0));
    }

    @Test
    public void getrange() throws Exception {
        Assert.assertEquals("", redis.getrange(key, 0, -1));
        redis.set(key, "foobar");
        Assert.assertEquals("oba", redis.getrange(key, 2, 4));
        Assert.assertEquals("bar", redis.getrange(key, 3, -1));
    }

    @Test
    public void getset() throws Exception {
        assertNull(redis.getset(key, value));
        Assert.assertEquals(value, redis.getset(key, "two"));
        Assert.assertEquals("two", redis.get(key));
    }

    @Test
    public void mget() throws Exception {
        setupMget();
        Assert.assertEquals(list("1", "2"), redis.mget("one", "two"));
    }

    protected void setupMget() {
        Assert.assertEquals(list((String) null), redis.mget(key));
        redis.set("one", "1");
        redis.set("two", "2");
    }

    @Test
    public void mgetStreaming() throws Exception {
        setupMget();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();
        Long count = redis.mget(streamingAdapter, "one", "two");
        assertEquals(2, count.intValue());

        assertEquals(list("1", "2"), streamingAdapter.getList());
    }

    @Test
    public void mset() throws Exception {
        Assert.assertEquals(list(null, null), redis.mget("one", "two"));
        Map<String, String> map = new HashMap<String, String>();
        map.put("one", "1");
        map.put("two", "2");
        Assert.assertEquals("OK", redis.mset(map));
        Assert.assertEquals(list("1", "2"), redis.mget("one", "two"));
    }

    @Test
    public void msetnx() throws Exception {
        redis.set("one", "1");
        Map<String, String> map = new HashMap<String, String>();
        map.put("one", "1");
        map.put("two", "2");
        assertFalse(redis.msetnx(map));
        redis.del("one");
        assertTrue(redis.msetnx(map));
        Assert.assertEquals("2", redis.get("two"));
    }

    @Test
    public void set() throws Exception {
        assertNull(redis.get(key));
        Assert.assertEquals("OK", redis.set(key, value));
        Assert.assertEquals(value, redis.get(key));

        assertEquals("OK", redis.set(key, value, px(20000)));
        Assert.assertEquals("OK", redis.set(key, value, ex(10)));
        Assert.assertEquals(value, redis.get(key));
        assertTrue(redis.ttl(key) >= 9);

        Assert.assertEquals("OK", redis.set(key, value, ex(10).px(20000)));
        Assert.assertEquals(value, redis.get(key));
        assertTrue(redis.ttl(key) >= 19);

        Assert.assertEquals("OK", redis.set(key, value, px(10000)));
        Assert.assertEquals(value, redis.get(key));
        assertTrue(redis.ttl(key) >= 9);

        assertNull(redis.set(key, value, nx()));
        Assert.assertEquals("OK", redis.set(key, value, xx()));
        Assert.assertEquals(value, redis.get(key));


        assertEquals("OK", redis.set(key, value, nx()));
        assertEquals(value, redis.get(key));

        redis.del(key);

        Assert.assertEquals("OK", redis.set(key, value, ex(10).px(20000).nx()));
        Assert.assertEquals(value, redis.get(key));
        assertTrue(redis.ttl(key) >= 19);
    }

    @Test(expected = RedisException.class)
    public void setNegativeEX() throws Exception {
        redis.set(key, value, ex(-10));
    }

    @Test(expected = RedisException.class)
    public void setNegativePX() throws Exception {
        redis.set(key, value, px(-1000));
    }

    @Test
    public void setExWithPx() throws Exception {
        exception.expect(RedisCommandExecutionException.class);
        exception.expectMessage("ERR syntax error");
        redis.set(key, value, ex(10).px(20000).nx());
    }

    @Test
    public void setbit() throws Exception {
        assertEquals(0, (long) redis.setbit(key, 0, 1));
        assertEquals(1, (long) redis.setbit(key, 0, 0));
    }

    @Test
    public void setex() throws Exception {
        Assert.assertEquals("OK", redis.setex(key, 10, value));
        Assert.assertEquals(value, redis.get(key));
        assertTrue(redis.ttl(key) >= 9);
    }

    @Test
    public void psetex() throws Exception {
        Assert.assertEquals("OK", redis.psetex(key, 20000, value));
        Assert.assertEquals(value, redis.get(key));
        assertTrue(redis.pttl(key) >= 19000);
    }

    @Test
    public void setnx() throws Exception {
        assertTrue(redis.setnx(key, value));
        assertFalse(redis.setnx(key, value));
    }

    @Test
    public void setrange() throws Exception {
        assertEquals("foo".length(), (long) redis.setrange(key, 0, "foo"));
        assertEquals(6, (long) redis.setrange(key, 3, "bar"));
        Assert.assertEquals("foobar", redis.get(key));
    }

    @Test
    public void strlen() throws Exception {
        assertEquals(0, (long) redis.strlen(key));
        redis.set(key, value);
        assertEquals(value.length(), (long) redis.strlen(key));
    }

    @Test
    public void time() throws Exception {

        List<String> time = redis.time();
        assertEquals(2, time.size());

        Long.parseLong(time.get(0));
        Long.parseLong(time.get(1));
    }
}
