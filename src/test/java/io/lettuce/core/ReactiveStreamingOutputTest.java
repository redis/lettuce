/*
 * Copyright 2011-2017 the original author or authors.
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
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import reactor.test.StepVerifier;

import io.lettuce.KeysAndValues;
import io.lettuce.core.GeoArgs.Unit;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;

/**
 * @author Mark Paluch
 */
public class ReactiveStreamingOutputTest extends AbstractRedisClientTest {

    private RedisReactiveCommands<String, String> reactive;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private StatefulRedisConnection<String, String> stateful;

    @Before
    public void openReactiveConnection() throws Exception {
        stateful = client.connect();
        reactive = stateful.reactive();
    }

    @After
    public void closeReactiveConnection() throws Exception {
        reactive.getStatefulConnection().close();
    }

    @Test
    public void keyListCommandShouldReturnAllElements() throws Exception {

        redis.mset(KeysAndValues.MAP);

        StepVerifier.create(reactive.keys("*")).recordWith(ArrayList::new).expectNextCount(KeysAndValues.COUNT)
                .expectRecordedMatches(strings -> strings.containsAll(KeysAndValues.KEYS)).verifyComplete();
    }

    @Test
    public void valueListCommandShouldReturnAllElements() throws Exception {

        redis.mset(KeysAndValues.MAP);

        StepVerifier.create(reactive.mget(KeysAndValues.KEYS.toArray(new String[KeysAndValues.COUNT])))
                .expectNextCount(KeysAndValues.COUNT).verifyComplete();
    }

    @Test
    public void stringListCommandShouldReturnAllElements() throws Exception {
        StepVerifier.create(reactive.configGet("*")).expectNextCount(120).thenCancel().verify();
    }

    @Test
    public void booleanListCommandShouldReturnAllElements() throws Exception {
        StepVerifier.create(reactive.scriptExists("a", "b", "c")).expectNextCount(3).verifyComplete();
    }

    @Test
    public void scoredValueListCommandShouldReturnAllElements() throws Exception {

        redis.zadd(key, 1d, "v1", 2d, "v2", 3d, "v3");

        StepVerifier.create(reactive.zrangeWithScores(key, 0, -1)).recordWith(ArrayList::new).expectNextCount(3)
                .expectRecordedMatches(values -> values.containsAll(Arrays.asList(sv(1, "v1"), sv(2, "v2"), sv(3, "v3"))))
                .verifyComplete();
    }

    @Test
    public void geoWithinListCommandShouldReturnAllElements() throws Exception {

        redis.geoadd(key, 50, 20, "value1");
        redis.geoadd(key, 50, 21, "value2");

        StepVerifier
                .create(reactive.georadius(key, 50, 20, 1000, Unit.km, new GeoArgs().withHash()))
                .recordWith(ArrayList::new)
                .expectNextCount(2)
                .consumeRecordedWith(
                        values -> {
                            assertThat(values).hasSize(2).contains(new GeoWithin<>("value1", null, 3542523898362974L, null),
                                    new GeoWithin<>("value2", null, 3542609801095198L, null));

                        }).verifyComplete();
    }

    @Test
    public void geoCoordinatesListCommandShouldReturnAllElements() throws Exception {

        redis.geoadd(key, 50, 20, "value1");
        redis.geoadd(key, 50, 21, "value2");

        StepVerifier.create(reactive.geopos(key, "value1", "value2")).expectNextCount(2).verifyComplete();
    }
}
