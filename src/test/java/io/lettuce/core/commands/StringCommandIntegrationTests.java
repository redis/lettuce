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

import static io.lettuce.core.SetArgs.Builder.*;
import static io.lettuce.core.StringMatchResult.Position;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.KeyValueStreamingAdapter;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisStringCommands}.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author dengliming
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StringCommandIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    protected StringCommandIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void append() {
        assertThat(redis.append(key, value)).isEqualTo(value.length());
        assertThat(redis.append(key, "X")).isEqualTo(value.length() + 1);
    }

    @Test
    void get() {
        assertThat(redis.get(key)).isNull();
        redis.set(key, value);
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    void getbit() {
        assertThat(redis.getbit(key, 0)).isEqualTo(0);
        redis.setbit(key, 0, 1);
        assertThat(redis.getbit(key, 0)).isEqualTo(1);
    }

    @Test
    void getrange() {
        assertThat(redis.getrange(key, 0, -1)).isEqualTo("");
        redis.set(key, "foobar");
        assertThat(redis.getrange(key, 2, 4)).isEqualTo("oba");
        assertThat(redis.getrange(key, 3, -1)).isEqualTo("bar");
    }

    @Test
    void getset() {
        assertThat(redis.getset(key, value)).isNull();
        assertThat(redis.getset(key, "two")).isEqualTo(value);
        assertThat(redis.get(key)).isEqualTo("two");
    }

    @Test
    void mget() {
        setupMget();
        assertThat(redis.mget("one", "two")).isEqualTo(list(kv("one", "1"), kv("two", "2")));
    }

    protected void setupMget() {
        assertThat(redis.mget(key)).isEqualTo(list(KeyValue.empty("key")));
        redis.set("one", "1");
        redis.set("two", "2");
    }

    @Test
    void mgetStreaming() {
        setupMget();

        KeyValueStreamingAdapter<String, String> streamingAdapter = new KeyValueStreamingAdapter<>();
        Long count = redis.mget(streamingAdapter, "one", "two");
        assertThat(count.intValue()).isEqualTo(2);

        assertThat(streamingAdapter.getMap()).containsEntry("one", "1").containsEntry("two", "2");
    }

    @Test
    void mset() {
        assertThat(redis.mget("one", "two")).isEqualTo(list(KeyValue.empty("one"), KeyValue.empty("two")));
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        assertThat(redis.mset(map)).isEqualTo("OK");
        assertThat(redis.mget("one", "two")).isEqualTo(list(kv("one", "1"), kv("two", "2")));
    }

    @Test
    void msetnx() {
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
    void set() {
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

    @Test
    @EnabledOnCommand("ACL") // Redis 6.0 guard
    void setKeepTTL() {

        redis.set(key, value, ex(10));

        assertThat(redis.set(key, "value2", keepttl())).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo("value2");
        assertThat(redis.ttl(key) >= 1).isTrue();
    }

    @Test
    void setNegativeEX() {
        assertThatThrownBy(() -> redis.set(key, value, ex(-10))).isInstanceOf(RedisException.class);
    }

    @Test
    void setNegativePX() {
        assertThatThrownBy(() -> redis.set(key, value, px(-1000))).isInstanceOf(RedisException.class);
    }

    @Test
    void setbit() {
        assertThat(redis.setbit(key, 0, 1)).isEqualTo(0);
        assertThat(redis.setbit(key, 0, 0)).isEqualTo(1);
    }

    @Test
    void setex() {
        assertThat(redis.setex(key, 10, value)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.ttl(key) >= 9).isTrue();
    }

    @Test
    void psetex() {
        assertThat(redis.psetex(key, 20000, value)).isEqualTo("OK");
        assertThat(redis.get(key)).isEqualTo(value);
        assertThat(redis.pttl(key) >= 19000).isTrue();
    }

    @Test
    void setnx() {
        assertThat(redis.setnx(key, value)).isTrue();
        assertThat(redis.setnx(key, value)).isFalse();
    }

    @Test
    void setrange() {
        assertThat(redis.setrange(key, 0, "foo")).isEqualTo("foo".length());
        assertThat(redis.setrange(key, 3, "bar")).isEqualTo(6);
        assertThat(redis.get(key)).isEqualTo("foobar");
    }

    @Test
    void strlen() {
        assertThat((long) redis.strlen(key)).isEqualTo(0);
        redis.set(key, value);
        assertThat((long) redis.strlen(key)).isEqualTo(value.length());
    }

    @Test
    void time() {

        List<String> time = redis.time();
        assertThat(time).hasSize(2);

        Long.parseLong(time.get(0));
        Long.parseLong(time.get(1));
    }

    @Test
    @EnabledOnCommand("STRALGO")
    void strAlgo() {

        StringMatchResult matchResult = redis.stralgoLcs(StrAlgoArgs.Builder.strings("ohmytext", "mynewtext"));
        assertThat(matchResult.getMatchString()).isEqualTo("mytext");

        // STRALGO LCS STRINGS a b
        matchResult = redis.stralgoLcs(StrAlgoArgs.Builder.strings("a", "b").minMatchLen(4).withIdx().withMatchLen());
        assertThat(matchResult.getMatchString()).isNullOrEmpty();
        assertThat(matchResult.getLen()).isEqualTo(0);
    }

    @Test
    @EnabledOnCommand("STRALGO")
    void strAlgoUsingKeys() {

        redis.set("key1{k}", "ohmytext");
        redis.set("key2{k}", "mynewtext");

        StringMatchResult matchResult = redis.stralgoLcs(StrAlgoArgs.Builder.keys("key1{k}", "key2{k}"));
        assertThat(matchResult.getMatchString()).isEqualTo("mytext");

        // STRALGO LCS STRINGS a b
        matchResult = redis.stralgoLcs(StrAlgoArgs.Builder.strings("a", "b").minMatchLen(4).withIdx().withMatchLen());
        assertThat(matchResult.getMatchString()).isNullOrEmpty();
        assertThat(matchResult.getLen()).isEqualTo(0);
    }

    @Test
    @EnabledOnCommand("STRALGO")
    void strAlgoJustLen() {

        StringMatchResult matchResult = redis.stralgoLcs(StrAlgoArgs.Builder.strings("ohmytext", "mynewtext").justLen());

        assertThat(matchResult.getLen()).isEqualTo(6);
    }

    @Test
    @EnabledOnCommand("STRALGO")
    void strAlgoWithMinMatchLen() {

        StringMatchResult matchResult = redis.stralgoLcs(StrAlgoArgs.Builder.strings("ohmytext", "mynewtext").minMatchLen(4));

        assertThat(matchResult.getMatchString()).isEqualTo("mytext");
    }

    @Test
    @EnabledOnCommand("STRALGO")
    void strAlgoWithIdx() {

        // STRALGO LCS STRINGS ohmytext mynewtext IDX MINMATCHLEN 4 WITHMATCHLEN
        StringMatchResult matchResult = redis
                .stralgoLcs(StrAlgoArgs.Builder.strings("ohmytext", "mynewtext").minMatchLen(4).withIdx().withMatchLen());

        assertThat(matchResult.getMatches()).hasSize(1);
        assertThat(matchResult.getMatches().get(0).getMatchLen()).isEqualTo(4);

        Position a = matchResult.getMatches().get(0).getA();
        Position b = matchResult.getMatches().get(0).getB();

        assertThat(a.getStart()).isEqualTo(4);
        assertThat(a.getEnd()).isEqualTo(7);
        assertThat(b.getStart()).isEqualTo(5);
        assertThat(b.getEnd()).isEqualTo(8);
        assertThat(matchResult.getLen()).isEqualTo(6);
    }

}
