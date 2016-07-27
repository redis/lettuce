// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static com.lambdaworks.redis.SortArgs.Builder.alpha;
import static com.lambdaworks.redis.SortArgs.Builder.asc;
import static com.lambdaworks.redis.SortArgs.Builder.by;
import static com.lambdaworks.redis.SortArgs.Builder.desc;
import static com.lambdaworks.redis.SortArgs.Builder.get;
import static com.lambdaworks.redis.SortArgs.Builder.limit;
import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.ListStreamingAdapter;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class SortCommandTest extends AbstractRedisClientTest {
    @Test
    public void sort() throws Exception {
        redis.rpush(key, "3", "2", "1");
        assertThat(redis.sort(key)).isEqualTo(list("1", "2", "3"));
        assertThat(redis.sort(key, asc())).isEqualTo(list("1", "2", "3"));
    }

    @Test
    public void sortStreaming() throws Exception {
        redis.rpush(key, "3", "2", "1");

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();
        Long count = redis.sort(streamingAdapter, key);

        assertThat(count.longValue()).isEqualTo(3);
        assertThat(streamingAdapter.getList()).isEqualTo(list("1", "2", "3"));
        streamingAdapter.getList().clear();

        count = redis.sort(streamingAdapter, key, desc());
        assertThat(count.longValue()).isEqualTo(3);
        assertThat(streamingAdapter.getList()).isEqualTo(list("3", "2", "1"));
    }

    @Test
    public void sortAlpha() throws Exception {
        redis.rpush(key, "A", "B", "C");
        assertThat(redis.sort(key, alpha().desc())).isEqualTo(list("C", "B", "A"));
    }

    @Test
    public void sortBy() throws Exception {
        redis.rpush(key, "foo", "bar", "baz");
        redis.set("weight_foo", "8");
        redis.set("weight_bar", "4");
        redis.set("weight_baz", "2");
        assertThat(redis.sort(key, by("weight_*"))).isEqualTo(list("baz", "bar", "foo"));
    }

    @Test
    public void sortDesc() throws Exception {
        redis.rpush(key, "1", "2", "3");
        assertThat(redis.sort(key, desc())).isEqualTo(list("3", "2", "1"));
    }

    @Test
    public void sortGet() throws Exception {
        redis.rpush(key, "1", "2");
        redis.set("obj_1", "foo");
        redis.set("obj_2", "bar");
        assertThat(redis.sort(key, get("obj_*"))).isEqualTo(list("foo", "bar"));
    }

    @Test
    public void sortLimit() throws Exception {
        redis.rpush(key, "3", "2", "1");
        assertThat(redis.sort(key, limit(1, 2))).isEqualTo(list("2", "3"));
    }

    @Test
    public void sortStore() throws Exception {
        redis.rpush("one", "1", "2", "3");
        assertThat(redis.sortStore("one", desc(), "two")).isEqualTo(3);
        assertThat(redis.lrange("two", 0, -1)).isEqualTo(list("3", "2", "1"));
    }
}
