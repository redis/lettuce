package io.lettuce.core.failover;

import java.util.concurrent.*;

import io.lettuce.core.BaseConnectionFuture;
import io.lettuce.core.failover.api.BaseRedisMultiDbConnection;

/**
 * A {@code MultiDbConnectionFuture} represents the result of an asynchronous multi-database connection initialization.
 * <p>
 * This future wrapper ensures that all callbacks (thenApply, thenAccept, etc.) execute on a separate thread pool rather than on
 * Netty event loop threads. This prevents deadlocks when users call blocking sync operations inside callbacks.
 * <p>
 * Example of the problem this solves:
 *
 * <pre>
 * {@code
 * // DANGEROUS with plain CompletableFuture - can deadlock!
 * future.thenApply(conn -> conn.sync().ping());
 *
 * // SAFE with MultiDbConnectionFuture - always runs on separate thread
 * future.thenApply(conn -> conn.sync().ping());
 * }
 * </pre>
 *
 * @param <K> Key type
 * @param <V> Value type
 * @author Ali Takavci
 * @since 7.4
 */
public class MultiDbConnectionFuture<C extends BaseRedisMultiDbConnection> extends BaseConnectionFuture<C> {

    /**
     * Create a new {@link MultiDbConnectionFuture} wrapping the given delegate future.
     *
     * @param delegate the underlying CompletableFuture
     */
    public MultiDbConnectionFuture(CompletableFuture<C> delegate) {

        super(delegate);
    }

    /**
     * Create a new {@link MultiDbConnectionFuture} wrapping the given delegate future with a custom executor.
     *
     * @param delegate the underlying CompletableFuture
     * @param defaultExecutor the executor to use for async callbacks
     */
    public MultiDbConnectionFuture(CompletableFuture<C> delegate, Executor defaultExecutor) {

        super(delegate, defaultExecutor);
    }

    /**
     * Create a {@link MultiDbConnectionFuture} from a {@link CompletableFuture}.
     *
     * @param future the CompletableFuture to wrap
     * @param <K> Key type
     * @param <V> Value type
     * @return the wrapped future
     */
    public static <C extends BaseRedisMultiDbConnection> MultiDbConnectionFuture<C> from(CompletableFuture<C> future) {
        return new MultiDbConnectionFuture<C>(future);

    }

    /**
     * Create a {@link MultiDbConnectionFuture} from a {@link CompletableFuture} with a custom executor.
     *
     * @param future the CompletableFuture to wrap
     * @param executor the executor to use for async callbacks
     * @param <K> Key type
     * @param <V> Value type
     * @return the wrapped future
     */
    public static <C extends BaseRedisMultiDbConnection> MultiDbConnectionFuture<C> from(CompletableFuture<C> future,
            Executor executor) {
        return new MultiDbConnectionFuture<C>(future, executor);
    }

    @Override
    protected <U> CompletionStage<U> wrap(CompletableFuture<U> future) {
        // We can't preserve K,V type parameters when wrapping arbitrary U types,
        // so we just return the CompletableFuture directly
        return future;
    }

}
