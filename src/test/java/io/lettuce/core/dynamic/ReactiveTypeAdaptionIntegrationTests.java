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
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import rx.Observable;
import rx.Single;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class ReactiveTypeAdaptionIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    private final RxJava1Types rxjava1;
    private final RxJava2Types rxjava2;
    private final RxJava3Types rxjava3;

    @Inject
    ReactiveTypeAdaptionIntegrationTests(StatefulRedisConnection<String, String> connection) {

        this.redis = connection.sync();

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());
        this.rxjava1 = factory.getCommands(RxJava1Types.class);
        this.rxjava2 = factory.getCommands(RxJava2Types.class);
        this.rxjava3 = factory.getCommands(RxJava3Types.class);
    }

    @BeforeEach
    void setUp() {
        redis.set(key, value);
    }

    @Test
    void rxJava1Single() {

        Single<String> single = rxjava1.getRxJava1Single(key);
        assertThat(single.toBlocking().value()).isEqualTo(value);
    }

    @Test
    void rxJava1Observable() {

        Observable<String> observable = rxjava1.getRxJava1Observable(key);
        assertThat(observable.toBlocking().last()).isEqualTo(value);
    }

    @Test
    void rxJava2Single() throws InterruptedException {

        io.reactivex.Single<String> single = rxjava2.getRxJava2Single(key);
        single.test().await().assertResult(value).assertComplete();
    }

    @Test
    void rxJava2Maybe() throws InterruptedException {

        io.reactivex.Maybe<String> maybe = rxjava2.getRxJava2Maybe(key);
        maybe.test().await().assertResult(value).assertComplete();
    }

    @Test
    void rxJava2Observable() throws InterruptedException {

        io.reactivex.Observable<String> observable = rxjava2.getRxJava2Observable(key);
        observable.test().await().assertResult(value).assertComplete();
    }

    @Test
    void rxJava2Flowable() throws InterruptedException {

        io.reactivex.Flowable<String> flowable = rxjava2.getRxJava2Flowable(key);
        flowable.test().await().assertResult(value).assertComplete();
    }

    @Test
    void rxJava3Single() throws InterruptedException {

        io.reactivex.rxjava3.core.Single<String> single = rxjava3.getRxJava3Single(key);
        single.test().await().assertResult(value).assertComplete();
    }

    @Test
    void rxJava3Maybe() throws InterruptedException {

        io.reactivex.rxjava3.core.Maybe<String> maybe = rxjava3.getRxJava3Maybe(key);
        maybe.test().await().assertResult(value).assertComplete();
    }

    @Test
    void rxJava3Observable() throws InterruptedException {

        io.reactivex.rxjava3.core.Observable<String> observable = rxjava3.getRxJava3Observable(key);
        observable.test().await().assertResult(value).assertComplete();
    }

    @Test
    void rxJava3Flowable() throws InterruptedException {

        io.reactivex.rxjava3.core.Flowable<String> flowable = rxjava3.getRxJava3Flowable(key);
        flowable.test().await().assertResult(value).assertComplete();
    }

    static interface RxJava1Types extends Commands {

        @Command("GET")
        Single<String> getRxJava1Single(String key);

        @Command("GET")
        Observable<String> getRxJava1Observable(String key);
    }

    static interface RxJava2Types extends Commands {

        @Command("GET")
        io.reactivex.Single<String> getRxJava2Single(String key);

        @Command("GET")
        io.reactivex.Maybe<String> getRxJava2Maybe(String key);

        @Command("GET")
        io.reactivex.Observable<String> getRxJava2Observable(String key);

        @Command("GET")
        io.reactivex.Flowable<String> getRxJava2Flowable(String key);
    }

    static interface RxJava3Types extends Commands {

        @Command("GET")
        io.reactivex.rxjava3.core.Single<String> getRxJava3Single(String key);

        @Command("GET")
        io.reactivex.rxjava3.core.Maybe<String> getRxJava3Maybe(String key);

        @Command("GET")
        io.reactivex.rxjava3.core.Observable<String> getRxJava3Observable(String key);

        @Command("GET")
        io.reactivex.rxjava3.core.Flowable<String> getRxJava3Flowable(String key);
    }
}
