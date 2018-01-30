/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core;

import java.net.SocketAddress;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Default {@link CompletableFuture} implementation. Delegates calls to the decorated {@link CompletableFuture} and provides a
 * {@link SocketAddress}.
 *
 * @since 4.4
 */
class DefaultConnectionFuture<T> extends CompletableFuture<T> implements ConnectionFuture<T> {

    private final SocketAddress remoteAddress;
    private final CompletableFuture<T> delegate;

    public DefaultConnectionFuture(SocketAddress remoteAddress, CompletableFuture<T> delegate) {

        this.remoteAddress = remoteAddress;
        this.delegate = delegate;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    private <U> DefaultConnectionFuture<U> adopt(CompletableFuture<U> newFuture) {
        return new DefaultConnectionFuture<>(remoteAddress, newFuture);
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return delegate.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.get(timeout, unit);
    }

    @Override
    public T join() {
        return delegate.join();
    }

    @Override
    public T getNow(T valueIfAbsent) {
        return delegate.getNow(valueIfAbsent);
    }

    @Override
    public boolean complete(T value) {
        return delegate.complete(value);
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        return delegate.completeExceptionally(ex);
    }

    @Override
    public <U> DefaultConnectionFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return adopt(delegate.thenApply(fn));
    }

    @Override
    public <U> DefaultConnectionFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return adopt(delegate.thenApplyAsync(fn));
    }

    @Override
    public <U> DefaultConnectionFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return adopt(delegate.thenApplyAsync(fn, executor));
    }

    @Override
    public DefaultConnectionFuture<Void> thenAccept(Consumer<? super T> action) {
        return adopt(delegate.thenAccept(action));
    }

    @Override
    public DefaultConnectionFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return adopt(delegate.thenAcceptAsync(action));
    }

    @Override
    public DefaultConnectionFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return adopt(delegate.thenAcceptAsync(action, executor));
    }

    @Override
    public DefaultConnectionFuture<Void> thenRun(Runnable action) {
        return adopt(delegate.thenRun(action));
    }

    @Override
    public DefaultConnectionFuture<Void> thenRunAsync(Runnable action) {
        return adopt(delegate.thenRunAsync(action));
    }

    @Override
    public DefaultConnectionFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return adopt(delegate.thenRunAsync(action, executor));
    }

    @Override
    public <U, V> DefaultConnectionFuture<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return adopt(delegate.thenCombine(other, fn));
    }

    @Override
    public <U, V> DefaultConnectionFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return adopt(delegate.thenCombineAsync(other, fn));
    }

    @Override
    public <U, V> DefaultConnectionFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return adopt(delegate.thenCombineAsync(other, fn, executor));
    }

    @Override
    public <U> DefaultConnectionFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return adopt(delegate.thenAcceptBoth(other, action));
    }

    @Override
    public <U> DefaultConnectionFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return adopt(delegate.thenAcceptBothAsync(other, action));
    }

    @Override
    public <U> DefaultConnectionFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor) {
        return adopt(delegate.thenAcceptBothAsync(other, action, executor));
    }

    @Override
    public DefaultConnectionFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return adopt(delegate.runAfterBoth(other, action));
    }

    @Override
    public DefaultConnectionFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return adopt(delegate.runAfterBothAsync(other, action));
    }

    @Override
    public DefaultConnectionFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return adopt(delegate.runAfterBothAsync(other, action, executor));
    }

    @Override
    public <U> DefaultConnectionFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return adopt(delegate.applyToEither(other, fn));
    }

    @Override
    public <U> DefaultConnectionFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return adopt(delegate.applyToEitherAsync(other, fn));
    }

    @Override
    public <U> DefaultConnectionFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
            Executor executor) {
        return adopt(delegate.applyToEitherAsync(other, fn, executor));
    }

    @Override
    public DefaultConnectionFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return adopt(delegate.acceptEither(other, action));
    }

    @Override
    public DefaultConnectionFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return adopt(delegate.acceptEitherAsync(other, action));
    }

    @Override
    public DefaultConnectionFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
            Executor executor) {
        return adopt(delegate.acceptEitherAsync(other, action, executor));
    }

    @Override
    public DefaultConnectionFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return adopt(delegate.runAfterEither(other, action));
    }

    @Override
    public DefaultConnectionFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return adopt(delegate.runAfterEitherAsync(other, action));
    }

    @Override
    public DefaultConnectionFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return adopt(delegate.runAfterEitherAsync(other, action, executor));
    }

    @Override
    public <U> DefaultConnectionFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return adopt(delegate.thenCompose(fn));
    }

    @Override
    public <U> DefaultConnectionFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return adopt(delegate.thenComposeAsync(fn));
    }

    @Override
    public <U> DefaultConnectionFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
            Executor executor) {
        return adopt(delegate.thenComposeAsync(fn, executor));
    }

    @Override
    public DefaultConnectionFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return adopt(delegate.whenComplete(action));
    }

    @Override
    public DefaultConnectionFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return adopt(delegate.whenCompleteAsync(action));
    }

    @Override
    public DefaultConnectionFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return adopt(delegate.whenCompleteAsync(action, executor));
    }

    @Override
    public <U> DefaultConnectionFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return adopt(delegate.handle(fn));
    }

    @Override
    public <U> DefaultConnectionFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return adopt(delegate.handleAsync(fn));
    }

    @Override
    public <U> DefaultConnectionFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return adopt(delegate.handleAsync(fn, executor));
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return delegate.toCompletableFuture();
    }

    @Override
    public DefaultConnectionFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return adopt(delegate.exceptionally(fn));
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    @Override
    public boolean isCompletedExceptionally() {
        return delegate.isCompletedExceptionally();
    }

    @Override
    public void obtrudeValue(T value) {
        delegate.obtrudeValue(value);
    }

    @Override
    public void obtrudeException(Throwable ex) {
        delegate.obtrudeException(ex);
    }

    @Override
    public int getNumberOfDependents() {
        return delegate.getNumberOfDependents();
    }
}
