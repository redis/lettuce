/*
 * Copyright 2018 the original author or authors.
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
package io.lettuce.test;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.api.async.AsyncExecutions;

/**
 * Utility methods to synchronize and create futures.
 *
 * @author Mark Paluch
 */
public class Futures {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /**
     * Check if all {@code futures} are {@link Future#isDone() completed}.
     *
     * @param futures
     * @return {@literal true} if all {@code futures} are {@link Future#isDone() completed}
     */
    public static boolean areAllCompleted(Collection<? extends Future<?>> futures) {

        for (Future<?> future : futures) {
            if (!future.isDone()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Await completion for all {@link Future} guarded by the global {@link #TIMEOUT}.
     */
    public static boolean await(Future<?> future) {

        if (!LettuceFutures.awaitAll(TIMEOUT, future)) {
            throw new IllegalStateException("Future timeout");
        }

        return true;
    }

    /**
     * Await completion for all {@link AsyncExecutions}s guarded by the global {@link #TIMEOUT}.
     *
     * @param executions
     */
    public static boolean await(AsyncExecutions<?> executions) {
        return awaitAll(Arrays.asList(executions.futures()));
    }

    /**
     * Await completion for all {@link Future}s guarded by the global {@link #TIMEOUT}.
     *
     * @param futures
     */
    public static boolean awaitAll(Collection<? extends Future<?>> futures) {

        if (!LettuceFutures.awaitAll(TIMEOUT, futures.toArray(new Future[0]))) {
            throw new IllegalStateException("Future timeout");
        }

        return true;
    }

    /**
     * Retrieve the value from the {@link Future} guarded by the global {@link #TIMEOUT}.
     *
     * @param future
     * @param <T>
     */
    public static <T> T get(Future<T> future) {

        if (!LettuceFutures.awaitAll(TIMEOUT, future)) {
            throw new IllegalStateException("Future timeout");
        }

        try {
            return future.get();
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    /**
     * Retrieve the value from the {@link CompletableFuture} guarded by the global {@link #TIMEOUT}.
     *
     * @param future
     * @param <T>
     */
    public static <T> T get(CompletableFuture<T> future) {
        return get((Future<T>) future);
    }

    /**
     * Retrieve the value from the {@link CompletionStage} guarded by the global {@link #TIMEOUT}.
     *
     * @param completionStage
     * @param <T>
     */
    public static <T> T get(CompletionStage<T> completionStage) {
        return get(completionStage.toCompletableFuture());
    }

    /**
     * Retrieve the value from the {@link RedisFuture} guarded by the global {@link #TIMEOUT}.
     *
     * @param future
     * @param <T>
     */
    public static <T> T get(RedisFuture<T> future) {
        return get(future.toCompletableFuture());
    }

}
