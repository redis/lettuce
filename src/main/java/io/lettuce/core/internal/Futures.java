package io.lettuce.core.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.resource.ClientResources;
import io.netty.channel.ChannelFuture;
import io.netty.util.Timeout;

/**
 * Utility methods for {@link java.util.concurrent.Future} handling. This class is part of the internal API and may change
 * without further notice.
 *
 * @author Mark Paluch
 * @author jinkshower
 * @since 5.1
 */
public abstract class Futures {

    private Futures() {
        // no instances allowed
    }

    /**
     * Create a composite {@link CompletableFuture} that is composed of the given {@code stages}.
     *
     * @param stages must not be {@code null}.
     * @return the composed {@link CompletableFuture}.
     * @since 5.1.1
     */
    @SuppressWarnings({ "rawtypes" })
    public static CompletableFuture<Void> allOf(Collection<? extends CompletionStage<?>> stages) {

        LettuceAssert.notNull(stages, "Futures must not be null");

        CompletionStage[] copies = stages.toArray(new CompletionStage[0]);
        CompletableFuture[] futures = new CompletableFuture[copies.length];

        int index = 0;
        for (CompletionStage<?> stage : copies) {
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
     * Unwrap exceptions from a {@link CompletionStage} into a new {@link CompletableFuture}.
     * <p>
     * The stage is bridged through {@link CompletionStage#whenComplete(java.util.function.BiConsumer)} rather than
     * {@link CompletionStage#toCompletableFuture()}, which the contract permits a minimal {@link CompletionStage}
     * implementation to reject with {@link UnsupportedOperationException}.
     *
     * @param stage the original stage
     * @param <T> the result type
     * @return a new {@link CompletableFuture} with unwrapped exceptions
     * @since 7.9
     */
    public static <T> CompletableFuture<T> unwrapExceptions(CompletionStage<T> stage) {

        CompletableFuture<T> f = new CompletableFuture<>();
        stage.whenComplete((v, t) -> {
            if (t != null) {
                Throwable cause = Exceptions.unwrap(t);
                f.completeExceptionally(cause != null ? cause : t);
            } else {
                f.complete(v);
            }
        });
        return f;
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

    /**
     * Return a {@link CompletableFuture} that mirrors {@code source} but completes exceptionally with {@link TimeoutException}
     * if {@code source} does not complete within {@code duration}. The timeout is scheduled on the
     * {@link ClientResources#timer()} and cancelled upon source completion. Timing out does not cancel {@code source}. If
     * {@code source} is already done, or {@code duration} is {@link Duration#ZERO} (no timeout), {@code source} itself is
     * returned.
     *
     * @param source the source future, must not be {@code null}.
     * @param duration timeout duration, must not be {@code null} or negative. {@link Duration#ZERO} means "do not time out".
     * @param resources client resources providing the timer, must not be {@code null}.
     * @param taskName short description of the awaited operation, used in the {@link TimeoutException} message; must not be
     *        {@code null}.
     * @param <T> the result type.
     * @return a {@link CompletableFuture} completing with the same result as {@code source} or with a {@link TimeoutException}.
     * @since 8.0
     */
    public static <T> CompletableFuture<T> withTimeout(CompletableFuture<T> source, Duration duration,
            ClientResources resources, String taskName) {

        LettuceAssert.notNull(source, "Source future must not be null");
        LettuceAssert.notNull(duration, "Duration must not be null");
        LettuceAssert.isTrue(!duration.isNegative(), "Duration must not be negative");
        LettuceAssert.notNull(resources, "ClientResources must not be null");
        LettuceAssert.notNull(taskName, "Task name must not be null");

        if (source.isDone() || duration.isZero()) {
            return source;
        }

        CompletableFuture<T> result = new CompletableFuture<>();
        Timeout scheduled = resources.timer().newTimeout(
                t -> result.completeExceptionally(
                        new TimeoutException(taskName + " timed out after " + duration.toMillis() + "ms")),
                duration.toNanos(), TimeUnit.NANOSECONDS);

        source.whenComplete((value, err) -> {
            scheduled.cancel();
            if (err != null) {
                result.completeExceptionally(err);
            } else {
                result.complete(value);
            }
        });

        return result;
    }

    /**
     * Try {@code attempts} in order, completing with the result of the first one that succeeds. Each attempt is invoked lazily:
     * the first attempt starts immediately, and each subsequent attempt starts only after the previous one fails. Every attempt
     * is invoked through this method, so a synchronous throw from a supplier is captured as a failure rather than propagated to
     * the caller. If all attempts fail, {@code onAllFailed} receives the failures in the order they occurred and returns the
     * throwable to complete with.
     *
     * @param attempts the attempts to try in order, must not be {@code null} or empty.
     * @param onAllFailed builds the throwable to propagate from all collected failures, invoked only when every attempt fails.
     * @param <T> the result type.
     * @return a {@link CompletableFuture} completing with the first successful attempt, or with {@code onAllFailed} applied to
     *         all failures.
     * @since 8.0
     */
    public static <T> CompletableFuture<T> withFallback(List<Supplier<CompletionStage<T>>> attempts,
            Function<List<Throwable>, Throwable> onAllFailed) {

        LettuceAssert.isTrue(attempts != null && !attempts.isEmpty(), "Attempts must not be empty");
        LettuceAssert.notNull(onAllFailed, "Error handler must not be null");

        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>(attempts.size()));

        CompletableFuture<T> chain = attempt(attempts.get(0), failures);
        for (int i = 1; i < attempts.size(); i++) {
            chain = fallbackTo(chain, attempts.get(i), failures);
        }

        CompletableFuture<T> result = new CompletableFuture<>();
        chain.whenComplete((value, error) -> {
            if (error == null) {
                result.complete(value);
            } else {
                result.completeExceptionally(onAllFailed.apply(failures));
            }
        });
        return result;
    }

    private static <T> CompletableFuture<T> fallbackTo(CompletableFuture<T> current, Supplier<CompletionStage<T>> next,
            List<Throwable> failures) {

        CompletableFuture<T> result = new CompletableFuture<>();
        current.whenComplete((value, error) -> {
            if (error == null) {
                result.complete(value);
            } else {
                attempt(next, failures).whenComplete((fallbackValue, fallbackError) -> {
                    if (fallbackError != null) {
                        result.completeExceptionally(fallbackError);
                    } else {
                        result.complete(fallbackValue);
                    }
                });
            }
        });
        return result;
    }

    private static <T> CompletableFuture<T> attempt(Supplier<? extends CompletionStage<T>> supplier, List<Throwable> failures) {

        CompletableFuture<T> future = new CompletableFuture<>();

        CompletionStage<T> stage;
        try {
            stage = supplier.get();
        } catch (Throwable t) {
            failures.add(t);
            future.completeExceptionally(t);
            return future;
        }

        stage.whenComplete((value, error) -> {
            if (error == null) {
                future.complete(value);
            } else {
                Throwable cause = Exceptions.unwrap(error);
                failures.add(cause);
                future.completeExceptionally(cause);
            }
        });
        return future;
    }

}
