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

import static com.google.code.tempusfugit.temporal.Duration.millis;
import static io.lettuce.core.ClientOptions.DisconnectedBehavior.REJECT_COMMANDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import io.lettuce.Delay;
import io.lettuce.Wait;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;

public class ReactiveConnectionTest extends AbstractRedisClientTest {

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
    public void doNotFireCommandUntilObservation() throws Exception {
        Mono<String> set = reactive.set(key, value);
        Delay.delay(millis(200));
        assertThat(redis.get(key)).isNull();
        set.subscribe();
        Wait.untilEquals(value, () -> redis.get(key)).waitOrTimeout();

        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void fireCommandAfterObserve() throws Exception {
        StepVerifier.create(reactive.set(key, value)).expectNext("OK").verifyComplete();
        assertThat(redis.get(key)).isEqualTo(value);
    }

    @Test
    public void isOpen() throws Exception {
        assertThat(reactive.isOpen()).isTrue();
    }

    @Test
    public void getStatefulConnection() throws Exception {
        assertThat(reactive.getStatefulConnection()).isSameAs(stateful);
    }

    @Test
    public void testCancelCommand() throws Exception {

        List<Object> result = new ArrayList<>();
        reactive.clientPause(2000).subscribe();
        Delay.delay(millis(100));

        reactive.set(key, value).subscribe(new CompletionSubscriber(result));
        Delay.delay(millis(100));

        reactive.reset();
        assertThat(result).isEmpty();
    }

    @Test
    public void testEcho() throws Exception {
        StepVerifier.create(reactive.echo("echo")).expectNext("echo").verifyComplete();
    }

    @Test
    public void testMonoMultiCancel() throws Exception {

        List<Object> result = new ArrayList<>();
        reactive.clientPause(1000).subscribe();
        Delay.delay(millis(100));

        Mono<String> set = reactive.set(key, value);
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        Delay.delay(millis(100));

        reactive.reset();
        assertThat(result).isEmpty();
    }

    @Test
    public void testFluxCancel() throws Exception {

        List<Object> result = new ArrayList<>();
        reactive.clientPause(1000).subscribe();
        Delay.delay(millis(100));

        Flux<KeyValue<String, String>> set = reactive.mget(key, value);
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        set.subscribe(new CompletionSubscriber(result));
        Delay.delay(millis(100));

        reactive.reset();
        assertThat(result).isEmpty();
    }

    @Test
    public void multiSubscribe() throws Exception {
        reactive.set(key, "1").subscribe();
        Mono<Long> incr = reactive.incr(key);
        incr.subscribe();
        incr.subscribe();
        incr.subscribe();

        Wait.untilEquals("4", () -> redis.get(key)).waitOrTimeout();

        assertThat(redis.get(key)).isEqualTo("4");
    }

    @Test
    public void transactional() throws Exception {

        final CountDownLatch sync = new CountDownLatch(1);

        RedisReactiveCommands<String, String> reactive = client.connect().reactive();

        reactive.multi().subscribe(multiResponse -> {
            reactive.set(key, "1").subscribe();
            reactive.incr(key).subscribe(getResponse -> {
                sync.countDown();
            });
            reactive.exec().subscribe();
        });

        sync.await(5, TimeUnit.SECONDS);

        String result = redis.get(key);
        assertThat(result).isEqualTo("2");
    }

    @Test
    public void auth() throws Exception {
        StepVerifier.create(reactive.auth("error")).expectError().verify();
    }

    @Test
    public void subscriberCompletingWithExceptionShouldBeHandledSafely() throws Exception {

        StepVerifier.create(Flux.concat(reactive.set("keyA", "valueA"), reactive.set("keyB", "valueB"))).expectNextCount(2)
                .verifyComplete();

        reactive.get("keyA").subscribe(createSubscriberWithExceptionOnComplete());
        reactive.get("keyA").subscribe(createSubscriberWithExceptionOnComplete());

        StepVerifier.create(reactive.get("keyB")).expectNext("valueB").verifyComplete();
    }

    @Test
    public void subscribeWithDisconnectedClient() throws Exception {

        client.setOptions(ClientOptions.builder().disconnectedBehavior(REJECT_COMMANDS).autoReconnect(false).build());

        StatefulRedisConnection<String, String> connection = client.connect();

        connection.async().quit();
        Wait.untilTrue(() -> !connection.isOpen()).waitOrTimeout();

        StepVerifier
                .create(connection.reactive().ping())
                .consumeErrorWith(
                        throwable -> {
                            assertThat(throwable).isInstanceOf(RedisException.class).hasMessageContaining(
                                    "not connected. Commands are rejected");

                        }).verify();

        connection.close();
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

        public CompletionSubscriber(List<Object> result) {
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
