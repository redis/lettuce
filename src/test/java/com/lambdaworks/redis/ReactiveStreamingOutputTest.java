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
package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import rx.observers.TestSubscriber;

import com.lambdaworks.KeysAndValues;
import com.lambdaworks.RedisConditions;
import com.lambdaworks.redis.GeoArgs.Unit;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;

public class ReactiveStreamingOutputTest extends AbstractRedisClientTest {

    private RedisReactiveCommands<String, String> reactive;
    private TestSubscriber<String> subscriber = TestSubscriber.create();

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private StatefulRedisConnection<String, String> stateful;

    @Before
    public void openReactiveConnection() {
        stateful = client.connect();
        reactive = stateful.reactive();
    }

    @After
    public void closeReactiveConnection() {
        reactive.close();
    }

    @Test
    public void keyListCommandShouldReturnAllElements() {

        redis.mset(KeysAndValues.MAP);

        reactive.keys("*").subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).containsAll(KeysAndValues.KEYS);
    }

    @Test
    public void valueListCommandShouldReturnAllElements() {

        redis.mset(KeysAndValues.MAP);

        reactive.mget(KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT])).subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).containsAll(KeysAndValues.VALUES);
    }

    @Test
    public void stringListCommandShouldReturnAllElements() {

        reactive.configGet("*").subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents().size()).isGreaterThan(120);
    }

    @Test
    public void booleanListCommandShouldReturnAllElements() {

        TestSubscriber<Boolean> subscriber = TestSubscriber.create();

        reactive.scriptExists("a", "b", "c").subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).hasSize(3).doesNotContainNull();
    }

    @Test
    public void scoredValueListCommandShouldReturnAllElements() {

        TestSubscriber<ScoredValue<String>> subscriber = TestSubscriber.create();

        redis.zadd(key, 1d, "v1", 2d, "v2", 3d, "v3");

        reactive.zrangeWithScores(key, 0, -1).subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).hasSize(3).contains(sv(1, "v1"), sv(2, "v2"), sv(3, "v3"));
    }

    @Test
    public void geoWithinListCommandShouldReturnAllElements() {

        assumeTrue(RedisConditions.of(redis).hasCommand("GEORADIUS"));

        TestSubscriber<GeoWithin<String>> subscriber = TestSubscriber.create();

        redis.geoadd(key, 50, 20, "value1");
        redis.geoadd(key, 50, 21, "value2");

        reactive.georadius(key, 50, 20, 1000, Unit.km, new GeoArgs().withHash()).subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).hasSize(2).contains(
new GeoWithin<>("value1", null, 3542523898362974L, null),
                new GeoWithin<>("value2", null, 3542609801095198L, null));
    }

    @Test
    public void geoCoordinatesListCommandShouldReturnAllElements() {

        assumeTrue(RedisConditions.of(redis).hasCommand("GEOPOS"));

        TestSubscriber<GeoCoordinates> subscriber = TestSubscriber.create();

        redis.geoadd(key, 50, 20, "value1");
        redis.geoadd(key, 50, 21, "value2");

        reactive.geopos(key, "value1", "value2").subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        assertThat(subscriber.getOnNextEvents()).hasSize(2).doesNotContainNull();
    }
}
