/*
 * Copyright 2017-2020 the original author or authors.
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
package io.lettuce.core;

import java.net.SocketAddress;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.lettuce.core.api.StatefulConnection;

/**
 * A {@code ConnectionFuture} represents the result of an asynchronous connection initialization. The future provides a
 * {@link StatefulConnection} on successful completion. It also provides the remote {@link SocketAddress}.
 *
 * @since 4.4
 */
public interface ConnectionFuture<T> extends CompletionStage<T>, Future<T> {

    /**
     * Create a {@link ConnectionFuture} given {@link SocketAddress} and {@link CompletableFuture} holding the connection
     * progress.
     *
     * @param remoteAddress initial connection endpoint, must not be {@code null}.
     * @param delegate must not be {@code null}.
     * @return the {@link ConnectionFuture} for {@link SocketAddress} and {@link CompletableFuture}.
     * @since 5.0
     */
    static <T> ConnectionFuture<T> from(SocketAddress remoteAddress, CompletableFuture<T> delegate) {
        return new DefaultConnectionFuture<>(remoteAddress, delegate);
    }

    /**
     * Create a completed {@link ConnectionFuture} given {@link SocketAddress} and {@code value} holding the value.
     *
     * @param remoteAddress initial connection endpoint, must not be {@code null}.
     * @param value must not be {@code null}.
     * @return the {@link ConnectionFuture} for {@link SocketAddress} and {@code value}.
     * @since 5.1
     */
    static <T> ConnectionFuture<T> completed(SocketAddress remoteAddress, T value) {
        return new DefaultConnectionFuture<>(remoteAddress, CompletableFuture.completedFuture(value));
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves its result.
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    T get() throws InterruptedException, ExecutionException;

    /**
     * Return the remote {@link SocketAddress}.
     *
     * @return the remote {@link SocketAddress}. May be {@code null} until the socket address is resolved.
     */
    SocketAddress getRemoteAddress();

    /**
     * Returns the result value when complete, or throws an (unchecked) exception if completed exceptionally. To better conform
     * with the use of common functional forms, if a computation involved in the completion of this CompletableFuture threw an
     * exception, this method throws an (unchecked) {@link CompletionException} with the underlying exception as its cause.
     *
     * @return the result value
     * @throws CancellationException if the computation was cancelled
     * @throws CompletionException if this future completed exceptionally or a completion computation threw an exception
     */
    T join();

    @Override
    <U> ConnectionFuture<U> thenApply(Function<? super T, ? extends U> fn);

    @Override
    <U> ConnectionFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn);

    @Override
    <U> ConnectionFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor);

    @Override
    ConnectionFuture<Void> thenAccept(Consumer<? super T> action);

    @Override
    ConnectionFuture<Void> thenAcceptAsync(Consumer<? super T> action);

    @Override
    ConnectionFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor);

    @Override
    ConnectionFuture<Void> thenRun(Runnable action);

    @Override
    ConnectionFuture<Void> thenRunAsync(Runnable action);

    @Override
    ConnectionFuture<Void> thenRunAsync(Runnable action, Executor executor);

    @Override
    <U, V> ConnectionFuture<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn);

    @Override
    <U, V> ConnectionFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn);

    @Override
    <U, V> ConnectionFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, Executor executor);

    @Override
    <U> ConnectionFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action);

    @Override
    <U> ConnectionFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action);

    @Override
    <U> ConnectionFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action,
            Executor executor);

    @Override
    ConnectionFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action);

    @Override
    ConnectionFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action);

    @Override
    ConnectionFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor);

    @Override
    <U> ConnectionFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn);

    @Override
    <U> ConnectionFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn);

    @Override
    <U> ConnectionFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
            Executor executor);

    @Override
    ConnectionFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action);

    @Override
    ConnectionFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action);

    @Override
    ConnectionFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor);

    @Override
    ConnectionFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action);

    @Override
    ConnectionFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action);

    @Override
    ConnectionFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor);

    @Override
    <U> ConnectionFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn);

    <U> ConnectionFuture<U> thenCompose(BiFunction<? super T, ? super Throwable, ? extends CompletionStage<U>> fn);

    @Override
    <U> ConnectionFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn);

    @Override
    <U> ConnectionFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor);

    @Override
    ConnectionFuture<T> exceptionally(Function<Throwable, ? extends T> fn);

    @Override
    ConnectionFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);

    @Override
    ConnectionFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action);

    @Override
    ConnectionFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor);

    @Override
    <U> ConnectionFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);

    @Override
    <U> ConnectionFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn);

    @Override
    <U> ConnectionFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor);

}
