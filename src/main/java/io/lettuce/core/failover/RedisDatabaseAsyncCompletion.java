package io.lettuce.core.failover;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Completion handler for databases that are created asynchronously after the initial connection is established.
 * <p>
 * This class allows the multi-database connection to register callbacks that will be invoked when additional databases complete
 * their connection and health check process.
 *
 * @param <SC> the connection type
 */
class RedisDatabaseAsyncCompletion<SC extends StatefulRedisConnection<?, ?>> {

    private final List<CompletableFuture<RedisDatabaseImpl<SC>>> databaseFutures;

    /**
     * Creates a new async completion handler.
     *
     * @param databaseFutures list of futures for databases being created
     */
    RedisDatabaseAsyncCompletion(List<CompletableFuture<RedisDatabaseImpl<SC>>> databaseFutures) {
        this.databaseFutures = databaseFutures;
    }

    /**
     * Registers a callback to be invoked when each database future completes.
     *
     * @param action the callback to invoke with the database instance or exception
     */
    void whenComplete(BiConsumer<RedisDatabaseImpl<SC>, Throwable> action) {
        databaseFutures.forEach(future -> future.whenComplete(action));
    }

}
