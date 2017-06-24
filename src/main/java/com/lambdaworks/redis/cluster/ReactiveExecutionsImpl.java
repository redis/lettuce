/*
 * Copyright 2016-2017 the original author or authors.
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
package com.lambdaworks.redis.cluster;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Observable;
import rx.Subscriber;

import com.lambdaworks.redis.cluster.api.rx.ReactiveExecutions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Default implementation of {@link ReactiveExecutions}.
 *
 * @author Mark Paluch
 * @since 4.4
 */
class ReactiveExecutionsImpl<T> implements ReactiveExecutions<T> {

    private Map<RedisClusterNode, CompletableFuture<Observable<? extends T>>> executions;

    public ReactiveExecutionsImpl(Map<RedisClusterNode, CompletableFuture<Observable<? extends T>>> executions) {
        this.executions = executions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Observable<T> observable() {
        return Observable.create(new FromCompletableFuture<>(executions.values())).flatMap(observable -> observable);
    }

    @Override
    public Collection<RedisClusterNode> nodes() {
        return executions.keySet();
    }

    static class FromCompletableFuture<T> implements Observable.OnSubscribe<Observable<? extends T>> {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static final AtomicIntegerFieldUpdater<FromCompletableFuture> LATCH_UPDATER = AtomicIntegerFieldUpdater
                .newUpdater(FromCompletableFuture.class, "latch");

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static final AtomicIntegerFieldUpdater<FromCompletableFuture> TERMINATED_UPDATER = AtomicIntegerFieldUpdater
                .newUpdater(FromCompletableFuture.class, "terminated");

        private final Collection<CompletableFuture<Observable<? extends T>>> futures;

        // Updated with AtomicIntegerFieldUpdater
        @SuppressWarnings("unused")
        private volatile int latch;

        // Updated with AtomicIntegerFieldUpdater
        @SuppressWarnings("unused")
        private volatile int terminated;

        public FromCompletableFuture(Collection<CompletableFuture<Observable<? extends T>>> futures) {

            this.futures = futures;
            LATCH_UPDATER.set(this, futures.size());

        }

        @Override
        public void call(Subscriber<? super Observable<? extends T>> subscriber) {

            subscriber.onStart();

            for (CompletableFuture<Observable<? extends T>> future : futures) {

                future.whenComplete((observable, throwable) -> {

                    if (throwable != null) {

                        if (TERMINATED_UPDATER.compareAndSet(this, 0, 1)) {
                            subscriber.onError(throwable);
                        }

                        return;
                    }

                    if (TERMINATED_UPDATER.get(this) == 1) {
                        return;
                    }

                    subscriber.onNext(observable);

                    if (LATCH_UPDATER.decrementAndGet(this) == 0) {
                        subscriber.onCompleted();
                    }
                });
            }

            if (futures.isEmpty()) {
                subscriber.onCompleted();
            }
        }
    }
}
