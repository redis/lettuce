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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Single;
import io.reactivex.Maybe;

/**
 * @author Mark Paluch
 */
class ReactiveTypeAdaptersUnitTests {

    private ConversionService conversionService = new ConversionService();

    @BeforeEach
    void before() {
        ReactiveTypeAdapters.registerIn(conversionService);
    }

    @Test
    void toWrapperShouldCastMonoToMono() {

        Mono<String> foo = Mono.just("foo");
        assertThat(conversionService.convert(foo, Mono.class)).isSameAs(foo);
    }

    @Test
    void toWrapperShouldConvertMonoToRxJava1Single() {

        Mono<String> foo = Mono.just("foo");
        assertThat(conversionService.convert(foo, Single.class)).isInstanceOf(Single.class);
    }

    @Test
    void toWrapperShouldConvertMonoToRxJava2Single() {

        Mono<String> foo = Mono.just("foo");
        assertThat(conversionService.convert(foo, io.reactivex.Single.class)).isInstanceOf(io.reactivex.Single.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2SingleToMono() {

        io.reactivex.Single<String> foo = io.reactivex.Single.just("foo");
        assertThat(conversionService.convert(foo, Mono.class)).isInstanceOf(Mono.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2SingleToPublisher() {

        io.reactivex.Single<String> foo = io.reactivex.Single.just("foo");
        assertThat(conversionService.convert(foo, Publisher.class)).isInstanceOf(Publisher.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2MaybeToMono() {

        io.reactivex.Maybe<String> foo = io.reactivex.Maybe.just("foo");
        assertThat(conversionService.convert(foo, Mono.class)).isInstanceOf(Mono.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2MaybeToFlux() {

        io.reactivex.Maybe<String> foo = io.reactivex.Maybe.just("foo");
        assertThat(conversionService.convert(foo, Flux.class)).isInstanceOf(Flux.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2MaybeToPublisher() {

        io.reactivex.Maybe<String> foo = io.reactivex.Maybe.just("foo");
        assertThat(conversionService.convert(foo, Publisher.class)).isInstanceOf(Publisher.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2FlowableToMono() {

        io.reactivex.Flowable<String> foo = io.reactivex.Flowable.just("foo");
        assertThat(conversionService.convert(foo, Mono.class)).isInstanceOf(Mono.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2FlowableToFlux() {

        io.reactivex.Flowable<String> foo = io.reactivex.Flowable.just("foo");
        assertThat(conversionService.convert(foo, Flux.class)).isInstanceOf(Flux.class);
    }

    @Test
    void toWrapperShouldCastRxJava2FlowableToPublisher() {

        io.reactivex.Flowable<String> foo = io.reactivex.Flowable.just("foo");
        assertThat(conversionService.convert(foo, Publisher.class)).isInstanceOf(Publisher.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2ObservableToMono() {

        io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
        assertThat(conversionService.convert(foo, Mono.class)).isInstanceOf(Mono.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2ObservableToFlux() {

        io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
        assertThat(conversionService.convert(foo, Flux.class)).isInstanceOf(Flux.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2ObservableToSingle() {

        io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
        assertThat(conversionService.convert(foo, io.reactivex.Single.class)).isInstanceOf(io.reactivex.Single.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2ObservableToMaybe() {

        io.reactivex.Observable<String> foo = io.reactivex.Observable.empty();
        assertThat(conversionService.convert(foo, Maybe.class)).isInstanceOf(Maybe.class);
    }

    @Test
    void toWrapperShouldConvertRxJava2ObservableToPublisher() {

        io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
        assertThat(conversionService.convert(foo, Publisher.class)).isInstanceOf(Publisher.class);
    }

    @Test
    void toWrapperShouldConvertMonoToFlux() {

        Mono<String> foo = Mono.just("foo");
        assertThat(conversionService.convert(foo, Flux.class)).isInstanceOf(Flux.class);
    }
}
