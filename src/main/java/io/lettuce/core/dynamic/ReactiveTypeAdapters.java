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

import java.util.function.Function;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;
import rx.internal.reactivestreams.PublisherAdapter;
import io.lettuce.core.dynamic.ReactiveTypes.ReactiveLibrary;
import io.lettuce.core.internal.LettuceAssert;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

/**
 * @author Mark Paluch
 * @since 5.0
 */
class ReactiveTypeAdapters {

    /**
     * Register adapters in the conversion service.
     *
     * @param conversionService
     */
    static void registerIn(ConversionService conversionService) {

        LettuceAssert.notNull(conversionService, "ConversionService must not be null!");

        if (ReactiveTypes.isAvailable(ReactiveLibrary.PROJECT_REACTOR)) {

            if (ReactiveTypes.isAvailable(ReactiveLibrary.RXJAVA1)) {

                conversionService.addConverter(PublisherToRxJava1CompletableAdapter.INSTANCE);
                conversionService.addConverter(RxJava1CompletableToPublisherAdapter.INSTANCE);
                conversionService.addConverter(RxJava1CompletableToMonoAdapter.INSTANCE);

                conversionService.addConverter(PublisherToRxJava1SingleAdapter.INSTANCE);
                conversionService.addConverter(RxJava1SingleToPublisherAdapter.INSTANCE);
                conversionService.addConverter(RxJava1SingleToMonoAdapter.INSTANCE);
                conversionService.addConverter(RxJava1SingleToFluxAdapter.INSTANCE);

                conversionService.addConverter(PublisherToRxJava1ObservableAdapter.INSTANCE);
                conversionService.addConverter(RxJava1ObservableToPublisherAdapter.INSTANCE);
                conversionService.addConverter(RxJava1ObservableToMonoAdapter.INSTANCE);
                conversionService.addConverter(RxJava1ObservableToFluxAdapter.INSTANCE);
            }

            if (ReactiveTypes.isAvailable(ReactiveLibrary.RXJAVA2)) {

                conversionService.addConverter(PublisherToRxJava2CompletableAdapter.INSTANCE);
                conversionService.addConverter(RxJava2CompletableToPublisherAdapter.INSTANCE);
                conversionService.addConverter(RxJava2CompletableToMonoAdapter.INSTANCE);

                conversionService.addConverter(PublisherToRxJava2SingleAdapter.INSTANCE);
                conversionService.addConverter(RxJava2SingleToPublisherAdapter.INSTANCE);
                conversionService.addConverter(RxJava2SingleToMonoAdapter.INSTANCE);
                conversionService.addConverter(RxJava2SingleToFluxAdapter.INSTANCE);

                conversionService.addConverter(PublisherToRxJava2ObservableAdapter.INSTANCE);
                conversionService.addConverter(RxJava2ObservableToPublisherAdapter.INSTANCE);
                conversionService.addConverter(RxJava2ObservableToMonoAdapter.INSTANCE);
                conversionService.addConverter(RxJava2ObservableToFluxAdapter.INSTANCE);

                conversionService.addConverter(PublisherToRxJava2FlowableAdapter.INSTANCE);
                conversionService.addConverter(RxJava2FlowableToPublisherAdapter.INSTANCE);

                conversionService.addConverter(PublisherToRxJava2MaybeAdapter.INSTANCE);
                conversionService.addConverter(RxJava2MaybeToPublisherAdapter.INSTANCE);
                conversionService.addConverter(RxJava2MaybeToMonoAdapter.INSTANCE);
                conversionService.addConverter(RxJava2MaybeToFluxAdapter.INSTANCE);
            }

            if (ReactiveTypes.isAvailable(ReactiveLibrary.RXJAVA3)) {

                conversionService.addConverter(PublisherToRxJava3CompletableAdapter.INSTANCE);
                conversionService.addConverter(RxJava3CompletableToPublisherAdapter.INSTANCE);
                conversionService.addConverter(RxJava3CompletableToMonoAdapter.INSTANCE);

                conversionService.addConverter(PublisherToRxJava3SingleAdapter.INSTANCE);
                conversionService.addConverter(RxJava3SingleToPublisherAdapter.INSTANCE);
                conversionService.addConverter(RxJava3SingleToMonoAdapter.INSTANCE);
                conversionService.addConverter(RxJava3SingleToFluxAdapter.INSTANCE);

                conversionService.addConverter(PublisherToRxJava3ObservableAdapter.INSTANCE);
                conversionService.addConverter(RxJava3ObservableToPublisherAdapter.INSTANCE);
                conversionService.addConverter(RxJava3ObservableToMonoAdapter.INSTANCE);
                conversionService.addConverter(RxJava3ObservableToFluxAdapter.INSTANCE);

                conversionService.addConverter(PublisherToRxJava3FlowableAdapter.INSTANCE);
                conversionService.addConverter(RxJava3FlowableToPublisherAdapter.INSTANCE);

                conversionService.addConverter(PublisherToRxJava3MaybeAdapter.INSTANCE);
                conversionService.addConverter(RxJava3MaybeToPublisherAdapter.INSTANCE);
                conversionService.addConverter(RxJava3MaybeToMonoAdapter.INSTANCE);
                conversionService.addConverter(RxJava3MaybeToFluxAdapter.INSTANCE);
            }

            conversionService.addConverter(PublisherToMonoAdapter.INSTANCE);
            conversionService.addConverter(PublisherToFluxAdapter.INSTANCE);

            if (ReactiveTypes.isAvailable(ReactiveLibrary.RXJAVA1)) {
                conversionService.addConverter(RxJava1SingleToObservableAdapter.INSTANCE);
                conversionService.addConverter(RxJava1ObservableToSingleAdapter.INSTANCE);
            }

            if (ReactiveTypes.isAvailable(ReactiveLibrary.RXJAVA2)) {
                conversionService.addConverter(RxJava2SingleToObservableAdapter.INSTANCE);
                conversionService.addConverter(RxJava2ObservableToSingleAdapter.INSTANCE);
                conversionService.addConverter(RxJava2ObservableToMaybeAdapter.INSTANCE);
            }

            if (ReactiveTypes.isAvailable(ReactiveLibrary.RXJAVA3)) {
                conversionService.addConverter(RxJava3SingleToObservableAdapter.INSTANCE);
                conversionService.addConverter(RxJava3ObservableToSingleAdapter.INSTANCE);
                conversionService.addConverter(RxJava3ObservableToMaybeAdapter.INSTANCE);
            }
        }
    }

    // -------------------------------------------------------------------------
    // ReactiveStreams adapters
    // -------------------------------------------------------------------------

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link Flux}.
     */
    public enum PublisherToFluxAdapter implements Function<Publisher<?>, Flux<?>> {

        INSTANCE;

        @Override
        public Flux<?> apply(Publisher<?> source) {
            return Flux.from(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link Mono}.
     */
    public enum PublisherToMonoAdapter implements Function<Publisher<?>, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(Publisher<?> source) {
            return Mono.from(source);
        }

    }

    // -------------------------------------------------------------------------
    // RxJava 1 adapters
    // -------------------------------------------------------------------------

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link Single}.
     */
    public enum PublisherToRxJava1SingleAdapter implements Function<Publisher<?>, Single<?>> {

        INSTANCE;

        @Override
        public Single<?> apply(Publisher<?> source) {
            return RxReactiveStreams.toSingle(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link Completable}.
     */
    public enum PublisherToRxJava1CompletableAdapter implements Function<Publisher<?>, Completable> {

        INSTANCE;

        @Override
        public Completable apply(Publisher<?> source) {
            return RxReactiveStreams.toCompletable(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link Observable}.
     */
    public enum PublisherToRxJava1ObservableAdapter implements Function<Publisher<?>, Observable<?>> {

        INSTANCE;

        @Override
        public Observable<?> apply(Publisher<?> source) {
            return RxReactiveStreams.toObservable(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Single} to {@link Publisher}.
     */
    public enum RxJava1SingleToPublisherAdapter implements Function<Single<?>, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(Single<?> source) {
            return Flux.defer(() -> RxReactiveStreams.toPublisher(source));
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Single} to {@link Mono}.
     */
    public enum RxJava1SingleToMonoAdapter implements Function<Single<?>, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(Single<?> source) {
            return Mono.defer(() -> Mono.from((Publisher<?>) RxReactiveStreams.toPublisher(source)));
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Single} to {@link Publisher}.
     */
    public enum RxJava1SingleToFluxAdapter implements Function<Single<?>, Flux<?>> {

        INSTANCE;

        @Override
        public Flux<?> apply(Single<?> source) {
            return Flux.defer(() -> RxReactiveStreams.toPublisher(source));
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Completable} to {@link Publisher}.
     */
    public enum RxJava1CompletableToPublisherAdapter implements Function<Completable, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(Completable source) {
            return Flux.defer(() -> RxReactiveStreams.toPublisher(source));
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Completable} to {@link Mono}.
     */
    public enum RxJava1CompletableToMonoAdapter implements Function<Completable, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(Completable source) {
            return Mono.from(RxJava1CompletableToPublisherAdapter.INSTANCE.apply(source));
        }

    }

    /**
     * An adapter {@link Function} to adopt an {@link Observable} to {@link Publisher}.
     */
    public enum RxJava1ObservableToPublisherAdapter implements Function<Observable<?>, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(Observable<?> source) {
            return Flux.defer(() -> new PublisherAdapter<>(source));
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Observable} to {@link Mono}.
     */
    public enum RxJava1ObservableToMonoAdapter implements Function<Observable<?>, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(Observable<?> source) {
            return Mono.defer(() -> Mono.from((Publisher<?>) RxReactiveStreams.toPublisher(source)));
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Observable} to {@link Flux}.
     */
    public enum RxJava1ObservableToFluxAdapter implements Function<Observable<?>, Flux<?>> {

        INSTANCE;

        @Override
        public Flux<?> apply(Observable<?> source) {
            return Flux.defer(() -> Flux.from((Publisher<?>) RxReactiveStreams.toPublisher(source)));
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Observable} to {@link Single}.
     */
    public enum RxJava1ObservableToSingleAdapter implements Function<Observable<?>, Single<?>> {

        INSTANCE;

        @Override
        public Single<?> apply(Observable<?> source) {
            return source.toSingle();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Single} to {@link Single}.
     */
    public enum RxJava1SingleToObservableAdapter implements Function<Single<?>, Observable<?>> {

        INSTANCE;

        @Override
        public Observable<?> apply(Single<?> source) {
            return source.toObservable();
        }

    }

    // -------------------------------------------------------------------------
    // RxJava 2 adapters
    // -------------------------------------------------------------------------

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link io.reactivex.Single}.
     */
    public enum PublisherToRxJava2SingleAdapter implements Function<Publisher<?>, io.reactivex.Single<?>> {

        INSTANCE;

        @Override
        public io.reactivex.Single<?> apply(Publisher<?> source) {
            return io.reactivex.Single.fromPublisher(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link io.reactivex.Completable}.
     */
    public enum PublisherToRxJava2CompletableAdapter implements Function<Publisher<?>, io.reactivex.Completable> {

        INSTANCE;

        @Override
        public io.reactivex.Completable apply(Publisher<?> source) {
            return io.reactivex.Completable.fromPublisher(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link io.reactivex.Observable}.
     */
    public enum PublisherToRxJava2ObservableAdapter implements Function<Publisher<?>, io.reactivex.Observable<?>> {

        INSTANCE;

        @Override
        public io.reactivex.Observable<?> apply(Publisher<?> source) {
            return io.reactivex.Observable.fromPublisher(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.Single} to {@link Publisher}.
     */
    public enum RxJava2SingleToPublisherAdapter implements Function<io.reactivex.Single<?>, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(io.reactivex.Single<?> source) {
            return source.toFlowable();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.Single} to {@link Mono}.
     */
    public enum RxJava2SingleToMonoAdapter implements Function<io.reactivex.Single<?>, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(io.reactivex.Single<?> source) {
            return Mono.from(source.toFlowable());
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.Single} to {@link Publisher}.
     */
    public enum RxJava2SingleToFluxAdapter implements Function<io.reactivex.Single<?>, Flux<?>> {

        INSTANCE;

        @Override
        public Flux<?> apply(io.reactivex.Single<?> source) {
            return Flux.from(source.toFlowable());
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.Completable} to {@link Publisher}.
     */
    public enum RxJava2CompletableToPublisherAdapter implements Function<io.reactivex.Completable, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(io.reactivex.Completable source) {
            return source.toFlowable();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.Completable} to {@link Mono}.
     */
    public enum RxJava2CompletableToMonoAdapter implements Function<io.reactivex.Completable, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(io.reactivex.Completable source) {
            return Mono.from(RxJava2CompletableToPublisherAdapter.INSTANCE.apply(source));
        }

    }

    /**
     * An adapter {@link Function} to adopt an {@link io.reactivex.Observable} to {@link Publisher}.
     */
    public enum RxJava2ObservableToPublisherAdapter implements Function<io.reactivex.Observable<?>, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(io.reactivex.Observable<?> source) {
            return source.toFlowable(BackpressureStrategy.BUFFER);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.Observable} to {@link Mono}.
     */
    public enum RxJava2ObservableToMonoAdapter implements Function<io.reactivex.Observable<?>, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(io.reactivex.Observable<?> source) {
            return Mono.from(source.toFlowable(BackpressureStrategy.BUFFER));
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.Observable} to {@link Flux}.
     */
    public enum RxJava2ObservableToFluxAdapter implements Function<io.reactivex.Observable<?>, Flux<?>> {

        INSTANCE;

        @Override
        public Flux<?> apply(io.reactivex.Observable<?> source) {
            return Flux.from(source.toFlowable(BackpressureStrategy.BUFFER));
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link io.reactivex.Flowable}.
     */
    public enum PublisherToRxJava2FlowableAdapter implements Function<Publisher<?>, io.reactivex.Flowable<?>> {

        INSTANCE;

        @Override
        public io.reactivex.Flowable<?> apply(Publisher<?> source) {
            return Flowable.fromPublisher(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.Flowable} to {@link Publisher}.
     */
    public enum RxJava2FlowableToPublisherAdapter implements Function<io.reactivex.Flowable<?>, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(io.reactivex.Flowable<?> source) {
            return source;
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link io.reactivex.Flowable}.
     */
    public enum PublisherToRxJava2MaybeAdapter implements Function<Publisher<?>, io.reactivex.Maybe<?>> {

        INSTANCE;

        @Override
        public io.reactivex.Maybe<?> apply(Publisher<?> source) {
            return Flowable.fromPublisher(source).singleElement();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.Maybe} to {@link Publisher}.
     */
    public enum RxJava2MaybeToPublisherAdapter implements Function<io.reactivex.Maybe<?>, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(io.reactivex.Maybe<?> source) {
            return source.toFlowable();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.Maybe} to {@link Mono}.
     */
    public enum RxJava2MaybeToMonoAdapter implements Function<io.reactivex.Maybe<?>, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(io.reactivex.Maybe<?> source) {
            return Mono.from(source.toFlowable());
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.Maybe} to {@link Flux}.
     */
    public enum RxJava2MaybeToFluxAdapter implements Function<io.reactivex.Maybe<?>, Flux<?>> {

        INSTANCE;

        @Override
        public Flux<?> apply(io.reactivex.Maybe<?> source) {
            return Flux.from(source.toFlowable());
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Observable} to {@link Single}.
     */
    public enum RxJava2ObservableToSingleAdapter implements Function<io.reactivex.Observable<?>, io.reactivex.Single<?>> {

        INSTANCE;

        @Override
        public io.reactivex.Single<?> apply(io.reactivex.Observable<?> source) {
            return source.singleOrError();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Observable} to {@link Maybe}.
     */
    public enum RxJava2ObservableToMaybeAdapter implements Function<io.reactivex.Observable<?>, io.reactivex.Maybe<?>> {

        INSTANCE;

        @Override
        public io.reactivex.Maybe<?> apply(io.reactivex.Observable<?> source) {
            return source.singleElement();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Single} to {@link Single}.
     */
    public enum RxJava2SingleToObservableAdapter implements Function<io.reactivex.Single<?>, io.reactivex.Observable<?>> {

        INSTANCE;

        @Override
        public io.reactivex.Observable<?> apply(io.reactivex.Single<?> source) {
            return source.toObservable();
        }

    }

    // -------------------------------------------------------------------------
    // RxJava 3 adapters
    // -------------------------------------------------------------------------

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link io.reactivex.Single}.
     */
    public enum PublisherToRxJava3SingleAdapter implements Function<Publisher<?>, io.reactivex.rxjava3.core.Single<?>> {

        INSTANCE;

        @Override
        public io.reactivex.rxjava3.core.Single<?> apply(Publisher<?> source) {
            return io.reactivex.rxjava3.core.Single.fromPublisher(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link io.reactivex.Completable}.
     */
    public enum PublisherToRxJava3CompletableAdapter implements Function<Publisher<?>, io.reactivex.rxjava3.core.Completable> {

        INSTANCE;

        @Override
        public io.reactivex.rxjava3.core.Completable apply(Publisher<?> source) {
            return io.reactivex.rxjava3.core.Completable.fromPublisher(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link io.reactivex.rxjava3.core.Observable}.
     */
    public enum PublisherToRxJava3ObservableAdapter implements Function<Publisher<?>, io.reactivex.rxjava3.core.Observable<?>> {

        INSTANCE;

        @Override
        public io.reactivex.rxjava3.core.Observable<?> apply(Publisher<?> source) {
            return io.reactivex.rxjava3.core.Observable.fromPublisher(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.rxjava3.core.Single} to {@link Publisher}.
     */
    public enum RxJava3SingleToPublisherAdapter implements Function<io.reactivex.rxjava3.core.Single<?>, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(io.reactivex.rxjava3.core.Single<?> source) {
            return source.toFlowable();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.rxjava3.core.Single} to {@link Mono}.
     */
    public enum RxJava3SingleToMonoAdapter implements Function<io.reactivex.rxjava3.core.Single<?>, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(io.reactivex.rxjava3.core.Single<?> source) {
            return Mono.from(source.toFlowable());
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.rxjava3.core.Single} to {@link Publisher}.
     */
    public enum RxJava3SingleToFluxAdapter implements Function<io.reactivex.rxjava3.core.Single<?>, Flux<?>> {

        INSTANCE;

        @Override
        public Flux<?> apply(io.reactivex.rxjava3.core.Single<?> source) {
            return Flux.from(source.toFlowable());
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.rxjava3.core.Completable} to {@link Publisher}.
     */
    public enum RxJava3CompletableToPublisherAdapter implements Function<io.reactivex.rxjava3.core.Completable, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(io.reactivex.rxjava3.core.Completable source) {
            return source.toFlowable();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.rxjava3.core.Completable} to {@link Mono}.
     */
    public enum RxJava3CompletableToMonoAdapter implements Function<io.reactivex.rxjava3.core.Completable, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(io.reactivex.rxjava3.core.Completable source) {
            return Mono.from(RxJava3CompletableToPublisherAdapter.INSTANCE.apply(source));
        }

    }

    /**
     * An adapter {@link Function} to adopt an {@link io.reactivex.rxjava3.core.Observable} to {@link Publisher}.
     */
    public enum RxJava3ObservableToPublisherAdapter implements Function<io.reactivex.rxjava3.core.Observable<?>, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(io.reactivex.rxjava3.core.Observable<?> source) {
            return source.toFlowable(io.reactivex.rxjava3.core.BackpressureStrategy.BUFFER);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.rxjava3.core.Observable} to {@link Mono}.
     */
    public enum RxJava3ObservableToMonoAdapter implements Function<io.reactivex.rxjava3.core.Observable<?>, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(io.reactivex.rxjava3.core.Observable<?> source) {
            return Mono.from(source.toFlowable(io.reactivex.rxjava3.core.BackpressureStrategy.BUFFER));
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.rxjava3.core.Observable} to {@link Flux}.
     */
    public enum RxJava3ObservableToFluxAdapter implements Function<io.reactivex.rxjava3.core.Observable<?>, Flux<?>> {

        INSTANCE;

        @Override
        public Flux<?> apply(io.reactivex.rxjava3.core.Observable<?> source) {
            return Flux.from(source.toFlowable(io.reactivex.rxjava3.core.BackpressureStrategy.BUFFER));
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link io.reactivex.rxjava3.core.Flowable}.
     */
    public enum PublisherToRxJava3FlowableAdapter implements Function<Publisher<?>, io.reactivex.rxjava3.core.Flowable<?>> {

        INSTANCE;

        @Override
        public io.reactivex.rxjava3.core.Flowable<?> apply(Publisher<?> source) {
            return io.reactivex.rxjava3.core.Flowable.fromPublisher(source);
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.rxjava3.core.Flowable} to {@link Publisher}.
     */
    public enum RxJava3FlowableToPublisherAdapter implements Function<io.reactivex.rxjava3.core.Flowable<?>, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(io.reactivex.rxjava3.core.Flowable<?> source) {
            return source;
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Publisher} to {@link io.reactivex.rxjava3.core.Flowable}.
     */
    public enum PublisherToRxJava3MaybeAdapter implements Function<Publisher<?>, io.reactivex.rxjava3.core.Maybe<?>> {

        INSTANCE;

        @Override
        public io.reactivex.rxjava3.core.Maybe<?> apply(Publisher<?> source) {
            return io.reactivex.rxjava3.core.Flowable.fromPublisher(source).singleElement();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.rxjava3.core.Maybe} to {@link Publisher}.
     */
    public enum RxJava3MaybeToPublisherAdapter implements Function<io.reactivex.rxjava3.core.Maybe<?>, Publisher<?>> {

        INSTANCE;

        @Override
        public Publisher<?> apply(io.reactivex.rxjava3.core.Maybe<?> source) {
            return source.toFlowable();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.rxjava3.core.Maybe} to {@link Mono}.
     */
    public enum RxJava3MaybeToMonoAdapter implements Function<io.reactivex.rxjava3.core.Maybe<?>, Mono<?>> {

        INSTANCE;

        @Override
        public Mono<?> apply(io.reactivex.rxjava3.core.Maybe<?> source) {
            return Mono.from(source.toFlowable());
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link io.reactivex.rxjava3.core.Maybe} to {@link Flux}.
     */
    public enum RxJava3MaybeToFluxAdapter implements Function<io.reactivex.rxjava3.core.Maybe<?>, Flux<?>> {

        INSTANCE;

        @Override
        public Flux<?> apply(io.reactivex.rxjava3.core.Maybe<?> source) {
            return Flux.from(source.toFlowable());
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Observable} to {@link Single}.
     */
    public enum RxJava3ObservableToSingleAdapter
            implements Function<io.reactivex.rxjava3.core.Observable<?>, io.reactivex.rxjava3.core.Single<?>> {

        INSTANCE;

        @Override
        public io.reactivex.rxjava3.core.Single<?> apply(io.reactivex.rxjava3.core.Observable<?> source) {
            return source.singleOrError();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Observable} to {@link Maybe}.
     */
    public enum RxJava3ObservableToMaybeAdapter
            implements Function<io.reactivex.rxjava3.core.Observable<?>, io.reactivex.rxjava3.core.Maybe<?>> {

        INSTANCE;

        @Override
        public io.reactivex.rxjava3.core.Maybe<?> apply(io.reactivex.rxjava3.core.Observable<?> source) {
            return source.singleElement();
        }

    }

    /**
     * An adapter {@link Function} to adopt a {@link Single} to {@link Single}.
     */
    public enum RxJava3SingleToObservableAdapter
            implements Function<io.reactivex.rxjava3.core.Single<?>, io.reactivex.rxjava3.core.Observable<?>> {

        INSTANCE;

        @Override
        public io.reactivex.rxjava3.core.Observable<?> apply(io.reactivex.rxjava3.core.Single<?> source) {
            return source.toObservable();
        }

    }

}
