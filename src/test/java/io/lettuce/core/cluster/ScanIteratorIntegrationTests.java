/*
 * Copyright 2016-2020 the original author or authors.
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
package io.lettuce.core.cluster;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.test.KeysAndValues;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanIteratorIntegrationTests extends TestSupport {

    private final StatefulRedisClusterConnection<String, String> connection;

    private final RedisClusterCommands<String, String> redis;

    @Inject
    ScanIteratorIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        this.connection = connection;
        this.redis = connection.sync();
        this.connection.sync().flushall();
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
        this.connection.setReadFrom(ReadFrom.MASTER);
    }

    @Test
    void scanShouldThrowNoSuchElementExceptionOnEmpty() {

        redis.mset(KeysAndValues.MAP);

        ScanIterator<String> scan = ScanIterator.scan(redis, ScanArgs.Builder.limit(50).match("key-foo"));

        assertThat(scan.hasNext()).isFalse();
        try {
            scan.next();
            fail("Missing NoSuchElementException");
        } catch (NoSuchElementException e) {
            assertThat(e).isInstanceOf(NoSuchElementException.class);
        }
    }

    @Test
    void keysSinglePass() {

        redis.mset(KeysAndValues.MAP);

        ScanIterator<String> scan = ScanIterator.scan(redis, ScanArgs.Builder.limit(50).match("key-11*"));

        assertThat(scan.hasNext()).isTrue();
        assertThat(scan.hasNext()).isTrue();

        for (int i = 0; i < 11; i++) {
            assertThat(scan.hasNext()).isTrue();
            assertThat(scan.next()).isNotNull();
        }

        assertThat(scan.hasNext()).isFalse();
    }

    @Test
    void keysMultiPass() {

        redis.mset(KeysAndValues.MAP);

        ScanIterator<String> scan = ScanIterator.scan(redis);

        List<String> keys = scan.stream().collect(Collectors.toList());

        assertThat(keys).containsAll(KeysAndValues.KEYS);
    }

    @Test
    void keysMultiPassFromAnyNode() {

        redis.mset(KeysAndValues.MAP);
        this.connection.setReadFrom(ReadFrom.ANY);

        ScanIterator<String> scan = ScanIterator.scan(redis);

        List<String> keys = scan.stream().collect(Collectors.toList());

        assertThat(keys).containsAll(KeysAndValues.KEYS);
    }

    @Test
    void hscanShouldThrowNoSuchElementExceptionOnEmpty() {

        redis.mset(KeysAndValues.MAP);

        ScanIterator<KeyValue<String, String>> scan = ScanIterator.hscan(redis, "none",
                ScanArgs.Builder.limit(50).match("key-foo"));

        assertThat(scan.hasNext()).isFalse();
        try {
            scan.next();
            fail("Missing NoSuchElementException");
        } catch (NoSuchElementException e) {
            assertThat(e).isInstanceOf(NoSuchElementException.class);
        }
    }

    @Test
    void hashSinglePass() {

        redis.hmset(key, KeysAndValues.MAP);

        ScanIterator<KeyValue<String, String>> scan = ScanIterator.hscan(redis, key,
                ScanArgs.Builder.limit(50).match("key-11*"));

        assertThat(scan.hasNext()).isTrue();
        assertThat(scan.hasNext()).isTrue();

        for (int i = 0; i < 11; i++) {
            assertThat(scan.hasNext()).isTrue();
            assertThat(scan.next()).isNotNull();
        }

        assertThat(scan.hasNext()).isFalse();
    }

    @Test
    void hashMultiPass() {

        redis.hmset(key, KeysAndValues.MAP);

        ScanIterator<KeyValue<String, String>> scan = ScanIterator.hscan(redis, key);

        List<KeyValue<String, String>> keys = scan.stream().collect(Collectors.toList());

        assertThat(keys).containsAll(KeysAndValues.KEYS.stream().map(s -> KeyValue.fromNullable(s, KeysAndValues.MAP.get(s)))
                .collect(Collectors.toList()));
    }

    @Test
    void sscanShouldThrowNoSuchElementExceptionOnEmpty() {

        redis.sadd(key, KeysAndValues.VALUES.toArray(new String[0]));

        ScanIterator<String> scan = ScanIterator.sscan(redis, "none", ScanArgs.Builder.limit(50).match("key-foo"));

        assertThat(scan.hasNext()).isFalse();
        try {
            scan.next();
            fail("Missing NoSuchElementException");
        } catch (NoSuchElementException e) {
            assertThat(e).isInstanceOf(NoSuchElementException.class);
        }
    }

    @Test
    void setSinglePass() {
        redis.sadd(key, KeysAndValues.KEYS.toArray(new String[0]));

        ScanIterator<String> scan = ScanIterator.sscan(redis, key, ScanArgs.Builder.limit(50).match("key-11*"));

        assertThat(scan.hasNext()).isTrue();
        assertThat(scan.hasNext()).isTrue();

        for (int i = 0; i < 11; i++) {
            assertThat(scan.hasNext()).isTrue();
            assertThat(scan.next()).isNotNull();
        }

        assertThat(scan.hasNext()).isFalse();
    }

    @Test
    void setMultiPass() {

        redis.sadd(key, KeysAndValues.KEYS.toArray(new String[0]));

        ScanIterator<String> scan = ScanIterator.sscan(redis, key);

        List<String> values = scan.stream().collect(Collectors.toList());

        assertThat(values).containsAll(values);
    }

    @Test
    void zscanShouldThrowNoSuchElementExceptionOnEmpty() {

        for (int i = 0; i < KeysAndValues.COUNT; i++) {
            redis.zadd(key, ScoredValue.just(i, KeysAndValues.KEYS.get(i)));
        }

        ScanIterator<ScoredValue<String>> scan = ScanIterator.zscan(redis, "none", ScanArgs.Builder.limit(50).match("key-foo"));

        assertThat(scan.hasNext()).isFalse();
        try {
            scan.next();
            fail("Missing NoSuchElementException");
        } catch (NoSuchElementException e) {
            assertThat(e).isInstanceOf(NoSuchElementException.class);
        }
    }

    @Test
    void zsetSinglePass() {

        for (int i = 0; i < KeysAndValues.COUNT; i++) {
            redis.zadd(key, ScoredValue.just(i, KeysAndValues.KEYS.get(i)));
        }

        ScanIterator<ScoredValue<String>> scan = ScanIterator.zscan(redis, key, ScanArgs.Builder.limit(50).match("key-11*"));

        assertThat(scan.hasNext()).isTrue();
        assertThat(scan.hasNext()).isTrue();

        for (int i = 0; i < 11; i++) {
            assertThat(scan.hasNext()).isTrue();
            assertThat(scan.next()).isNotNull();
        }

        assertThat(scan.hasNext()).isFalse();
    }

    @Test
    void zsetMultiPass() {

        List<ScoredValue<String>> expected = new ArrayList<>();
        for (int i = 0; i < KeysAndValues.COUNT; i++) {
            ScoredValue<String> scoredValue = ScoredValue.just(i, KeysAndValues.KEYS.get(i));
            expected.add(scoredValue);
            redis.zadd(key, scoredValue);
        }

        ScanIterator<ScoredValue<String>> scan = ScanIterator.zscan(redis, key);

        List<ScoredValue<String>> values = scan.stream().collect(Collectors.toList());

        assertThat(values).containsAll(values);
    }

}
