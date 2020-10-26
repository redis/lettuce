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
import static org.assertj.core.api.Assertions.fail;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.reactivex.Observable;

/**
 * @author Mark Paluch
 */
class ConversionServiceUnitTests {

    private ConversionService sut = new ConversionService();

    @Test
    void getConverter() {

        sut.addConverter(new FluxToObservableConverter());
        sut.addConverter(new MonoToObservableConverter());

        assertThat(sut.getConverter(Flux.just("").getClass(), Observable.class)).isNotNull()
                .isInstanceOf(FluxToObservableConverter.class);

        try {
            sut.getConverter(Flux.just("").getClass(), String.class);
            fail("Missing IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("No converter found for reactor.core.publisher.FluxJust to java.lang.String");
        }
    }

    @Test
    void canConvert() {

        sut.addConverter(new FluxToObservableConverter());
        sut.addConverter(new MonoToObservableConverter());

        assertThat(sut.canConvert(Flux.class, Observable.class)).isTrue();
        assertThat(sut.canConvert(Observable.class, Flux.class)).isFalse();
    }

    @Test
    void convert() {

        sut.addConverter(new FluxToObservableConverter());
        sut.addConverter(new MonoToObservableConverter());

        Observable<String> observable = sut.convert(Mono.just("hello"), Observable.class);
        observable.test().assertValue("world").assertComplete();
    }

    private class FluxToObservableConverter implements Function<Flux<?>, Observable<?>> {

        @Override
        public Observable<?> apply(Flux<?> source) {
            return null;
        }

    }

    private class MonoToObservableConverter implements Function<Mono<?>, Observable<?>> {

        @Override
        public Observable<?> apply(Mono<?> source) {
            return Observable.just("world");
        }

    }

}
