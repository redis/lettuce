/*
 * Copyright 2011-2016 the original author or authors.
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
package io.lettuce.core.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import io.lettuce.core.*;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
public class SetCommandTest extends AbstractRedisClientTest {

    @Test
    public void sadd() throws Exception {
        assertThat(redis.sadd(key, "a")).isEqualTo(1L);
        assertThat(redis.sadd(key, "a")).isEqualTo(0);
        assertThat(redis.smembers(key)).isEqualTo(set("a"));
        assertThat(redis.sadd(key, "b", "c")).isEqualTo(2);
        assertThat(redis.smembers(key)).isEqualTo(set("a", "b", "c"));
    }

    @Test
    public void scard() throws Exception {
        assertThat((long) redis.scard(key)).isEqualTo(0);
        redis.sadd(key, "a");
        assertThat((long) redis.scard(key)).isEqualTo(1);
    }

    @Test
    public void sdiff() throws Exception {
        setupSet();
        assertThat(redis.sdiff("key1", "key2", "key3")).isEqualTo(set("b", "d"));
    }

    @Test
    public void sdiffStreaming() throws Exception {
        setupSet();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();

        Long count = redis.sdiff(streamingAdapter, "key1", "key2", "key3");
        assertThat(count.intValue()).isEqualTo(2);
        assertThat(streamingAdapter.getList()).containsOnly("b", "d");
    }

    @Test
    public void sdiffstore() throws Exception {
        setupSet();
        assertThat(redis.sdiffstore("newset", "key1", "key2", "key3")).isEqualTo(2);
        assertThat(redis.smembers("newset")).containsOnly("b", "d");
    }

    @Test
    public void sinter() throws Exception {
        setupSet();
        assertThat(redis.sinter("key1", "key2", "key3")).isEqualTo(set("c"));
    }

    @Test
    public void sinterStreaming() throws Exception {
        setupSet();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();
        Long count = redis.sinter(streamingAdapter, "key1", "key2", "key3");

        assertThat(count.intValue()).isEqualTo(1);
        assertThat(streamingAdapter.getList()).containsExactly("c");
    }

    @Test
    public void sinterstore() throws Exception {
        setupSet();
        assertThat(redis.sinterstore("newset", "key1", "key2", "key3")).isEqualTo(1);
        assertThat(redis.smembers("newset")).containsExactly("c");
    }

    @Test
    public void sismember() throws Exception {
        assertThat(redis.sismember(key, "a")).isFalse();
        redis.sadd(key, "a");
        assertThat(redis.sismember(key, "a")).isTrue();
    }

    @Test
    public void smove() throws Exception {
        redis.sadd(key, "a", "b", "c");
        assertThat(redis.smove(key, "key1", "d")).isFalse();
        assertThat(redis.smove(key, "key1", "a")).isTrue();
        assertThat(redis.smembers(key)).isEqualTo(set("b", "c"));
        assertThat(redis.smembers("key1")).isEqualTo(set("a"));
    }

    @Test
    public void smembers() throws Exception {
        setupSet();
        assertThat(redis.smembers(key)).isEqualTo(set("a", "b", "c"));
    }

    @Test
    public void smembersStreaming() throws Exception {
        setupSet();
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();
        Long count = redis.smembers(streamingAdapter, key);
        assertThat(count.longValue()).isEqualTo(3);
        assertThat(streamingAdapter.getList()).containsOnly("a", "b", "c");
    }

    @Test
    public void spop() throws Exception {
        assertThat(redis.spop(key)).isNull();
        redis.sadd(key, "a", "b", "c");
        String rand = redis.spop(key);
        assertThat(set("a", "b", "c").contains(rand)).isTrue();
        assertThat(redis.smembers(key).contains(rand)).isFalse();
    }

    @Test
    public void spopMultiple() throws Exception {
        assertThat(redis.spop(key)).isNull();
        redis.sadd(key, "a", "b", "c");
        Set<String> rand = redis.spop(key, 2);
        assertThat(rand).hasSize(2);
        assertThat(set("a", "b", "c").containsAll(rand)).isTrue();
    }

    @Test
    public void srandmember() throws Exception {
        assertThat(redis.spop(key)).isNull();
        redis.sadd(key, "a", "b", "c", "d");
        assertThat(set("a", "b", "c", "d").contains(redis.srandmember(key))).isTrue();
        assertThat(redis.smembers(key)).isEqualTo(set("a", "b", "c", "d"));
        List<String> rand = redis.srandmember(key, 3);
        assertThat(rand).hasSize(3);
        assertThat(set("a", "b", "c", "d").containsAll(rand)).isTrue();
        List<String> randWithDuplicates = redis.srandmember(key, -10);
        assertThat(randWithDuplicates).hasSize(10);
    }

    @Test
    public void srandmemberStreaming() throws Exception {
        assertThat(redis.spop(key)).isNull();
        redis.sadd(key, "a", "b", "c", "d");

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();

        Long count = redis.srandmember(streamingAdapter, key, 2);

        assertThat(count.longValue()).isEqualTo(2);

        assertThat(set("a", "b", "c", "d").containsAll(streamingAdapter.getList())).isTrue();

    }

    @Test
    public void srem() throws Exception {
        redis.sadd(key, "a", "b", "c");
        assertThat(redis.srem(key, "d")).isEqualTo(0);
        assertThat(redis.srem(key, "b")).isEqualTo(1);
        assertThat(redis.smembers(key)).isEqualTo(set("a", "c"));
        assertThat(redis.srem(key, "a", "c")).isEqualTo(2);
        assertThat(redis.smembers(key)).isEqualTo(set());
    }

    @Test(expected = IllegalArgumentException.class)
    public void sremEmpty() throws Exception {
        redis.srem(key);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sremNulls() throws Exception {
        redis.srem(key, new String[0]);
    }

    @Test
    public void sunion() throws Exception {
        setupSet();
        assertThat(redis.sunion("key1", "key2", "key3")).isEqualTo(set("a", "b", "c", "d", "e"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void sunionEmpty() throws Exception {
        redis.sunion();
    }

    @Test
    public void sunionStreaming() throws Exception {
        setupSet();

        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        Long count = redis.sunion(adapter, "key1", "key2", "key3");

        assertThat(count.longValue()).isEqualTo(5);

        assertThat(new TreeSet<String>(adapter.getList())).isEqualTo(new TreeSet<String>(list("c", "a", "b", "e", "d")));
    }

    @Test
    public void sunionstore() throws Exception {
        setupSet();
        assertThat(redis.sunionstore("newset", "key1", "key2", "key3")).isEqualTo(5);
        assertThat(redis.smembers("newset")).isEqualTo(set("a", "b", "c", "d", "e"));
    }

    @Test
    public void sscan() throws Exception {
        redis.sadd(key, value);
        ValueScanCursor<String> cursor = redis.sscan(key);

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getValues()).isEqualTo(list(value));
    }

    @Test
    public void sscanWithCursor() throws Exception {
        redis.sadd(key, value);
        ValueScanCursor<String> cursor = redis.sscan(key, ScanCursor.INITIAL);

        assertThat(cursor.getValues()).hasSize(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    public void sscanWithCursorAndArgs() throws Exception {
        redis.sadd(key, value);

        ValueScanCursor<String> cursor = redis.sscan(key, ScanCursor.INITIAL, ScanArgs.Builder.limit(5));

        assertThat(cursor.getValues()).hasSize(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

    }

    @Test
    public void sscanStreaming() throws Exception {
        redis.sadd(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        StreamScanCursor cursor = redis.sscan(adapter, key);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getList()).isEqualTo(list(value));
    }

    @Test
    public void sscanStreamingWithCursor() throws Exception {
        redis.sadd(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        StreamScanCursor cursor = redis.sscan(adapter, key, ScanCursor.INITIAL);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    public void sscanStreamingWithCursorAndArgs() throws Exception {
        redis.sadd(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        StreamScanCursor cursor = redis.sscan(adapter, key, ScanCursor.INITIAL, ScanArgs.Builder.limit(5));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    public void sscanStreamingArgs() throws Exception {
        redis.sadd(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        StreamScanCursor cursor = redis.sscan(adapter, key, ScanArgs.Builder.limit(100).match("*"));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getList()).isEqualTo(list(value));
    }

    @Test
    public void sscanMultiple() throws Exception {

        Set<String> expect = new HashSet<>();
        Set<String> check = new HashSet<>();
        setup100KeyValues(expect);

        ValueScanCursor<String> cursor = redis.sscan(key, ScanArgs.Builder.limit(5));

        assertThat(cursor.getCursor()).isNotNull().isNotEqualTo("0");
        assertThat(cursor.isFinished()).isFalse();

        check.addAll(cursor.getValues());

        while (!cursor.isFinished()) {
            cursor = redis.sscan(key, cursor);
            check.addAll(cursor.getValues());
        }

        assertThat(new TreeSet<String>(check)).isEqualTo(new TreeSet<String>(expect));
    }

    @Test
    public void scanMatch() throws Exception {

        Set<String> expect = new HashSet<>();
        setup100KeyValues(expect);

        ValueScanCursor<String> cursor = redis.sscan(key, ScanArgs.Builder.limit(200).match("value1*"));

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

        assertThat(cursor.getValues()).hasSize(11);
    }

    protected void setup100KeyValues(Set<String> expect) {
        for (int i = 0; i < 100; i++) {
            redis.sadd(key, value + i);
            expect.add(value + i);
        }
    }

    private void setupSet() {
        redis.sadd(key, "a", "b", "c");
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
    }

}
