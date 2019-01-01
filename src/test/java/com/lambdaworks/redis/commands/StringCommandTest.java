/*
 * Copyright 2011-2019 the original author or authors.
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
package com.lambdaworks.redis.commands;

import static com.lambdaworks.redis.SetArgs.Builder.ex;
import static com.lambdaworks.redis.SetArgs.Builder.nx;
import static com.lambdaworks.redis.SetArgs.Builder.px;
import static com.lambdaworks.redis.SetArgs.Builder.xx;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.ListStreamingAdapter;
import com.lambdaworks.redis.RedisException;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
public class StringCommandTest extends AbstractRedisClientTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void append() {
        assertThat(redis.append(key, value)).isEqualTo(value.length());
        assertThat(redis.append(key, "X")).isEqualTo(value.length() + 1);
    }

    @Test
    public void get() {
        assertThat(redis.get(key)).isNull();
        redis.set(key, value);
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void getbit() {
        assertThat(redis.getbit(key, 0)).isEqualTo(0);
        redis.setbit(key, 0, 1);
        assertThat(redis.getbit(key, 0)).isEqualTo(1);
    }

    @Test
    public void getrange() {
        assertThat(redis.getrange(key, 0, -1)).isEqualTo("");
        redis.set(key, "foobar");
        assertThat(redis.getrange(key, 2, 4)).isEqualTo("oba");
        assertThat(redis.getrange(key, 3, -1)).isEqualTo("bar");
    }

    @Test
    public void getset() {
        assertThat(redis.getset(key, value)).isNull();
        assertThat(redis.getset(key, "two")).isEqualTo(value);
        assertThat(redis.get(key)).isEqualTo("two");
    }

    @Test
    public void mget() {
        setupMget();
        assertThat(redis.mget("one", "two")).isEqualTo(list("1", "2"));
    }

    protected void setupMget() {
        assertThat(redis.mget(key)).isEqualTo(list((String) null));
        redis.set("one", "1");
        redis.set("two", "2");
    }

    @Test
    public void mgetStreaming() {
        setupMget();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();
        Long count = redis.mget(streamingAdapter, "one", "two");
        assertThat(count.intValue()).isEqualTo(2);

        assertThat(streamingAdapter.getList()).isEqualTo(list("1", "2"));
    }

    @Test
    public void mset() {
        assertThat(redis.mget("one", "two")).isEqualTo(list(null, null));
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        assertThat(redis.mset(map)).isEqualTo("OK");
        assertThat(redis.mget("one", "two")).isEqualTo(list("1", "2"));
    }

    @Test
    public void msetnx() {
        redis.set("one", "1");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        assertThat(redis.msetnx(map)).isFalse();
        redis.del("one");
        assertThat(redis.msetnx(map)).isTrue();
        assertThat(redis.get("two")).isEqualTo("2");
    }

    @Test
    public void set() {
        assertThat(redis.get(key)).isNull();
        assertThat(redis.set(key, value)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);

        assertThat(redis.set(key, value, px(20000))).isEqualTo("OK");
        assertThat(redis.set(key, value, ex(10))).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.ttl(key)).isGreaterThanOrEqualTo(9);

        assertThat(redis.set(key, value, px(10000))).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.ttl(key)).isGreaterThanOrEqualTo(9);

        assertThat(redis.set(key, value, nx())).isNull();
        assertThat(redis.set(key, value, xx())).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);

        redis.del(key);
        assertThat(redis.set(key, value, nx())).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);

        redis.del(key);

        assertThat(redis.set(key, value, px(20000).nx())).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.ttl(key) >= 19).isTrue();
    }

    @Test(expected = RedisException.class)
    public void setNegativeEX() {
        redis.set(key, value, ex(-10));
    }

    @Test(expected = RedisException.class)
    public void setNegativePX() {
        redis.set(key, value, px(-1000));
    }

    @Test
    public void setbit() {
        assertThat(redis.setbit(key, 0, 1)).isEqualTo(0);
        assertThat(redis.setbit(key, 0, 0)).isEqualTo(1);
    }

    @Test
    public void setex() {
        assertThat(redis.setex(key, 10, value)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.ttl(key) >= 9).isTrue();
    }

    @Test
    public void psetex() {
        assertThat(redis.psetex(key, 20000, value)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.pttl(key) >= 19000).isTrue();
    }

    @Test
    public void setnx() {
        assertThat(redis.setnx(key, value)).isTrue();
        assertThat(redis.setnx(key, value)).isFalse();
    }

    @Test
    public void setrange() {
        assertThat(redis.setrange(key, 0, "foo")).isEqualTo("foo".length());
        assertThat(redis.setrange(key, 3, "bar")).isEqualTo(6);
        assertThat(redis.get(key)).isEqualTo("foobar");
    }

    @Test
    public void strlen() {
        assertThat((long) redis.strlen(key)).isEqualTo(0);
        redis.set(key, value);
        assertThat((long) redis.strlen(key)).isEqualTo(value.length());
    }

    @Test
    public void time() {

        List<String> time = redis.time();
        assertThat(time).hasSize(2);

        Long.parseLong(time.get(0));
        Long.parseLong(time.get(1));
    }
}
