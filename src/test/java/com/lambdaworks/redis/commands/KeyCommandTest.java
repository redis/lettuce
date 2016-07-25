// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;

import java.util.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.lambdaworks.redis.*;

public class KeyCommandTest extends AbstractRedisClientTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void del() throws Exception {
        redis.set(key, value);
        assertThat((long) redis.del(key)).isEqualTo(1);
        redis.set(key + "1", value);
        redis.set(key + "2", value);

        assertThat(redis.del(key + "1", key + "2")).isEqualTo(2);
    }

    @Test
    public void unlink() throws Exception {
        redis.set(key, value);
        assertThat((long) redis.unlink(key)).isEqualTo(1);
        redis.set(key + "1", value);
        redis.set(key + "2", value);
        assertThat(redis.unlink(key + "1", key + "2")).isEqualTo(2);
    }

    @Test
    public void dump() throws Exception {
        assertThat(redis.dump("invalid")).isNull();
        redis.set(key, value);
        assertThat(redis.dump(key).length > 0).isTrue();
    }

    @Test
    public void exists() throws Exception {
        assertThat(redis.exists(key)).isEqualTo(0);
        redis.set(key, value);
        assertThat(redis.exists(key)).isEqualTo(1);
    }

    @Test
    public void existsVariadic() throws Exception {
        assertThat(redis.exists(key, "key2", "key3")).isEqualTo(0);
        redis.set(key, value);
        redis.set("key2", value);
        assertThat(redis.exists(key, "key2", "key3")).isEqualTo(2);
    }

    @Test
    public void expire() throws Exception {
        assertThat(redis.expire(key, 10)).isFalse();
        redis.set(key, value);
        assertThat(redis.expire(key, 10)).isTrue();
        assertThat((long) redis.ttl(key)).isEqualTo(10);
    }

    @Test
    public void expireat() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        assertThat(redis.expireat(key, expiration)).isFalse();
        redis.set(key, value);
        assertThat(redis.expireat(key, expiration)).isTrue();

        assertThat(redis.ttl(key)).isGreaterThanOrEqualTo(8);
    }

    @Test
    public void keys() throws Exception {
        assertThat(redis.keys("*")).isEqualTo(list());
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        redis.mset(map);
        List<String> keys = redis.keys("???");
        assertThat(keys).hasSize(2);
        assertThat(keys.contains("one")).isTrue();
        assertThat(keys.contains("two")).isTrue();
    }

    @Test
    public void keysStreaming() throws Exception {
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        assertThat(redis.keys("*")).isEqualTo(list());
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        redis.mset(map);
        Long count = redis.keys(adapter, "???");
        assertThat(count.intValue()).isEqualTo(2);

        List<String> keys = adapter.getList();
        assertThat(keys).hasSize(2);
        assertThat(keys.contains("one")).isTrue();
        assertThat(keys.contains("two")).isTrue();
    }

    @Test
    public void move() throws Exception {
        redis.set(key, value);
        redis.move(key, 1);
        assertThat(redis.get(key)).isNull();
        redis.select(1);
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void objectEncoding() throws Exception {
        redis.set(key, value);
        assertThat(redis.objectEncoding(key)).isEqualTo("embstr");
        redis.set(key, String.valueOf(1));
        assertThat(redis.objectEncoding(key)).isEqualTo("int");
    }

    @Test
    public void objectIdletime() throws Exception {
        redis.set(key, value);
        assertThat((long) redis.objectIdletime(key)).isLessThan(2);
    }

    @Test
    public void objectRefcount() throws Exception {
        redis.set(key, value);
        assertThat(redis.objectRefcount(key)).isGreaterThan(0);
    }

    @Test
    public void persist() throws Exception {
        assertThat(redis.persist(key)).isFalse();
        redis.set(key, value);
        assertThat(redis.persist(key)).isFalse();
        redis.expire(key, 10);
        assertThat(redis.persist(key)).isTrue();
    }

    @Test
    public void pexpire() throws Exception {
        assertThat(redis.pexpire(key, 5000)).isFalse();
        redis.set(key, value);
        assertThat(redis.pexpire(key, 5000)).isTrue();
        assertThat(redis.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);
    }

    @Test
    public void pexpireat() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + 5000);
        assertThat(redis.pexpireat(key, expiration)).isFalse();
        redis.set(key, value);
        assertThat(redis.pexpireat(key, expiration)).isTrue();
        assertThat(redis.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);
    }

    @Test
    public void pttl() throws Exception {
        assertThat((long) redis.pttl(key)).isEqualTo(-2);
        redis.set(key, value);
        assertThat((long) redis.pttl(key)).isEqualTo(-1);
        redis.pexpire(key, 5000);
        assertThat(redis.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);
    }

    @Test
    public void randomkey() throws Exception {
        assertThat(redis.randomkey()).isNull();
        redis.set(key, value);
        assertThat(redis.randomkey()).isEqualTo(key);
    }

    @Test
    public void rename() throws Exception {
        redis.set(key, value);

        assertThat(redis.rename(key, key + "X")).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
        assertThat(redis.get(key + "X")).isEqualTo(value);
        redis.set(key, value + "X");
        assertThat(redis.rename(key + "X", key)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test(expected = RedisException.class)
    public void renameNonexistentKey() throws Exception {
        redis.rename(key, key + "X");
    }

    @Test
    public void renamenx() throws Exception {
        redis.set(key, value);
        assertThat(redis.renamenx(key, key + "X")).isTrue();
        assertThat(redis.get(key + "X")).isEqualTo(value);
        redis.set(key, value);
        assertThat(redis.renamenx(key + "X", key)).isFalse();
    }

    @Test(expected = RedisException.class)
    public void renamenxNonexistentKey() throws Exception {
        redis.renamenx(key, key + "X");
    }

    @Test
    public void renamenxIdenticalKeys() throws Exception {
        redis.set(key, value);
        assertThat(redis.renamenx(key, key)).isFalse();
    }

    @Test
    public void restore() throws Exception {
        redis.set(key, value);
        byte[] bytes = redis.dump(key);
        redis.del(key);

        assertThat(redis.restore(key, 0, bytes)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.pttl(key).longValue()).isEqualTo(-1);

        redis.del(key);
        assertThat(redis.restore(key, 1000, bytes)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);

        assertThat(redis.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(1000);
    }

    @Test
    public void touch() throws Exception {
        assertThat((long) redis.touch(key)).isEqualTo(0);
        redis.set(key, value);
        assertThat((long) redis.touch(key, "key2")).isEqualTo(1);
    }

    @Test
    public void ttl() throws Exception {
        assertThat((long) redis.ttl(key)).isEqualTo(-2);
        redis.set(key, value);
        assertThat((long) redis.ttl(key)).isEqualTo(-1);
        redis.expire(key, 10);
        assertThat((long) redis.ttl(key)).isEqualTo(10);
    }

    @Test
    public void type() throws Exception {
        assertThat(redis.type(key)).isEqualTo("none");

        redis.set(key, value);
        assertThat(redis.type(key)).isEqualTo("string");

        redis.hset(key + "H", value, "1");
        assertThat(redis.type(key + "H")).isEqualTo("hash");

        redis.lpush(key + "L", "1");
        assertThat(redis.type(key + "L")).isEqualTo("list");

        redis.sadd(key + "S", "1");
        assertThat(redis.type(key + "S")).isEqualTo("set");

        redis.zadd(key + "Z", 1, "1");
        assertThat(redis.type(key + "Z")).isEqualTo("zset");
    }

    @Test
    public void scan() throws Exception {
        redis.set(key, value);

        KeyScanCursor<String> cursor = redis.scan();
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getKeys()).isEqualTo(list(key));
    }

    @Test
    public void scanWithArgs() throws Exception {
        redis.set(key, value);

        KeyScanCursor<String> cursor = redis.scan(ScanArgs.Builder.limit(10));
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

    }

    @Test
    public void scanInitialCursor() throws Exception {
        redis.set(key, value);

        KeyScanCursor<String> cursor = redis.scan(ScanCursor.INITIAL);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getKeys()).isEqualTo(list(key));
    }

    @Test(expected = IllegalArgumentException.class)
    public void scanFinishedCursor() throws Exception {
        redis.set(key, value);
        redis.scan(ScanCursor.FINISHED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void scanNullCursor() throws Exception {
        redis.set(key, value);
        redis.scan((ScanCursor) null);
    }

    @Test
    public void scanStreaming() throws Exception {
        redis.set(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        StreamScanCursor cursor = redis.scan(adapter);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getList()).isEqualTo(list(key));
    }

    @Test
    public void scanStreamingWithCursor() throws Exception {
        redis.set(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        StreamScanCursor cursor = redis.scan(adapter, ScanCursor.INITIAL);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    public void scanStreamingWithCursorAndArgs() throws Exception {
        redis.set(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        StreamScanCursor cursor = redis.scan(adapter, ScanCursor.INITIAL, ScanArgs.Builder.limit(5));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    public void scanStreamingArgs() throws Exception {
        redis.set(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        StreamScanCursor cursor = redis.scan(adapter, ScanArgs.Builder.limit(100).match("*"));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getList()).isEqualTo(list(key));
    }

    @Test
    public void scanMultiple() throws Exception {

        Set<String> expect = new HashSet<>();
        Set<String> check = new HashSet<>();
        setup100KeyValues(expect);

        KeyScanCursor<String> cursor = redis.scan(ScanArgs.Builder.limit(12));

        assertThat(cursor.getCursor()).isNotNull();
        assertNotEquals("0", cursor.getCursor());
        assertThat(cursor.isFinished()).isFalse();

        check.addAll(cursor.getKeys());

        while (!cursor.isFinished()) {
            cursor = redis.scan(cursor);
            check.addAll(cursor.getKeys());
        }

        assertThat(check).isEqualTo(expect);
        assertThat(check).hasSize(100);
    }

    @Test
    public void scanMatch() throws Exception {

        Set<String> expect = new HashSet<>();
        setup100KeyValues(expect);

        KeyScanCursor<String> cursor = redis.scan(ScanArgs.Builder.limit(200).match("key1*"));

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

        assertThat(cursor.getKeys()).hasSize(11);
    }

    protected void setup100KeyValues(Set<String> expect) {
        for (int i = 0; i < 100; i++) {
            redis.set(key + i, value + i);
            expect.add(key + i);
        }
    }
}
