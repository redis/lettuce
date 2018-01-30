/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.resource;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.util.concurrent.*;

/**
 * Utility class to support netty's future handling.
 *
 * @author Mark Paluch
 * @since 3.4
 */
class Futures {

    /**
     * Create a promise that emits a {@code Boolean} value on completion of the {@code future}
     *
     * @param future the future.
     * @return Promise emitting a {@code Boolean} value. {@literal true} if the {@code future} completed successfully, otherwise
     *         the cause wil be transported.
     */
    static Promise<Boolean> toBooleanPromise(Future<?> future) {

        DefaultPromise<Boolean> result = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

        if (future.isDone() || future.isCancelled()) {
            if (future.isSuccess()) {
                result.setSuccess(true);
            } else {
                result.setFailure(future.cause());
            }
            return result;
        }

        future.addListener((GenericFutureListener<Future<Object>>) f -> {

            if (f.isSuccess()) {
                result.setSuccess(true);
            } else {
                result.setFailure(f.cause());
            }
        });
        return result;
    }

    /**
     * Promise aggregator that aggregates multiple promises into one {@link Promise}. The aggregator workflow is:
     * <ol>
     * <li>Create a new instance of {@link io.lettuce.core.resource.Futures.PromiseAggregator}</li>
     * <li>Call {@link #expectMore(int)} until the number of expected futures is reached</li>
     * <li>Arm the aggregator using {@link #arm()}</li>
     * <li>Add the number of futures using {@link #add(Promise[])} until the expectation is met. The added futures can be either
     * done or in progress.</li>
     * <li>The {@code aggregatePromise} is released/finished as soon as the last future/promise completes</li>
     *
     * </ol>
     *
     * @param <V> Result value type
     * @param <F> Future type
     */
    static class PromiseAggregator<V, F extends Future<V>> implements GenericFutureListener<F> {

        private final Promise<?> aggregatePromise;
        private Set<Promise<V>> pendingPromises;
        private AtomicInteger expectedPromises = new AtomicInteger();
        private AtomicInteger processedPromises = new AtomicInteger();
        private boolean armed;

        /**
         * Creates a new instance.
         *
         * @param aggregatePromise the {@link Promise} to notify
         */
        public PromiseAggregator(Promise<V> aggregatePromise) {
            LettuceAssert.notNull(aggregatePromise, "AggregatePromise must not be null");
            this.aggregatePromise = aggregatePromise;
        }

        /**
         * Add the number of {@code count} to the count of expected promises.
         *
         * @param count number of futures/promises, that is added to the overall expectation count.
         * @throws IllegalStateException if the aggregator was armed
         */
        public void expectMore(int count) {
            LettuceAssert.assertState(!armed, "Aggregator is armed and does not allow any further expectations");

            expectedPromises.addAndGet(count);
        }

        /**
         * Arm the aggregator to expect completion of the futures.
         *
         * @throws IllegalStateException if the aggregator was armed
         */
        public void arm() {
            LettuceAssert.assertState(!armed, "Aggregator is already armed");
            armed = true;
        }

        /**
         * Add the given {@link Promise}s to the aggregator.
         *
         * @param promises the promises
         * @throws IllegalStateException if the aggregator was not armed
         */
        @SafeVarargs
        public final PromiseAggregator<V, F> add(Promise<V>... promises) {

            LettuceAssert.notNull(promises, "Promises must not be null");
            LettuceAssert.assertState(armed,
                    "Aggregator is not armed and does not allow adding promises in that state. Call arm() first.");

            if (promises.length == 0) {
                return this;
            }
            synchronized (this) {
                if (pendingPromises == null) {
                    int size;
                    if (promises.length > 1) {
                        size = promises.length;
                    } else {
                        size = 2;
                    }
                    pendingPromises = new LinkedHashSet<>(size);
                }
                for (Promise<V> p : promises) {
                    if (p == null) {
                        continue;
                    }
                    pendingPromises.add(p);
                    p.addListener(this);
                }
            }
            return this;
        }

        @Override
        public synchronized void operationComplete(F future) throws Exception {
            if (pendingPromises == null) {
                aggregatePromise.setSuccess(null);
            } else {
                pendingPromises.remove(future);
                processedPromises.incrementAndGet();
                if (!future.isSuccess()) {
                    Throwable cause = future.cause();
                    aggregatePromise.setFailure(cause);
                    for (Promise<V> pendingFuture : pendingPromises) {
                        pendingFuture.setFailure(cause);
                    }
                } else if (processedPromises.get() == expectedPromises.get()) {
                    if (pendingPromises.isEmpty()) {
                        aggregatePromise.setSuccess(null);
                    } else {
                        throw new IllegalStateException(
                                "Processed promises == expected promises but pending promises is not empty. This should not have happened!");
                    }
                }
            }
        }
    }
}
