/*
 * Copyright 2016 the original author or authors.
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

import io.reactivex.Flowable;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Maybe;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Single;

/**
 * @author Mark Paluch
 */
public class ReactiveTypeAdaptersTest {

    ConversionService conversionService = new ConversionService();

    @Before
    public void before() throws Exception {
        ReactiveTypeAdapters.registerIn(conversionService);
    }

    @Test
    public void toWrapperShouldCastMonoToMono() {

        Mono<String> foo = Mono.just("foo");
        assertThat(conversionService.convert(foo, Mono.class)).isSameAs(foo);
    }

    @Test
    public void toWrapperShouldConvertMonoToRxJava1Single() {

        Mono<String> foo = Mono.just("foo");
        assertThat(conversionService.convert(foo, Single.class)).isInstanceOf(Single.class);
    }

    @Test
    public void toWrapperShouldConvertMonoToRxJava2Single() {

        Mono<String> foo = Mono.just("foo");
        assertThat(conversionService.convert(foo, io.reactivex.Single.class)).isInstanceOf(io.reactivex.Single.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2SingleToMono() {

        io.reactivex.Single<String> foo = io.reactivex.Single.just("foo");
        assertThat(conversionService.convert(foo, Mono.class)).isInstanceOf(Mono.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2SingleToPublisher() {

        io.reactivex.Single<String> foo = io.reactivex.Single.just("foo");
        assertThat(conversionService.convert(foo, Publisher.class)).isInstanceOf(Publisher.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2MaybeToMono() {

        io.reactivex.Maybe<String> foo = io.reactivex.Maybe.just("foo");
        assertThat(conversionService.convert(foo, Mono.class)).isInstanceOf(Mono.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2MaybeToFlux() {

        io.reactivex.Maybe<String> foo = io.reactivex.Maybe.just("foo");
        assertThat(conversionService.convert(foo, Flux.class)).isInstanceOf(Flux.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2MaybeToPublisher() {

        io.reactivex.Maybe<String> foo = io.reactivex.Maybe.just("foo");
        assertThat(conversionService.convert(foo, Publisher.class)).isInstanceOf(Publisher.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2FlowableToMono() {

        io.reactivex.Flowable<String> foo = io.reactivex.Flowable.just("foo");
        assertThat(conversionService.convert(foo, Mono.class)).isInstanceOf(Mono.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2FlowableToFlux() {

        io.reactivex.Flowable<String> foo = io.reactivex.Flowable.just("foo");
        assertThat(conversionService.convert(foo, Flux.class)).isInstanceOf(Flux.class);
    }

    @Test
    public void toWrapperShouldCastRxJava2FlowableToPublisher() {

        io.reactivex.Flowable<String> foo = io.reactivex.Flowable.just("foo");
        assertThat(conversionService.convert(foo, Publisher.class)).isInstanceOf(Publisher.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2ObservableToMono() {

        io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
        assertThat(conversionService.convert(foo, Mono.class)).isInstanceOf(Mono.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2ObservableToFlux() {

        io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
        assertThat(conversionService.convert(foo, Flux.class)).isInstanceOf(Flux.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2ObservableToSingle() {

        io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
        assertThat(conversionService.convert(foo, io.reactivex.Single.class)).isInstanceOf(io.reactivex.Single.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2ObservableToMaybe() {

        io.reactivex.Observable<String> foo = io.reactivex.Observable.empty();
        assertThat(conversionService.convert(foo, Maybe.class)).isInstanceOf(Maybe.class);
    }

    @Test
    public void toWrapperShouldConvertRxJava2ObservableToPublisher() {

        io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
        assertThat(conversionService.convert(foo, Publisher.class)).isInstanceOf(Publisher.class);
    }

    @Test
    public void toWrapperShouldConvertMonoToFlux() {

        Mono<String> foo = Mono.just("foo");
        assertThat(conversionService.convert(foo, Flux.class)).isInstanceOf(Flux.class);
    }
}
