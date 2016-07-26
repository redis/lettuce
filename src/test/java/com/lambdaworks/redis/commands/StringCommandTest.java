// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static com.lambdaworks.redis.SetArgs.Builder.ex;
import static com.lambdaworks.redis.SetArgs.Builder.nx;
import static com.lambdaworks.redis.SetArgs.Builder.px;
import static com.lambdaworks.redis.SetArgs.Builder.xx;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.codec.ByteArrayCodec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class StringCommandTest extends AbstractRedisClientTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void append() throws Exception {
        assertThat(redis.append(key, value)).isEqualTo( value.length() );
        assertThat(redis.append(key, "X")).isEqualTo( value.length() + 1 );
    }

    @Test
    public void get() throws Exception {
        assertThat(redis.get(key)).isNull();
        redis.set(key, value);
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void getbit() throws Exception {
        assertThat(redis.getbit(key, 0)).isEqualTo(0);
        redis.setbit(key, 0, 1);
        assertThat(redis.getbit(key, 0)).isEqualTo(1);
    }

    @Test
    public void getrange() throws Exception {
        assertThat(redis.getrange(key, 0, -1)).isEqualTo( "" );
        redis.set(key, "foobar");
        assertThat(redis.getrange(key, 2, 4)).isEqualTo( "oba" );
        assertThat(redis.getrange(key, 3, -1)).isEqualTo( "bar" );
    }

    @Test
    public void getset() throws Exception {
        assertThat(redis.getset(key, value)).isNull();
        assertThat(redis.getset(key, "two")).isEqualTo( value );
        assertThat(redis.get(key)).isEqualTo("two");
    }

    @Test
    public void mget() throws Exception {
        setupMget();
        assertThat(redis.mget("one", "two")).isEqualTo(list(kv("one", "1"), kv("two", "2")));
    }

    protected void setupMget() {
        assertThat(redis.mget(key)).isEqualTo(list(KeyValue.empty("key")));
        redis.set("one", "1");
        redis.set("two", "2");
    }

    @Test
    public void mgetStreaming() throws Exception {
        setupMget();

        KeyValueStreamingAdapter<String, String> streamingAdapter = new KeyValueStreamingAdapter<>();
        Long count = redis.mget(streamingAdapter, "one", "two");
        assertThat(count.intValue()).isEqualTo(2);

        assertThat(streamingAdapter.getMap()).containsEntry("one", "1").containsEntry("two", "2");
    }

    @Test
    public void mset() throws Exception {
        assertThat(redis.mget("one", "two")).isEqualTo(list(KeyValue.empty("one"), KeyValue.empty("two")));
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        assertThat(redis.mset(map)).isEqualTo("OK");
        assertThat(redis.mget("one", "two")).isEqualTo(list(kv("one", "1"), kv("two", "2")));
    }

    @Test
    public void msetnx() throws Exception {
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
    public void set() throws Exception {
        assertThat(redis.get(key)).isNull();
        assertThat(redis.set(key, value)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);

        assertThat(redis.set(key, value, px(20000))).isEqualTo("OK");
        assertThat(redis.set(key, value, ex(10))).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.ttl(key)).isGreaterThanOrEqualTo( 9 );

        assertThat(redis.set(key, value, px(10000))).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.ttl(key)).isGreaterThanOrEqualTo( 9 );

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
        assertThat(redis.setbit(key, 0, 1)).isEqualTo(0);
        assertThat(redis.setbit(key, 0, 0)).isEqualTo(1);
    }

    @Test
    public void setex() throws Exception {
        assertThat(redis.setex(key, 10, value)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.ttl(key) >= 9).isTrue();
    }

    @Test
    public void psetex() throws Exception {
        assertThat(redis.psetex(key, 20000, value)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.pttl(key) >= 19000).isTrue();
    }

    @Test
    public void setnx() throws Exception {
        assertThat(redis.setnx(key, value)).isTrue();
        assertThat(redis.setnx(key, value)).isFalse();
    }

    @Test
    public void setrange() throws Exception {
        assertThat(redis.setrange(key, 0, "foo")).isEqualTo("foo".length());
        assertThat(redis.setrange(key, 3, "bar")).isEqualTo(6);
        assertThat(redis.get(key)).isEqualTo("foobar");
    }

    @Test
    public void strlen() throws Exception {
        assertThat((long) redis.strlen(key)).isEqualTo(0);
        redis.set(key, value);
        assertThat((long) redis.strlen(key)).isEqualTo(value.length());
    }

    @Test
    public void time() throws Exception {

        List<String> time = redis.time();
        assertThat(time).hasSize(2);

        Long.parseLong(time.get(0));
        Long.parseLong(time.get(1));
    }
}
