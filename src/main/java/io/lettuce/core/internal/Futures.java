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
package io.lettuce.core.internal;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.*;

import io.lettuce.core.RedisFuture;
import io.netty.channel.ChannelFuture;

/**
 * Utility methods for {@link java.util.concurrent.Future} handling. This class is part of the internal API and may change
 * without further notice.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public abstract class Futures {

    private Futures() {
        // no instances allowed
    }

    /**
     * Create a composite {@link CompletableFuture} is composed from the given {@code stages}.
     *
     * @param stages must not be {@code null}.
     * @return the composed {@link CompletableFuture}.
     * @since 5.1.1
     */
    @SuppressWarnings({ "rawtypes" })
    public static CompletableFuture<Void> allOf(Collection<? extends CompletionStage<?>> stages) {

        LettuceAssert.notNull(stages, "Futures must not be null");

        CompletableFuture[] futures = new CompletableFuture[stages.size()];

        int index = 0;
        for (CompletionStage<?> stage : stages) {
            futures[index++] = stage.toCompletableFuture();
        }

        return CompletableFuture.allOf(futures);
    }

    /**
     * Create a {@link CompletableFuture} that is completed exceptionally with {@code throwable}.
     *
     * @param throwable must not be {@code null}.
     * @return the exceptionally completed {@link CompletableFuture}.
     */
    public static <T> CompletableFuture<T> failed(Throwable throwable) {

        LettuceAssert.notNull(throwable, "Throwable must not be null");

        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);

        return future;
    }

    /**
     * Adapt Netty's {@link ChannelFuture} emitting a {@link Void} result.
     *
     * @param future the {@link ChannelFuture} to adapt.
     * @return the {@link CompletableFuture}.
     * @since 6.0
     */
    public static <V> CompletionStage<V> toCompletionStage(io.netty.util.concurrent.Future<V> future) {

        LettuceAssert.notNull(future, "Future must not be null");

        CompletableFuture<V> promise = new CompletableFuture<>();

        if (future.isDone() || future.isCancelled()) {
            if (future.isSuccess()) {
                promise.complete(null);
            } else {
                promise.completeExceptionally(future.cause());
            }
            return promise;
        }

        future.addListener(f -> {
            if (f.isSuccess()) {
                promise.complete(null);
            } else {
                promise.completeExceptionally(f.cause());
            }
        });

        return promise;
    }

    /**
     * Adapt Netty's {@link io.netty.util.concurrent.Future} emitting a value result into a {@link CompletableFuture}.
     *
     * @param source source {@link io.netty.util.concurrent.Future} emitting signals.
     * @param target target {@link CompletableFuture}.
     * @since 6.0
     */
    public static <V> void adapt(io.netty.util.concurrent.Future<V> source, CompletableFuture<V> target) {

        source.addListener(f -> {
            if (f.isSuccess()) {
                target.complete(null);
            } else {
                target.completeExceptionally(f.cause());
            }
        });

        if (source.isSuccess()) {
            target.complete(null);
        } else if (source.isCancelled()) {
            target.cancel(false);
        } else if (source.isDone() && !source.isSuccess()) {
            target.completeExceptionally(source.cause());
        }
    }

    /**
     * Wait until future is complete or the supplied timeout is reached.
     *
     * @param timeout Maximum time to wait for futures to complete.
     * @param future Future to wait for.
     * @return {@code true} if future completes in time, otherwise {@code false}
     * @since 6.0
     */
    public static boolean await(Duration timeout, Future<?> future) {
        return await(timeout.toNanos(), TimeUnit.NANOSECONDS, future);
    }

    /**
     * Wait until future is complete or the supplied timeout is reached.
     *
     * @param timeout Maximum time to wait for futures to complete.
     * @param unit Unit of time for the timeout.
     * @param future Future to wait for.
     * @return {@code true} if future completes in time, otherwise {@code false}
     * @since 6.0
     */
    public static boolean await(long timeout, TimeUnit unit, Future<?> future) {

        try {
            long nanos = unit.toNanos(timeout);

            if (nanos < 0) {
                return false;
            }

            if (nanos == 0) {
                future.get();
            } else {
                future.get(nanos, TimeUnit.NANOSECONDS);
            }

            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (Exception e) {
            throw Exceptions.fromSynchronization(e);
        }
    }

    /**
     * Wait until futures are complete or the supplied timeout is reached.
     *
     * @param timeout Maximum time to wait for futures to complete.
     * @param futures Futures to wait for.
     * @return {@code true} if all futures complete in time, otherwise {@code false}
     * @since 6.0
     */
    public static boolean awaitAll(Duration timeout, Future<?>... futures) {
        return awaitAll(timeout.toNanos(), TimeUnit.NANOSECONDS, futures);
    }

    /**
     * Wait until futures are complete or the supplied timeout is reached.
     *
     * @param timeout Maximum time to wait for futures to complete.
     * @param unit Unit of time for the timeout.
     * @param futures Futures to wait for.
     * @return {@code true} if all futures complete in time, otherwise {@code false}
     */
    public static boolean awaitAll(long timeout, TimeUnit unit, Future<?>... futures) {

        try {
            long nanos = unit.toNanos(timeout);
            long time = System.nanoTime();

            for (Future<?> f : futures) {

                if (timeout <= 0) {
                    f.get();
                } else {
                    if (nanos < 0) {
                        return false;
                    }

                    f.get(nanos, TimeUnit.NANOSECONDS);

                    long now = System.nanoTime();
                    nanos -= now - time;
                    time = now;
                }
            }

            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (Exception e) {
            throw Exceptions.fromSynchronization(e);
        }
    }

    /**
     * Wait until futures are complete or the supplied timeout is reached. Commands are canceled if the timeout is reached but
     * the command is not finished.
     *
     * @param cmd Command to wait for
     * @param timeout Maximum time to wait for futures to complete
     * @param unit Unit of time for the timeout
     * @param <T> Result type
     * @return Result of the command.
     * @since 6.0
     */
    public static <T> T awaitOrCancel(RedisFuture<T> cmd, long timeout, TimeUnit unit) {

        try {
            if (timeout > 0 && !cmd.await(timeout, unit)) {
                cmd.cancel(true);
                throw ExceptionFactory.createTimeoutException(Duration.ofNanos(unit.toNanos(timeout)));
            }
            return cmd.get();
        } catch (Exception e) {
            throw Exceptions.bubble(e);
        }
    }

}
