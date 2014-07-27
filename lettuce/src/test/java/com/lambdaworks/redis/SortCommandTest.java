// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static com.lambdaworks.redis.SortArgs.Builder.alpha;
import static com.lambdaworks.redis.SortArgs.Builder.asc;
import static com.lambdaworks.redis.SortArgs.Builder.by;
import static com.lambdaworks.redis.SortArgs.Builder.desc;
import static com.lambdaworks.redis.SortArgs.Builder.get;
import static com.lambdaworks.redis.SortArgs.Builder.limit;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SortCommandTest extends AbstractCommandTest {
    @Test
    public void sort() throws Exception {
        redis.rpush(key, "3", "2", "1");
        assertEquals(list("1", "2", "3"), redis.sort(key));
        assertEquals(list("1", "2", "3"), redis.sort(key, asc()));
    }

    @Test
    public void sortStreaming() throws Exception {
        redis.rpush(key, "3", "2", "1");

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();
        Long count = redis.sort(streamingAdapter, key);

        assertEquals(3, count.longValue());
        assertEquals(list("1", "2", "3"), streamingAdapter.getList());
        streamingAdapter.getList().clear();

        count = redis.sort(streamingAdapter, key, desc());
        assertEquals(3, count.longValue());
        assertEquals(list("3", "2", "1"), streamingAdapter.getList());
    }

    @Test
    public void sortAlpha() throws Exception {
        redis.rpush(key, "A", "B", "C");
        assertEquals(list("C", "B", "A"), redis.sort(key, alpha().desc()));
    }

    @Test
    public void sortBy() throws Exception {
        redis.rpush(key, "foo", "bar", "baz");
        redis.set("weight_foo", "8");
        redis.set("weight_bar", "4");
        redis.set("weight_baz", "2");
        assertEquals(list("baz", "bar", "foo"), redis.sort(key, by("weight_*")));
    }

    @Test
    public void sortDesc() throws Exception {
        redis.rpush(key, "1", "2", "3");
        assertEquals(list("3", "2", "1"), redis.sort(key, desc()));
    }

    @Test
    public void sortGet() throws Exception {
        redis.rpush(key, "1", "2");
        redis.set("obj_1", "foo");
        redis.set("obj_2", "bar");
        assertEquals(list("foo", "bar"), redis.sort(key, get("obj_*")));
    }

    @Test
    public void sortLimit() throws Exception {
        redis.rpush(key, "3", "2", "1");
        assertEquals(list("2", "3"), redis.sort(key, limit(1, 2)));
    }

    @Test
    public void sortStore() throws Exception {
        redis.rpush("one", "1", "2", "3");
        assertEquals(3, (long) redis.sortStore("one", desc(), "two"));
        assertEquals(list("3", "2", "1"), redis.lrange("two", 0, -1));
    }
}
