package io.lettuce.core;

import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class for connection futures that provides protection against event loop deadlocks.
 * <p>
 * This class ensures that all callbacks (thenApply, thenAccept, etc.) execute on a separate thread pool rather than on Netty
 * event loop threads. This prevents deadlocks when users call blocking sync operations inside callbacks.
 * <p>
 * Example of the problem this solves:
 *
 * <pre>
 * {@code
 * // DANGEROUS with plain CompletableFuture - can deadlock!
 * future.thenApply(conn -> conn.sync().ping());
 *
 * // SAFE with BaseConnectionFuture - always runs on separate thread
 * future.thenApply(conn -> conn.sync().ping());
 * }
 * </pre>
 *
 * @param <T> Connection type
 * @author Ali Takavci
 * @since 7.4
 */
public abstract class BaseConnectionFuture<T> implements CompletionStage<T>, Future<T> {

    protected final CompletableFuture<T> delegate;

    protected final Executor defaultExecutor;

    /**
     * Create a new {@link BaseConnectionFuture} wrapping the given delegate future.
     *
     * @param delegate the underlying CompletableFuture
     */
    protected BaseConnectionFuture(CompletableFuture<T> delegate) {
        this(delegate, ForkJoinPool.commonPool());
    }

    /**
     * Create a new {@link BaseConnectionFuture} wrapping the given delegate future with a custom executor.
     *
     * @param delegate the underlying CompletableFuture
     * @param defaultExecutor the executor to use for async callbacks
     */
    protected BaseConnectionFuture(CompletableFuture<T> delegate, Executor defaultExecutor) {
        this.delegate = delegate;
        this.defaultExecutor = defaultExecutor;
    }

    /**
     * Subclasses must implement this to wrap a new CompletableFuture in the appropriate subclass type.
     *
     * @param future the future to wrap
     * @param <U> the new type
     * @return the wrapped future
     */
    protected abstract <U> CompletionStage<U> wrap(CompletableFuture<U> future);

    // =========================================================================
    // Future interface methods
    // =========================================================================

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return delegate.isCancelled();
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

    // =========================================================================
    // CompletionStage methods - ALL force async execution
    // =========================================================================

    @Override
    public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.thenApplyAsync(fn, defaultExecutor));
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return wrap(delegate.thenApplyAsync(fn, defaultExecutor));
    }

    @Override
    public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return wrap(delegate.thenApplyAsync(fn, executor));
    }

    @Override
    public CompletionStage<Void> thenAccept(Consumer<? super T> action) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.thenAcceptAsync(action, defaultExecutor));
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
        return wrap(delegate.thenAcceptAsync(action, defaultExecutor));
    }

    @Override
    public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return wrap(delegate.thenAcceptAsync(action, executor));
    }

    @Override
    public CompletionStage<Void> thenRun(Runnable action) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.thenRunAsync(action, defaultExecutor));
    }

    @Override
    public CompletionStage<Void> thenRunAsync(Runnable action) {
        return wrap(delegate.thenRunAsync(action, defaultExecutor));
    }

    @Override
    public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
        return wrap(delegate.thenRunAsync(action, executor));
    }

    @Override
    public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.thenCombineAsync(other, fn, defaultExecutor));
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
        return wrap(delegate.thenCombineAsync(other, fn, defaultExecutor));
    }

    @Override
    public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return wrap(delegate.thenCombineAsync(other, fn, executor));
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.thenAcceptBothAsync(other, action, defaultExecutor));
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
        return wrap(delegate.thenAcceptBothAsync(other, action, defaultExecutor));
    }

    @Override
    public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor) {
        return wrap(delegate.thenAcceptBothAsync(other, action, executor));
    }

    @Override
    public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.runAfterBothAsync(other, action, defaultExecutor));
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return wrap(delegate.runAfterBothAsync(other, action, defaultExecutor));
    }

    @Override
    public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return wrap(delegate.runAfterBothAsync(other, action, executor));
    }

    @Override
    public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.applyToEitherAsync(other, fn, defaultExecutor));
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return wrap(delegate.applyToEitherAsync(other, fn, defaultExecutor));
    }

    @Override
    public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
            Executor executor) {
        return wrap(delegate.applyToEitherAsync(other, fn, executor));
    }

    @Override
    public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.acceptEitherAsync(other, action, defaultExecutor));
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return wrap(delegate.acceptEitherAsync(other, action, defaultExecutor));
    }

    @Override
    public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
            Executor executor) {
        return wrap(delegate.acceptEitherAsync(other, action, executor));
    }

    @Override
    public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.runAfterEitherAsync(other, action, defaultExecutor));
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return wrap(delegate.runAfterEitherAsync(other, action, defaultExecutor));
    }

    @Override
    public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return wrap(delegate.runAfterEitherAsync(other, action, executor));
    }

    @Override
    public <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.thenComposeAsync(fn, defaultExecutor));
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return wrap(delegate.thenComposeAsync(fn, defaultExecutor));
    }

    @Override
    public <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return wrap(delegate.thenComposeAsync(fn, executor));
    }

    @Override
    public CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.whenCompleteAsync(action, defaultExecutor));
    }

    @Override
    public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return wrap(delegate.whenCompleteAsync(action, defaultExecutor));
    }

    @Override
    public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return wrap(delegate.whenCompleteAsync(action, executor));
    }

    @Override
    public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        // Force async execution to prevent event loop blocking
        return wrap(delegate.handleAsync(fn, defaultExecutor));
    }

    @Override
    public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return wrap(delegate.handleAsync(fn, defaultExecutor));
    }

    @Override
    public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return wrap(delegate.handleAsync(fn, executor));
    }

    @Override
    public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn) {
        // exceptionally doesn't have an async variant, but it's typically safe
        // as it only runs on exception and doesn't block
        return wrap(delegate.exceptionally(fn));
    }

    @Override
    public CompletableFuture<T> toCompletableFuture() {
        return delegate.toCompletableFuture();
    }

}
