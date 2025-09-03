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
package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.ClientOptions.DisconnectedBehavior.*;
import static io.lettuce.core.ScriptOutputType.INTEGER;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.WithPassword;
import io.lettuce.test.condition.EnabledOnCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author Mark Paluch
 * @author Nikolai Perevozchikov
 * @author Tugdual Grall
 * @author Hari Mani
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReactiveConnectionIntegrationTests extends TestSupport {

    private final StatefulRedisConnection<String, String> connection;

    private final RedisCommands<String, String> redis;

    private final RedisReactiveCommands<String, String> reactive;

    @Inject
    ReactiveConnectionIntegrationTests(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
        this.redis = connection.sync();
        this.reactive = connection.reactive();
    }

    @BeforeEach
    void setUp() {
        this.connection.async().flushall();
    }

    @Test
    void doNotFireCommandUntilObservation() {

        RedisReactiveCommands<String, String> reactive = connection.reactive();
        Mono<String> set = reactive.set(key, value);
        Delay.delay(Duration.ofMillis(50));
        assertThat(redis.get(key)).isNull();
        set.subscribe();
        Wait.untilEquals(value, () -> redis.get(key)).waitOrTimeout();

        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    void fireCommandAfterObserve() {
        StepVerifier.create(reactive.set(key, value)).expectNext("OK").verifyComplete();
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    void isOpen() {
        assertThat(connection.isOpen()).isTrue();
    }

    @Test
    void testEcho() {
        StepVerifier.create(reactive.echo("echo")).expectNext("echo").verifyComplete();
    }

    @Test
    void multiSubscribe() throws Exception {

        CountDownLatch latch = new CountDownLatch(4);
        reactive.set(key, "1").subscribe(s -> latch.countDown());
        Mono<Long> incr = reactive.incr(key);
        incr.subscribe(s -> latch.countDown());
        incr.subscribe(s -> latch.countDown());
        incr.subscribe(s -> latch.countDown());

        latch.await();

        Wait.untilEquals("4", () -> redis.get(key)).waitOrTimeout();

        assertThat(redis.get(key)).isEqualTo("4");
    }

    @Test
    @Inject
    void transactional(RedisClient client) throws Exception {

        final CountDownLatch sync = new CountDownLatch(1);
        try (StatefulRedisConnection<String, String> statefulRedisConnection = client.connect()) {
            RedisReactiveCommands<String, String> reactive = statefulRedisConnection.reactive();

            reactive.multi().subscribe(multiResponse -> {
                reactive.set(key, "1").subscribe();
                reactive.incr(key).subscribe(getResponse -> sync.countDown());
                reactive.exec().subscribe();
            });

            sync.await(5, TimeUnit.SECONDS);

            String result = redis.get(key);
            assertThat(result).isEqualTo("2");
        }
    }

    @Test
    void auth() {
        WithPassword.enableAuthentication(this.connection.sync());

        try {
            StepVerifier.create(reactive.auth("error")).expectError().verify();
        } finally {
            WithPassword.disableAuthentication(this.connection.sync());
        }
    }

    @Test
    @EnabledOnCommand("ACL")
    void authWithUsername() {

        try {

            StepVerifier.create(reactive.auth(username, "error")).expectNext("OK").verifyComplete();

            WithPassword.enableAuthentication(this.connection.sync());

            StepVerifier.create(reactive.auth(username, "error")).expectError().verify();
            StepVerifier.create(reactive.auth(aclUsername, aclPasswd)).expectNext("OK").verifyComplete();
            StepVerifier.create(reactive.auth(aclUsername, "error")).expectError().verify();
        } finally {
            WithPassword.disableAuthentication(this.connection.sync());
        }
    }

    @Test
    void subscriberCompletingWithExceptionShouldBeHandledSafely() {

        StepVerifier.create(Flux.concat(reactive.set("keyA", "valueA"), reactive.set("keyB", "valueB"))).expectNextCount(2)
                .verifyComplete();

        reactive.get("keyA").subscribe(createSubscriberWithExceptionOnComplete());
        reactive.get("keyA").subscribe(createSubscriberWithExceptionOnComplete());

        StepVerifier.create(reactive.get("keyB")).expectNext("valueB").verifyComplete();
    }

    @Test
    @Inject
    void subscribeWithDisconnectedClient(RedisClient client) {

        client.setOptions(ClientOptions.builder().disconnectedBehavior(REJECT_COMMANDS).autoReconnect(false).build());

        StatefulRedisConnection<String, String> connection = client.connect();

        connection.async().quit();
        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

        StepVerifier
                .create(connection.reactive().ping()).consumeErrorWith(throwable -> assertThat(throwable)
                        .isInstanceOf(RedisException.class).hasMessageContaining("not connected. Commands are rejected"))
                .verify();

        connection.close();
    }

    @Test
    @Inject
    void publishOnSchedulerTest(RedisClient client) {

        client.setOptions(ClientOptions.builder().publishOnScheduler(true).build());
        try (StatefulRedisConnection<String, String> statefulRedisConnection = client.connect()) {
            RedisReactiveCommands<String, String> reactive = statefulRedisConnection.reactive();

            int counter = 0;
            for (int i = 0; i < 1000; i++) {
                if (reactive.eval("return 1", INTEGER).next().block() == null) {
                    counter++;
                }
            }

            assertThat(counter).isZero();
        }
    }

    private static Subscriber<String> createSubscriberWithExceptionOnComplete() {
        return new Subscriber<String>() {

            @Override
            public void onSubscribe(Subscription s) {
                s.request(1000);
            }

            @Override
            public void onComplete() {
                throw new RuntimeException("throwing something");
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(String s) {
            }

        };
    }

    private static class CompletionSubscriber implements Subscriber<Object> {

        private final List<Object> result;

        CompletionSubscriber(List<Object> result) {
            this.result = result;
        }

        @Override
        public void onSubscribe(Subscription s) {
            s.request(1000);
        }

        @Override
        public void onComplete() {
            result.add("completed");
        }

        @Override
        public void onError(Throwable e) {
            result.add(e);
        }

        @Override
        public void onNext(Object o) {
            result.add(o);
        }

    }

}
