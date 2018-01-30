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
package io.lettuce.core.cluster;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import io.lettuce.core.RedisFuture;

/**
 * Pipelining for commands that are executed on multiple cluster nodes. Merges results and emits one composite result.
 *
 * @author Mark Paluch
 */
class PipelinedRedisFuture<V> extends CompletableFuture<V> implements RedisFuture<V> {

    private final CountDownLatch latch = new CountDownLatch(1);

    public PipelinedRedisFuture(CompletionStage<V> completionStage) {
        this(completionStage, v -> v);
    }

    public PipelinedRedisFuture(CompletionStage<V> completionStage, Function<V, V> converter) {
        completionStage.thenAccept(v -> complete(converter.apply(v)))
                .exceptionally(throwable -> {
                    completeExceptionally(throwable);
                    return null;
                });
    }

    public PipelinedRedisFuture(Map<?, ? extends CompletionStage<?>> executions, Function<PipelinedRedisFuture<V>, V> converter) {

        CompletableFuture.allOf(executions.values().toArray(new CompletableFuture<?>[executions.size()]))
                .thenRun(() -> complete(converter.apply(this))).exceptionally(throwable -> {
                    completeExceptionally(throwable);
                    return null;
                });
    }

    @Override
    public boolean complete(V value) {
        boolean result = super.complete(value);
        latch.countDown();
        return result;
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {

        boolean value = super.completeExceptionally(ex);
        latch.countDown();
        return value;
    }

    @Override
    public String getError() {
        return null;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

}
