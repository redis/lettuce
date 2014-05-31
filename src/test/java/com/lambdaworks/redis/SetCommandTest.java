// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

public class SetCommandTest extends AbstractCommandTest {
    @Test
    public void sadd() throws Exception {
        assertEquals(1, (long) redis.sadd(key, "a"));
        assertEquals(0, (long) redis.sadd(key, "a"));
        assertEquals(set("a"), redis.smembers(key));
        assertEquals(2, (long) redis.sadd(key, "b", "c"));
        assertEquals(set("a", "b", "c"), redis.smembers(key));
    }

    @Test
    public void scard() throws Exception {
        assertEquals(0, (long) redis.scard(key));
        redis.sadd(key, "a");
        assertEquals(1, (long) redis.scard(key));
    }

    @Test
    public void sdiff() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(set("b", "d"), redis.sdiff("key1", "key2", "key3"));
    }

    @Test
    public void sdiffstore() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(2, (long) redis.sdiffstore("newset", "key1", "key2", "key3"));
        assertEquals(set("b", "d"), redis.smembers("newset"));
    }

    @Test
    public void sinter() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(set("c"), redis.sinter("key1", "key2", "key3"));
    }

    @Test
    public void sinterstore() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(1, (long) redis.sinterstore("newset", "key1", "key2", "key3"));
        assertEquals(set("c"), redis.smembers("newset"));
    }

    @Test
    public void sismember() throws Exception {
        assertFalse(redis.sismember(key, "a"));
        redis.sadd(key, "a");
        assertTrue(redis.sismember(key, "a"));
    }

    @Test
    public void smove() throws Exception {
        redis.sadd(key, "a", "b", "c");
        assertFalse(redis.smove(key, "key1", "d"));
        assertTrue(redis.smove(key, "key1", "a"));
        assertEquals(set("b", "c"), redis.smembers(key));
        assertEquals(set("a"), redis.smembers("key1"));
    }

    @Test
    public void smembers() throws Exception {
        redis.sadd(key, "a");
        redis.sadd(key, "b");
        assertTrue(set("a", "b").equals(redis.smembers(key)));
    }

    @Test
    public void spop() throws Exception {
        assertNull(redis.spop(key));
        redis.sadd(key, "a", "b", "c");
        String rand = redis.spop(key);
        assertTrue(set("a", "b", "c").contains(rand));
        assertFalse(redis.smembers(key).contains(rand));
    }

    @Test
    public void srandmember() throws Exception {
        assertNull(redis.spop(key));
        redis.sadd(key, "a", "b", "c", "d");
        assertTrue(set("a", "b", "c", "d").contains(redis.srandmember(key)));
        assertEquals(set("a", "b", "c", "d"), redis.smembers(key));
        Set<String> rand = redis.srandmember(key, 3);
        assertEquals(3, rand.size());
        assertTrue(set("a", "b", "c", "d").containsAll(rand));
    }

    @Test
    public void srem() throws Exception {
        redis.sadd(key, "a", "b", "c");
        assertEquals(0, (long) redis.srem(key, "d"));
        assertEquals(1, (long) redis.srem(key, "b"));
        assertEquals(set("a", "c"), redis.smembers(key));
        assertEquals(2, (long) redis.srem(key, "a", "c"));
        assertEquals(set(), redis.smembers(key));
    }

    @Test
    public void sunion() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(set("a", "b", "c", "d", "e"), redis.sunion("key1", "key2", "key3"));
    }

    @Test
    public void sunionStreaming() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");

        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        Long count = redis.sunion(adapter, "key1", "key2", "key3");

        assertEquals(5, count.longValue());

        assertEquals(new TreeSet(list("c", "a", "b", "e", "d")), new TreeSet(adapter.getList()));
    }

    @Test
    public void sunionstore() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(5, (long) redis.sunionstore("newset", "key1", "key2", "key3"));
        assertEquals(set("a", "b", "c", "d", "e"), redis.smembers("newset"));
    }

    @Test
    public void sscan() throws Exception {
        redis.sadd(key, value);
        ValueScanCursor<String> cursor = redis.sscan(key);

        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());
        assertEquals(list(value), cursor.getValues());

    }

    @Test
    public void sscanStreaming() throws Exception {
        redis.sadd(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        StreamScanCursor cursor = redis.sscan(adapter, key, ScanArgs.Builder.count(100).match("*"));

        assertEquals(1, cursor.getCount());
        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());
        assertEquals(list(value), adapter.getList());

    }

    @Test
    public void sscanMultiple() throws Exception {

        Set<String> expect = new HashSet<String>();
        Set<String> check = new HashSet<String>();
        setup100KeyValues(expect);

        ValueScanCursor<String> cursor = redis.sscan(key, ScanArgs.Builder.count(5));

        assertNotNull(cursor.getCursor());
        assertNotEquals("0", cursor.getCursor());
        assertFalse(cursor.isFinished());

        check.addAll(cursor.getValues());

        while (!cursor.isFinished()) {
            cursor = redis.sscan(key, cursor);
            check.addAll(cursor.getValues());
        }

        assertEquals(new TreeSet(expect), new TreeSet(check));
    }

    @Test
    public void scanMatch() throws Exception {

        Set<String> expect = new HashSet<String>();
        setup100KeyValues(expect);

        ValueScanCursor<String> cursor = redis.sscan(key, ScanArgs.Builder.count(100).match("value1*"));

        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());

        assertEquals(11, cursor.getValues().size());
    }

    protected void setup100KeyValues(Set<String> expect) {
        for (int i = 0; i < 100; i++) {
            redis.sadd(key, value + i);
            expect.add(value + i);
        }
    }
}
