/*
 * Copyright 2016-2018 the original author or authors.
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

    @Inject
    ReactiveTypeAdaptionIntegrationTests(StatefulRedisConnection<String, String> connection) {

        this.redis = connection.sync();

        RedisCommandFactory factory = new RedisCommandFactory(redis.getStatefulConnection());
        this.rxjava1 = factory.getCommands(RxJava1Types.class);
        this.rxjava2 = factory.getCommands(RxJava2Types.class);
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
}
