package io.lettuce.core.api;

import io.lettuce.core.TransactionBuilder;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ConnectionWatchdog;

/**
 * A thread-safe connection to a redis server. Multiple threads may share one {@link StatefulRedisConnection}.
 *
 * A {@link ConnectionWatchdog} monitors each connection and reconnects automatically until {@link #close} is called. All
 * pending commands will be (re)sent after successful reconnection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface StatefulRedisConnection<K, V> extends StatefulConnection<K, V> {

    /**
     *
     * @return true, if the connection is within a transaction.
     */
    boolean isMulti();

    /**
     * Returns the {@link RedisCommands} API for the current connection. Does not create a new connection.
     * <p>
     * Note: this accessor is scheduled for removal in a future major release; a {@code commands(...)}-based replacement is
     * planned.
     *
     * @return the synchronous API for the underlying connection.
     */
    RedisCommands<K, V> sync();

    /**
     * Returns the {@link RedisAsyncCommands} API for the current connection. Does not create a new connection.
     * <p>
     * Note: this accessor is scheduled for removal in a future major release; a {@code commands(...)}-based replacement is
     * planned.
     *
     * @return the asynchronous API for the underlying connection.
     */
    RedisAsyncCommands<K, V> async();

    /**
     * Returns the {@link RedisReactiveCommands} API for the current connection. Does not create a new connection.
     *
     * @return the reactive API for the underlying connection.
     * @deprecated since 7.7, use {@link #commands(CommandsFactory)} with {@link RedisReactiveCommands#factory()} instead;
     *             scheduled for removal in Lettuce 8.0.
     */
    @Deprecated
    RedisReactiveCommands<K, V> reactive();

    /**
     * Add a new {@link PushListener listener} to consume push messages.
     *
     * @param listener the listener, must not be {@code null}.
     * @since 6.0
     */
    void addListener(PushListener listener);

    /**
     * Remove an existing {@link PushListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     * @since 6.0
     */
    void removeListener(PushListener listener);

    /**
     * Returns the command API created by {@code factory}, bound to this connection. Does not create a new connection.
     * <p>
     * The command API is created once per connection and cached; calling this method again with the same {@code factory}
     * returns the same instance.
     *
     * @param factory the command API factory, must not be {@code null}
     * @param <T> the command API type
     * @return the command API bound to this connection
     * @since 7.7
     */
    <T> T commands(CommandsFactory<StatefulRedisConnection<K, V>, T> factory);

    /**
     * Create a new transaction builder for atomic transaction dispatch.
     * <p>
     * This method creates a {@link TransactionBuilder} that collects commands and dispatches them atomically as a single
     * MULTI/EXEC block. This approach is thread-safe and prevents command interleaving from other threads.
     * <p>
     * Usage example:
     *
     * <pre>
     *
     * {
     *     &#64;code
     *     TransactionResult result = connection.transaction().set("key1", "value1").incr("counter").execute();
     * }
     * </pre>
     *
     * @return a new transaction builder.
     * @since 7.6
     */
    TransactionBuilder<K, V> transaction();

    /**
     * Create a new transaction builder with WATCH support for optimistic locking.
     * <p>
     * The specified keys will be watched before the transaction is executed. If any of the watched keys are modified by another
     * client before the transaction executes, the entire transaction will be aborted and the result will indicate the
     * transaction was discarded.
     * <p>
     * Usage example:
     *
     * <pre>
     *
     * {
     *     &#64;code
     *     TransactionResult result = connection.transaction("mykey").get("mykey").set("mykey", "newvalue").execute();
     *     if (result.wasDiscarded()) {
     *         // Another client modified "mykey", retry logic here
     *     }
     * }
     * </pre>
     *
     * @param watchKeys the keys to watch.
     * @return a new transaction builder with WATCH.
     * @since 7.6
     */
    @SuppressWarnings("unchecked")
    TransactionBuilder<K, V> transaction(K... watchKeys);

}
