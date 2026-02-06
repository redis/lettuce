package io.lettuce.core.failover;

import java.util.concurrent.*;

import io.lettuce.core.BaseConnectionFuture;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.failover.api.BaseRedisMultiDbConnection;

/**
 * A {@code MultiDbConnectionFuture} represents the result of an asynchronous multi-database connection initialization.
 * <p>
 * This future wrapper delegates all method calls to the underlying {@link CompletableFuture}.
 *
 * @param <C> Connection type
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
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
     * Create a {@link MultiDbConnectionFuture} from a {@link CompletableFuture}.
     *
     * @param future the CompletableFuture to wrap
     * @param <C> Connection type
     * @return the wrapped future
     */
    public static <C extends BaseRedisMultiDbConnection> MultiDbConnectionFuture<C> from(CompletableFuture<C> future) {
        return new MultiDbConnectionFuture<C>(future);
    }

    @Override
    protected <U> CompletionStage<U> wrap(CompletableFuture<U> future) {
        // We can't preserve K,V type parameters when wrapping arbitrary U types,
        // so we just return the CompletableFuture directly
        return future;
    }

}
