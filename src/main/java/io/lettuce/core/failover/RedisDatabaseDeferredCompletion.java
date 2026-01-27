package io.lettuce.core.failover;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import io.lettuce.core.api.AsyncCloseable;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Completion handler for databases that are created asynchronously after the initial connection is established.
 * <p>
 * This class allows the multi-database connection to register callbacks that will be invoked when additional databases complete
 * their connection and health check process.
 *
 * @param <SC> the connection type
 */
class RedisDatabaseDeferredCompletion<SC extends StatefulRedisConnection<?, ?>> implements AsyncCloseable {

    private final List<CompletableFuture<RedisDatabaseImpl<SC>>> databaseFutures;

    /**
     * Creates a new async completion handler.
     *
     * @param databaseFutures list of futures for databases being created
     */
    RedisDatabaseDeferredCompletion(List<CompletableFuture<RedisDatabaseImpl<SC>>> databaseFutures) {
        this.databaseFutures = databaseFutures;
    }

    /**
     * Registers a callback to be invoked when each database future completes.
     *
     * @param action the callback to invoke with the database instance or exception
     */
    @SuppressWarnings({ "unchecked" })
    CompletableFuture<RedisDatabaseImpl<SC>>[] whenComplete(BiConsumer<RedisDatabaseImpl<SC>, Throwable> action) {
        // Returns array of futures that complete when each database future completes
        return databaseFutures.stream().map(f -> f.whenComplete(action)).toArray(CompletableFuture[]::new);
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return CompletableFuture.allOf(databaseFutures.stream().map(f -> f.exceptionally(e -> null).thenCompose(db -> {
            if (db != null) {
                return db.closeAsync();
            }
            return CompletableFuture.completedFuture(null);
        })).toArray(CompletableFuture[]::new));
    }

}
