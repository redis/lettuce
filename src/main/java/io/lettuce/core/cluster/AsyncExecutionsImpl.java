/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.cluster;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;

import io.lettuce.core.cluster.api.async.AsyncExecutions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
class AsyncExecutionsImpl<T> implements AsyncExecutions<T> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final AtomicReferenceFieldUpdater<AsyncExecutionsImpl<?>, CompletionStage> UPDATER = (AtomicReferenceFieldUpdater) AtomicReferenceFieldUpdater
            .newUpdater(AsyncExecutionsImpl.class, CompletionStage.class, "publicStage");

    private final Map<RedisClusterNode, CompletableFuture<T>> executions;

    private volatile CompletionStage<List<T>> publicStage;

    @SuppressWarnings("unchecked")
    public AsyncExecutionsImpl(Map<RedisClusterNode, CompletionStage<? extends T>> executions) {

        Map<RedisClusterNode, CompletionStage<? extends T>> map = new HashMap<>(executions);
        this.executions = Collections.unmodifiableMap((Map) map);
    }

    @Override
    public Map<RedisClusterNode, CompletableFuture<T>> asMap() {
        return executions;
    }

    @Override
    public Iterator<CompletableFuture<T>> iterator() {
        return asMap().values().iterator();
    }

    @Override
    public Collection<RedisClusterNode> nodes() {
        return executions.keySet();
    }

    @Override
    public CompletableFuture<T> get(RedisClusterNode redisClusterNode) {
        return executions.get(redisClusterNode);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public CompletableFuture<T>[] futures() {
        return executions.values().toArray(new CompletableFuture[0]);
    }

    @Override
    public <R, A> CompletionStage<R> thenCollect(Collector<? super T, A, R> collector) {

        return publicStage().thenApply(items -> {

            A container = collector.supplier().get();

            BiConsumer<A, ? super T> accumulator = collector.accumulator();
            items.forEach(item -> accumulator.accept(container, item));

            if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
                return (R) container;
            }

            return collector.finisher().apply(container);
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private CompletionStage<List<T>> publicStage() {

        CompletionStage stage = UPDATER.get(this);

        if (stage == null) {
            stage = createPublicStage(this.executions);
            UPDATER.compareAndSet(this, null, stage);
        }

        return stage;
    }

    @SuppressWarnings("rawtypes")
    private CompletableFuture<List<T>> createPublicStage(Map<RedisClusterNode, CompletableFuture<T>> map) {

        return CompletableFuture.allOf(map.values().toArray(new CompletableFuture[0])).thenApply(ignore -> {

            List<T> results = new ArrayList<>(map.size());
            for (CompletionStage<? extends T> value : map.values()) {
                results.add(value.toCompletableFuture().join());
            }

            return results;
        });
    }

    // --------------------------------
    // delegate methods.
    // --------------------------------

    @Override
    public <U> CompletionStage<U> thenApply(Function<? super List<T>, ? extends U> fn) {
        return publicStage().thenApply(fn);
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(Function<? super List<T>, ? extends U> fn) {
        return publicStage().thenApplyAsync(fn);
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(Function<? super List<T>, ? extends U> fn, Executor executor) {
        return publicStage().thenApplyAsync(fn, executor);
    }

    @Override
    public CompletionStage<Void> thenAccept(Consumer<? super List<T>> action) {
        return publicStage().thenAccept(action);
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super List<T>> action) {
        return publicStage().thenAcceptAsync(action);
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super List<T>> action, Executor executor) {
        return publicStage().thenAcceptAsync(action, executor);
    }

    @Override
    public CompletionStage<Void> thenRun(Runnable action) {
        return publicStage().thenRun(action);
    }

    @Override
    public CompletionStage<Void> thenRunAsync(Runnable action) {
        return publicStage().thenRunAsync(action);
    }

    @Override
    public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
        return publicStage().thenRunAsync(action, executor);
    }

    @Override
    public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super List<T>, ? super U, ? extends V> fn) {
        return publicStage().thenCombine(other, fn);
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super List<T>, ? super U, ? extends V> fn) {
        return publicStage().thenCombineAsync(other, fn);
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super List<T>, ? super U, ? extends V> fn, Executor executor) {
        return publicStage().thenCombineAsync(other, fn, executor);
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
            BiConsumer<? super List<T>, ? super U> action) {
        return publicStage().thenAcceptBoth(other, action);
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super List<T>, ? super U> action) {
        return publicStage().thenAcceptBothAsync(other, action);
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super List<T>, ? super U> action, Executor executor) {
        return publicStage().thenAcceptBothAsync(other, action, executor);
    }

    @Override
    public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return publicStage().runAfterBoth(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return publicStage().runAfterBothAsync(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return publicStage().runAfterBothAsync(other, action, executor);
    }

    @Override
    public <U> CompletionStage<U> applyToEither(CompletionStage<? extends List<T>> other, Function<? super List<T>, U> fn) {
        return publicStage().applyToEither(other, fn);
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends List<T>> other,
            Function<? super List<T>, U> fn) {
        return publicStage().applyToEitherAsync(other, fn);
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends List<T>> other, Function<? super List<T>, U> fn,
            Executor executor) {
        return publicStage().applyToEitherAsync(other, fn, executor);
    }

    @Override
    public CompletionStage<Void> acceptEither(CompletionStage<? extends List<T>> other, Consumer<? super List<T>> action) {
        return publicStage().acceptEither(other, action);
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends List<T>> other, Consumer<? super List<T>> action) {
        return publicStage().acceptEitherAsync(other, action);
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends List<T>> other, Consumer<? super List<T>> action,
            Executor executor) {
        return publicStage().acceptEitherAsync(other, action, executor);
    }

    @Override
    public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return publicStage().runAfterEither(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return publicStage().runAfterEitherAsync(other, action);
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return publicStage().runAfterEitherAsync(other, action, executor);
    }

    @Override
    public <U> CompletionStage<U> thenCompose(Function<? super List<T>, ? extends CompletionStage<U>> fn) {
        return publicStage().thenCompose(fn);
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(Function<? super List<T>, ? extends CompletionStage<U>> fn) {
        return publicStage().thenComposeAsync(fn);
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(Function<? super List<T>, ? extends CompletionStage<U>> fn,
            Executor executor) {
        return publicStage().thenComposeAsync(fn, executor);
    }

    @Override
    public CompletionStage<List<T>> exceptionally(Function<Throwable, ? extends List<T>> fn) {
        return publicStage().exceptionally(fn);
    }

    @Override
    public CompletionStage<List<T>> whenComplete(BiConsumer<? super List<T>, ? super Throwable> action) {
        return publicStage().whenComplete(action);
    }

    @Override
    public CompletionStage<List<T>> whenCompleteAsync(BiConsumer<? super List<T>, ? super Throwable> action) {
        return publicStage().whenCompleteAsync(action);
    }

    @Override
    public CompletionStage<List<T>> whenCompleteAsync(BiConsumer<? super List<T>, ? super Throwable> action,
            Executor executor) {
        return publicStage().whenCompleteAsync(action, executor);
    }

    @Override
    public <U> CompletionStage<U> handle(BiFunction<? super List<T>, Throwable, ? extends U> fn) {
        return publicStage().handle(fn);
    }

    @Override
    public <U> CompletionStage<U> handleAsync(BiFunction<? super List<T>, Throwable, ? extends U> fn) {
        return publicStage().handleAsync(fn);
    }

    @Override
    public <U> CompletionStage<U> handleAsync(BiFunction<? super List<T>, Throwable, ? extends U> fn, Executor executor) {
        return publicStage().handleAsync(fn, executor);
    }

    @Override
    public CompletableFuture<List<T>> toCompletableFuture() {
        return publicStage().toCompletableFuture();
    }

}
