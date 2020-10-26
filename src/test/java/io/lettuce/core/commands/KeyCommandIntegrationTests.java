/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.*;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.ListStreamingAdapter;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KeyCommandIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    protected KeyCommandIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void del() {
        redis.set(key, value);
        assertThat((long) redis.del(key)).isEqualTo(1);
        redis.set(key + "1", value);
        redis.set(key + "2", value);

        assertThat(redis.del(key + "1", key + "2")).isEqualTo(2);
    }

    @Test
    @EnabledOnCommand("UNLINK")
    void unlink() {

        redis.set(key, value);
        assertThat((long) redis.unlink(key)).isEqualTo(1);
        redis.set(key + "1", value);
        redis.set(key + "2", value);
        assertThat(redis.unlink(key + "1", key + "2")).isEqualTo(2);
    }

    @Test
    void dump() {
        assertThat(redis.dump("invalid")).isNull();
        redis.set(key, value);
        assertThat(redis.dump(key).length > 0).isTrue();
    }

    @Test
    void exists() {
        assertThat(redis.exists(key)).isEqualTo(0);
        redis.set(key, value);
        assertThat(redis.exists(key)).isEqualTo(1);
    }

    @Test
    void existsVariadic() {
        assertThat(redis.exists(key, "key2", "key3")).isEqualTo(0);
        redis.set(key, value);
        redis.set("key2", value);
        assertThat(redis.exists(key, "key2", "key3")).isEqualTo(2);
    }

    @Test
    void expire() {
        assertThat(redis.expire(key, 10)).isFalse();
        redis.set(key, value);
        assertThat(redis.expire(key, 10)).isTrue();
        assertThat((long) redis.ttl(key)).isEqualTo(10);
    }

    @Test
    void expireat() {
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        assertThat(redis.expireat(key, expiration)).isFalse();
        redis.set(key, value);
        assertThat(redis.expireat(key, expiration)).isTrue();

        assertThat(redis.ttl(key)).isGreaterThanOrEqualTo(8);
    }

    @Test
    void keys() {
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
    void keysStreaming() {
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

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
    public void move() {
        redis.set(key, value);
        redis.move(key, 1);
        assertThat(redis.get(key)).isNull();
        redis.select(1);
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    void objectEncoding() {
        redis.set(key, value);
        assertThat(redis.objectEncoding(key)).isEqualTo("embstr");
        redis.set(key, String.valueOf(1));
        assertThat(redis.objectEncoding(key)).isEqualTo("int");
    }

    @Test
    void objectIdletime() {
        redis.set(key, value);
        assertThat((long) redis.objectIdletime(key)).isLessThan(2);
    }

    @Test
    void objectRefcount() {
        redis.set(key, value);
        assertThat(redis.objectRefcount(key)).isGreaterThan(0);
    }

    @Test
    void persist() {
        assertThat(redis.persist(key)).isFalse();
        redis.set(key, value);
        assertThat(redis.persist(key)).isFalse();
        redis.expire(key, 10);
        assertThat(redis.persist(key)).isTrue();
    }

    @Test
    void pexpire() {
        assertThat(redis.pexpire(key, 5000)).isFalse();
        redis.set(key, value);
        assertThat(redis.pexpire(key, 5000)).isTrue();
        assertThat(redis.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);
    }

    @Test
    void pexpireat() {
        Date expiration = new Date(System.currentTimeMillis() + 5000);
        assertThat(redis.pexpireat(key, expiration)).isFalse();
        redis.set(key, value);
        assertThat(redis.pexpireat(key, expiration)).isTrue();
        assertThat(redis.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);
    }

    @Test
    void pttl() {
        assertThat((long) redis.pttl(key)).isEqualTo(-2);
        redis.set(key, value);
        assertThat((long) redis.pttl(key)).isEqualTo(-1);
        redis.pexpire(key, 5000);
        assertThat(redis.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(5000);
    }

    @Test
    void randomkey() {
        assertThat(redis.randomkey()).isNull();
        redis.set(key, value);
        assertThat(redis.randomkey()).isEqualTo(key);
    }

    @Test
    void rename() {
        redis.set(key, value);

        assertThat(redis.rename(key, key + "X")).isEqualTo("OK");
        assertThat(redis.get(key)).isNull();
        assertThat(redis.get(key + "X")).isEqualTo(value);
        redis.set(key, value + "X");
        assertThat(redis.rename(key + "X", key)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    void renameNonexistentKey() {
        assertThatThrownBy(() -> redis.rename(key, key + "X")).isInstanceOf(RedisException.class);
    }

    @Test
    void renamenx() {
        redis.set(key, value);
        assertThat(redis.renamenx(key, key + "X")).isTrue();
        assertThat(redis.get(key + "X")).isEqualTo(value);
        redis.set(key, value);
        assertThat(redis.renamenx(key + "X", key)).isFalse();
    }

    @Test
    void renamenxNonexistentKey() {
        assertThatThrownBy(() -> redis.renamenx(key, key + "X")).isInstanceOf(RedisException.class);
    }

    @Test
    void restore() {
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

        assertThatThrownBy(() -> redis.restore(key, 0, bytes)).isInstanceOf(RedisException.class);
    }

    @Test
    void restoreReplace() {

        redis.set(key, value);
        byte[] bytes = redis.dump(key);
        redis.set(key, "foo");

        assertThat(redis.restore(key, bytes, RestoreArgs.Builder.ttl(Duration.ofSeconds(1)).replace())).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.pttl(key)).isGreaterThan(0).isLessThanOrEqualTo(1000);
    }

    @Test
    @EnabledOnCommand("TOUCH")
    void touch() {

        assertThat((long) redis.touch(key)).isEqualTo(0);
        redis.set(key, value);
        assertThat((long) redis.touch(key, "key2")).isEqualTo(1);
    }

    @Test
    void ttl() {
        assertThat((long) redis.ttl(key)).isEqualTo(-2);
        redis.set(key, value);
        assertThat((long) redis.ttl(key)).isEqualTo(-1);
        redis.expire(key, 10);
        assertThat((long) redis.ttl(key)).isEqualTo(10);
    }

    @Test
    void type() {
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
    void scan() {
        redis.set(key, value);

        KeyScanCursor<String> cursor = redis.scan();
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getKeys()).isEqualTo(list(key));
    }

    @Test
    void scanWithArgs() {
        redis.set(key, value);

        KeyScanCursor<String> cursor = redis.scan(ScanArgs.Builder.limit(10));
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

    }

    @Test
    void scanInitialCursor() {
        redis.set(key, value);

        KeyScanCursor<String> cursor = redis.scan(ScanCursor.INITIAL);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getKeys()).isEqualTo(list(key));
    }

    @Test
    void scanFinishedCursor() {
        redis.set(key, value);
        assertThatThrownBy(() -> redis.scan(ScanCursor.FINISHED)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scanNullCursor() {
        redis.set(key, value);
        assertThatThrownBy(() -> redis.scan((ScanCursor) null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scanStreaming() {
        redis.set(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.scan(adapter);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getList()).isEqualTo(list(key));
    }

    @Test
    void scanStreamingWithCursor() {
        redis.set(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.scan(adapter, ScanCursor.INITIAL);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    void scanStreamingWithCursorAndArgs() {
        redis.set(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.scan(adapter, ScanCursor.INITIAL, ScanArgs.Builder.limit(5));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    void scanStreamingArgs() {
        redis.set(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.scan(adapter, ScanArgs.Builder.limit(100).match("*"));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getList()).isEqualTo(list(key));
    }

    @Test
    void scanMultiple() {

        Set<String> expect = new HashSet<>();
        Set<String> check = new HashSet<>();
        setup100KeyValues(expect);

        KeyScanCursor<String> cursor = redis.scan(ScanArgs.Builder.limit(12));

        assertThat(cursor.getCursor()).isNotNull();
        assertThat(cursor.getCursor()).isNotEqualTo("0");
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
    void scanMatch() {

        Set<String> expect = new HashSet<>();
        setup100KeyValues(expect);

        KeyScanCursor<String> cursor = redis.scan(ScanArgs.Builder.limit(200).match("key1*"));

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

        assertThat(cursor.getKeys()).hasSize(11);
    }

    void setup100KeyValues(Set<String> expect) {
        for (int i = 0; i < 100; i++) {
            redis.set(key + i, value + i);
            expect.add(key + i);
        }
    }
}
