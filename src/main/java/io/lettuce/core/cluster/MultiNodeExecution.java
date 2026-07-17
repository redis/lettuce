/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.cluster;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.internal.Exceptions;

/**
 * Utility to perform and synchronize command executions on multiple cluster nodes.
 *
 * @author Mark Paluch
 */
public class MultiNodeExecution {

    public static <T> T execute(Callable<T> function) {
        try {
            return function.call();
        } catch (Exception e) {
            throw Exceptions.bubble(e);
        }
    }

    /**
     * Aggregate (sum) results of the {@link RedisFuture}s.
     *
     * @param executions mapping of a key to the future
     * @return future producing an aggregation result
     */
    public static RedisFuture<Long> aggregateAsync(Map<?, ? extends CompletionStage<Long>> executions) {

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            AtomicLong result = new AtomicLong();
            for (CompletionStage<Long> future : executions.values()) {
                Long value = execute(() -> future.toCompletableFuture().get());
                if (value != null) {
                    result.getAndAdd(value);
                }
            }

            return result.get();
        });
    }

    /**
     * Logical {@code AND} of the {@link RedisFuture} results. The result is {@code true} only if every future produces
     * {@code true}; a {@code null} result is treated as {@code false}.
     *
     * @param executions mapping of a key to the future
     * @return future producing the conjunction of all results
     */
    public static RedisFuture<Boolean> andOfAsync(Map<?, ? extends CompletionStage<Boolean>> executions) {

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            boolean result = true;
            for (CompletionStage<Boolean> future : executions.values()) {
                Boolean value = execute(() -> future.toCompletableFuture().get());
                result = result && Boolean.TRUE.equals(value);
            }

            return result;
        });
    }

    /**
     * Returns the maximum of the {@link RedisFuture} results.
     *
     * @param executions mapping of a key to the future
     * @return future producing the maximum result
     */
    public static RedisFuture<Long> maxOfAsync(Map<?, ? extends CompletionStage<Long>> executions) {

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            long result = 0;
            for (CompletionStage<Long> future : executions.values()) {
                Long value = execute(() -> future.toCompletableFuture().get());
                if (value != null) {
                    result = Math.max(result, value);
                }
            }

            return result;
        });
    }

    /**
     * Returns the result of the first {@link RedisFuture} and guarantee that all futures are finished.
     *
     * @param executions mapping of a key to the future
     * @param <T> result type
     * @return future returning the first result.
     */
    public static <T> RedisFuture<T> firstOfAsync(Map<?, ? extends CompletionStage<T>> executions) {

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            // make sure, that all futures are executed before returning the result.
            for (CompletionStage<T> future : executions.values()) {
                execute(() -> future.toCompletableFuture().get());
            }
            for (CompletionStage<T> future : executions.values()) {
                return execute(() -> future.toCompletableFuture().get());
            }
            return null;
        });
    }

    /**
     * Returns the result of the last {@link RedisFuture} and guarantee that all futures are finished.
     *
     * @param executions mapping of a key to the future
     * @param <T> result type
     * @return future returning the first result.
     */
    public static <T> RedisFuture<T> lastOfAsync(Map<?, ? extends CompletionStage<T>> executions) {

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            // make sure, that all futures are executed before returning the result.
            T result = null;
            for (CompletionStage<T> future : executions.values()) {
                result = execute(() -> future.toCompletableFuture().get());
            }
            return result;
        });
    }

    /**
     * Returns always {@literal OK} and guarantee that all futures are finished.
     *
     * @param executions mapping of a key to the future
     * @return future returning the first result.
     */
    public static RedisFuture<String> alwaysOkOfAsync(Map<?, ? extends CompletionStage<String>> executions) {

        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {

            synchronize(executions);

            return "OK";
        });
    }

    private static void synchronize(Map<?, ? extends CompletionStage<String>> executions) {

        // make sure, that all futures are executed before returning the result.
        for (CompletionStage<String> future : executions.values()) {
            try {
                future.toCompletableFuture().get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RedisCommandInterruptedException(e);
            } catch (ExecutionException e) {
                // swallow exceptions
            }
        }
    }

}
