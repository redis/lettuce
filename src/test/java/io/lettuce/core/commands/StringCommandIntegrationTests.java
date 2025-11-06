/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.SetArgs.Builder.*;
import static io.lettuce.core.StringMatchResult.Position;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.test.condition.RedisConditions;

import javax.inject.Inject;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
 * @author Andrey Shlykov
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StringCommandIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    protected final String KEY_1 = "key1{k}";

    protected final String KEY_2 = "key2{k}";

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
    @EnabledOnCommand("GETDEL")
    void getdel() {
        redis.set(key, value);
        assertThat(redis.getdel(key)).isEqualTo(value);
        assertThat(redis.get(key)).isNull();
    }

    @Test
    @EnabledOnCommand("GETEX")
    void getex() {
        redis.set(key, value);
        assertThat(redis.getex(key, GetExArgs.Builder.ex(Duration.ofSeconds(100)))).isEqualTo(value);
        assertThat(redis.ttl(key)).isGreaterThan(1);
        assertThat(redis.getex(key, GetExArgs.Builder.persist())).isEqualTo(value);
        assertThat(redis.ttl(key)).isEqualTo(-1);
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

        assertThat(redis.set(key, value, ex(Duration.ofSeconds(10)))).isEqualTo("OK");
        assertThat(redis.ttl(key)).isBetween(5L, 10L);

        assertThat(redis.set(key, value, px(Duration.ofSeconds(10)))).isEqualTo("OK");
        assertThat(redis.ttl(key)).isBetween(5L, 10L);

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
    @EnabledOnCommand("ZMSCORE") // Redis 6.2
    void setExAt() {

        assertThat(redis.set(key, value, exAt(Instant.now().plusSeconds(60)))).isEqualTo("OK");
        assertThat(redis.ttl(key)).isBetween(50L, 61L);

        assertThat(redis.set(key, value, pxAt(Instant.now().plusSeconds(60)))).isEqualTo("OK");
        assertThat(redis.ttl(key)).isBetween(50L, 61L);
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
    @EnabledOnCommand("ZMSCORE") // Redis 6.2
    void setGet() {
        assertThat(redis.setGet(key, value)).isNull();
        assertThat(redis.setGet(key, "value2")).isEqualTo(value);
        assertThat(redis.get(key)).isEqualTo("value2");
    }

    @Test
    @EnabledOnCommand("ZMSCORE") // Redis 6.2
    void setGetWithArgs() {
        assertThat(redis.setGet(key, value)).isNull();
        assertThat(redis.setGet(key, "value2", SetArgs.Builder.ex(100))).isEqualTo(value);
        assertThat(redis.get(key)).isEqualTo("value2");
        assertThat(redis.ttl(key)).isGreaterThanOrEqualTo(10);
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

        redis.set(KEY_1, "ohmytext");
        redis.set(KEY_2, "mynewtext");

        StringMatchResult matchResult = redis.stralgoLcs(StrAlgoArgs.Builder.keys(KEY_1, KEY_2));
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

    @Test
    @EnabledOnCommand("LCS")
    void lcs() {
        redis.set(KEY_1, "ohmytext");
        redis.set(KEY_2, "mynewtext");

        // > LCS key1 key2
        StringMatchResult matchResult = redis.lcs(LcsArgs.Builder.keys(KEY_1, KEY_2));
        assertThat(matchResult.getMatchString()).isEqualTo("mytext");
        assertThat(matchResult.getMatches().size()).isEqualTo(0);
        assertThat(matchResult.getLen()).isEqualTo(0);

    }

    @Test
    @EnabledOnCommand("LCS")
    void lcsNonExistantKeys() {

        // > LCS a b IDX MINMATCHLEN 4 WITHMATCHLEN
        // Keys don't exist.
        StringMatchResult matchResult = redis.lcs(LcsArgs.Builder.keys("a{k}", "b{k}").minMatchLen(4).withMatchLen());
        assertThat(matchResult.getMatchString()).isNullOrEmpty();
        assertThat(matchResult.getMatches()).isNullOrEmpty();
        assertThat(matchResult.getLen()).isEqualTo(0);
    }

    @Test
    @EnabledOnCommand("LCS")
    void lcsJustLen() {
        redis.set(KEY_1, "ohmytext");
        redis.set(KEY_2, "mynewtext");

        // > LCS key1 key2 LEN
        StringMatchResult matchResult = redis.lcs(LcsArgs.Builder.keys(KEY_1, KEY_2).justLen());
        assertThat(matchResult.getLen()).isEqualTo(6);
        assertThat(matchResult.getMatchString()).isNullOrEmpty();
        assertThat(matchResult.getMatches()).isNullOrEmpty();
    }

    @Test
    @EnabledOnCommand("LCS")
    void lcsIdx() {
        redis.set(KEY_1, "ohmytext");
        redis.set(KEY_2, "mynewtext");

        // > LCS key1 key2 IDX
        StringMatchResult matchResult = redis.lcs(LcsArgs.Builder.keys(KEY_1, KEY_2).withIdx());

        assertThat(matchResult.getMatches().size()).isEqualTo(2);
        assertThat(matchResult.getMatches().get(0).getA().getStart()).isEqualTo(4);
        assertThat(matchResult.getMatches().get(0).getA().getEnd()).isEqualTo(7);
        assertThat(matchResult.getMatches().get(0).getB().getStart()).isEqualTo(5);
        assertThat(matchResult.getMatches().get(0).getB().getEnd()).isEqualTo(8);

        assertThat(matchResult.getMatches().get(1).getA().getStart()).isEqualTo(2);
        assertThat(matchResult.getMatches().get(1).getA().getEnd()).isEqualTo(3);
        assertThat(matchResult.getMatches().get(1).getB().getStart()).isEqualTo(0);
        assertThat(matchResult.getMatches().get(1).getB().getEnd()).isEqualTo(1);

        assertThat(matchResult.getLen()).isEqualTo(6);

        assertThat(matchResult.getMatchString()).isNullOrEmpty();
    }

    @Test
    @EnabledOnCommand("LCS")
    void lcsWithMinMatchLen() {
        redis.set(KEY_1, "ohmytext");
        redis.set(KEY_2, "mynewtext");

        // > LCS key1 key2 IDX MINMATCHLEN 4
        StringMatchResult matchResult = redis.lcs(LcsArgs.Builder.keys(KEY_1, KEY_2).withIdx().minMatchLen(4));

        assertThat(matchResult.getMatches().get(0).getA().getStart()).isEqualTo(4);
        assertThat(matchResult.getMatches().get(0).getA().getEnd()).isEqualTo(7);
        assertThat(matchResult.getMatches().get(0).getB().getStart()).isEqualTo(5);
        assertThat(matchResult.getMatches().get(0).getB().getEnd()).isEqualTo(8);

        assertThat(matchResult.getLen()).isEqualTo(6);

        assertThat(matchResult.getMatchString()).isNullOrEmpty();
    }

    @Test
    @EnabledOnCommand("LCS")
    void lcsMinMatchLenIdxMatchLen() {
        redis.set(KEY_1, "ohmytext");
        redis.set(KEY_2, "mynewtext");

        // > LCS key1 key2 IDX MINMATCHLEN 4 WITHMATCHLEN
        StringMatchResult matchResult = redis.lcs(LcsArgs.Builder.keys(KEY_1, KEY_2).minMatchLen(4).withMatchLen().withIdx());

        assertThat(matchResult.getMatches().get(0).getA().getStart()).isEqualTo(4);
        assertThat(matchResult.getMatches().get(0).getA().getEnd()).isEqualTo(7);
        assertThat(matchResult.getMatches().get(0).getB().getStart()).isEqualTo(5);
        assertThat(matchResult.getMatches().get(0).getB().getEnd()).isEqualTo(8);

        assertThat(matchResult.getMatches().get(0).getMatchLen()).isEqualTo(4);

        assertThat(matchResult.getLen()).isEqualTo(6);

        assertThat(matchResult.getMatchString()).isNullOrEmpty();
    }

    @Test
    @EnabledOnCommand("DIGEST")
    void digestKey_returnsHex_and_changesOnValueChange() {

        assertThat(redis.digestKey("d")).isNull();
        redis.set("d", "v1");
        String d1 = redis.digestKey("d");
        assertThat(d1).isNotNull().matches("[0-9a-f]{16}");
        assertThat(redis.digestKey("d")).isEqualTo(d1);
        redis.set("d", "v2");
        assertThat(redis.digestKey("d")).isNotEqualTo(d1);
    }

    @Test
    @EnabledOnCommand("DELEX")
    void set_with_ValueCondition_exists_notExists() {

        String k = "k:nx-xx";
        // key does not exist: EXISTS/XX should abort
        assertThat(redis.set(k, "v", ValueCondition.<String> exists())).isNull();
        // NOT_EXISTS/NX should succeed
        assertThat(redis.set(k, "v", ValueCondition.<String> notExists())).isEqualTo("OK");
        // NOT_EXISTS/NX should now abort
        assertThat(redis.set(k, "v2", ValueCondition.<String> notExists())).isNull();
        // EXISTS/XX should succeed
        assertThat(redis.set(k, "v2", ValueCondition.<String> exists())).isEqualTo("OK");
        assertThat(redis.get(k)).isEqualTo("v2");
    }

    @Test
    @EnabledOnCommand("DELEX")
    void set_with_ValueCondition_equal_notEqual() {

        String k = "k:ifeq-ifne";
        redis.set(k, "v1");
        // wrong equality value -> abort
        assertThat(redis.set(k, "v2", ValueCondition.equal("nope"))).isNull();
        assertThat(redis.get(k)).isEqualTo("v1");
        // correct equality value -> success
        assertThat(redis.set(k, "v2", ValueCondition.equal("v1"))).isEqualTo("OK");
        assertThat(redis.get(k)).isEqualTo("v2");
        // not-equal that fails (current is v2)
        assertThat(redis.set(k, "v3", ValueCondition.notEqual("v2"))).isNull();
        assertThat(redis.get(k)).isEqualTo("v2");
        // not-equal that succeeds
        assertThat(redis.set(k, "v3", ValueCondition.notEqual("other"))).isEqualTo("OK");
        assertThat(redis.get(k)).isEqualTo("v3");
    }

    @Test
    @EnabledOnCommand("DELEX")
    void setget_with_ValueCondition_returnsPreviousValue() {

        String k = "k:setget";
        redis.set(k, "A");
        // condition mismatch -> server returns previous value, value unchanged
        assertThat(redis.setGet(k, "B", ValueCondition.equal("X"))).isEqualTo("A");
        assertThat(redis.get(k)).isEqualTo("A");
        // condition match -> returns previous and updates
        assertThat(redis.setGet(k, "C", ValueCondition.equal("A"))).isEqualTo("A");
        assertThat(redis.get(k)).isEqualTo("C");
    }

    @Test
    @EnabledOnCommand("DIGEST")
    void set_with_digest_conditions() {

        String k = "k:digest";
        redis.set(k, "C");
        String d = redis.digestKey(k);
        assertThat(d).isNotNull();
        // digest equal -> success
        assertThat(redis.set(k, "D", ValueCondition.<String> digestEqualHex(d))).isEqualTo("OK");
        // reusing old digest equal -> abort
        assertThat(redis.set(k, "E", ValueCondition.<String> digestEqualHex(d))).isNull();
        // digest not equal (against old digest) -> success
        assertThat(redis.set(k, "F", ValueCondition.<String> digestNotEqualHex(d))).isEqualTo("OK");
    }

    @Test
    @EnabledOnCommand("DIGEST")
    void set_with_digestNotEqual_fails_when_equal() {
        String k = "k:dig-not-eq-fail";

        redis.set(k, "X");
        String d = redis.digestKey(k);
        assertThat(redis.set(k, "Y", ValueCondition.<String> digestNotEqualHex(d))).isNull();
        assertThat(redis.get(k)).isEqualTo("X");
    }

    @Test
    @EnabledOnCommand("DIGEST")
    void setget_with_digest_conditions() {
        String k = "k:setget-dig";

        redis.set(k, "A");
        String dA = redis.digestKey(k);
        // match -> returns previous and updates
        assertThat(redis.setGet(k, "B", ValueCondition.<String> digestEqualHex(dA))).isEqualTo("A");
        assertThat(redis.get(k)).isEqualTo("B");
        // mismatch (using old digest) -> returns previous and does not update
        assertThat(redis.setGet(k, "C", ValueCondition.<String> digestEqualHex(dA))).isEqualTo("B");
        assertThat(redis.get(k)).isEqualTo("B");
    }

    @Test
    @EnabledOnCommand("DIGEST")
    void digestKey_emptyValue() {
        String k = "k:empty";
        redis.set(k, "");
        String d = redis.digestKey(k);

        assertThat(d).isNotNull().matches("[0-9a-f]{16}");
        assertThat(redis.digestKey(k)).isEqualTo(d);
    }

    @Test
    void setget_with_exists_notExists_conditions() {
        Assumptions.assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.3.224"));

        String k1 = "k:setget-nx";
        // NX when missing -> perform set, returns previous (null)
        assertThat(redis.setGet(k1, "A", ValueCondition.notExists())).isNull();
        assertThat(redis.get(k1)).isEqualTo("A");
        // NX when present -> no set, returns previous
        assertThat(redis.setGet(k1, "B", ValueCondition.notExists())).isEqualTo("A");
        assertThat(redis.get(k1)).isEqualTo("A");

        String k2 = "k:setget-xx";
        // XX when missing -> no-op, returns previous (null)
        redis.del(k2);
        assertThat(redis.setGet(k2, "X", ValueCondition.exists())).isNull();
        assertThat(redis.get(k2)).isNull();
        // XX when present -> set, returns previous
        redis.set(k2, "Y");
        assertThat(redis.setGet(k2, "Z", ValueCondition.exists())).isEqualTo("Y");
        assertThat(redis.get(k2)).isEqualTo("Z");
    }

    @Test
    void set_with_SetArgs_and_ValueCondition_combination() {
        String k = "k:set-args-cond";
        // NX + EX should set with TTL
        Assumptions.assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.3.224"));

        assertThat(redis.set(k, "v1", ex(100), ValueCondition.notExists())).isEqualTo("OK");
        assertThat(redis.ttl(k)).isGreaterThan(0);
        // NX when present should abort and keep value/ttl
        Long ttlBefore = redis.ttl(k);
        assertThat(redis.set(k, "v2", ex(100), ValueCondition.notExists())).isNull();
        assertThat(redis.get(k)).isEqualTo("v1");
        assertThat(redis.ttl(k)).isGreaterThan(0);
        assertThat(redis.ttl(k)).isLessThanOrEqualTo(ttlBefore);
    }

}
