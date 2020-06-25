/*
 * Copyright 2017-2020 the original author or authors.
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
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanStreamIntegrationTests extends TestSupport {

    private final StatefulRedisConnection<String, String> connection;

    private final RedisCommands<String, String> redis;

    @Inject
    ScanStreamIntegrationTests(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
        this.redis = connection.sync();
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void shouldScanIteratively() {

        for (int i = 0; i < 1000; i++) {
            redis.set("key-" + i, value);
        }
        ScanIterator<String> scan = ScanIterator.scan(redis);
        List<String> list = Flux.fromIterable(() -> scan).collectList().block();

        RedisReactiveCommands<String, String> reactive = redis.getStatefulConnection().reactive();

        StepVerifier.create(ScanStream.scan(reactive, ScanArgs.Builder.limit(200)).take(250)).expectNextCount(250)
                .verifyComplete();
        StepVerifier.create(ScanStream.scan(reactive)).expectNextSequence(list).verifyComplete();
    }

    @Test
    void shouldHscanIteratively() {

        for (int i = 0; i < 1000; i++) {
            redis.hset(key, "field-" + i, "value-" + i);
        }

        RedisReactiveCommands<String, String> reactive = redis.getStatefulConnection().reactive();

        StepVerifier.create(ScanStream.hscan(reactive, key, ScanArgs.Builder.limit(200)).take(250)).expectNextCount(250)
                .verifyComplete();
        StepVerifier.create(ScanStream.hscan(reactive, key)).expectNextCount(1000).verifyComplete();
    }

    @Test
    void shouldSscanIteratively() {

        for (int i = 0; i < 1000; i++) {
            redis.sadd(key, "value-" + i);
        }

        RedisReactiveCommands<String, String> reactive = redis.getStatefulConnection().reactive();

        StepVerifier.create(ScanStream.sscan(reactive, key, ScanArgs.Builder.limit(200)), 0).thenRequest(250)
                .expectNextCount(250).thenCancel().verify();
        StepVerifier.create(ScanStream.sscan(reactive, key).count()).expectNext(1000L).verifyComplete();
    }

    @Test
    void shouldZscanIteratively() {

        for (int i = 0; i < 1000; i++) {
            redis.zadd(key, (double) i, "value-" + i);
        }

        RedisReactiveCommands<String, String> reactive = redis.getStatefulConnection().reactive();

        StepVerifier.create(ScanStream.zscan(reactive, key, ScanArgs.Builder.limit(200)).take(250)).expectNextCount(250)
                .verifyComplete();
        StepVerifier.create(ScanStream.zscan(reactive, key)).expectNextCount(1000).verifyComplete();
    }

    @Test
    void shouldCorrectlyEmitItemsWithConcurrentPoll() {

        RedisReactiveCommands<String, String> commands = connection.reactive();

        String sourceKey = "source";
        String targetKey = "target";

        IntStream.range(0, 10_000).forEach(num -> connection.async().hset(sourceKey, String.valueOf(num), String.valueOf(num)));

        redis.del(targetKey);

        ScanStream.hscan(commands, sourceKey).map(KeyValue::getValue) //
                .map(Integer::parseInt) //
                .filter(num -> num % 2 == 0) //
                .concatMap(item -> commands.sadd(targetKey, String.valueOf(item))) //
                .as(StepVerifier::create) //
                .expectNextCount(5000) //
                .verifyComplete();

        assertThat(redis.scard(targetKey)).isEqualTo(5_000);
    }

}
