package io.lettuce.core.pubsub;

import io.lettuce.core.api.PubSubCommandsFactory;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

/**
 * An asynchronous thread-safe pub/sub connection to a redis server. After one or more channels are subscribed to only pub/sub
 * related commands or {@literal QUIT} may be called.
 *
 * Incoming messages and results of the {@literal subscribe}/{@literal unsubscribe} calls will be passed to all registered
 * {@link RedisPubSubListener}s.
 *
 * A {@link io.lettuce.core.protocol.ConnectionWatchdog} monitors each connection and reconnects automatically until
 * {@link #close} is called. Channel and pattern subscriptions are renewed after reconnecting.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface StatefulRedisPubSubConnection<K, V> extends StatefulRedisConnection<K, V> {

    /**
     * Returns the {@link RedisPubSubCommands} API for the current connection. Does not create a new connection.
     * <p>
     * Note: this accessor is scheduled for removal in a future major release; a {@code commands(...)}-based replacement is
     * planned.
     *
     * @return the synchronous API for the underlying connection.
     */
    RedisPubSubCommands<K, V> sync();

    /**
     * Returns the {@link RedisPubSubAsyncCommands} API for the current connection. Does not create a new connection.
     * <p>
     * Note: this accessor is scheduled for removal in a future major release; a {@code commands(...)}-based replacement is
     * planned.
     *
     * @return the asynchronous API for the underlying connection.
     */
    RedisPubSubAsyncCommands<K, V> async();

    /**
     * Add a new {@link RedisPubSubListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    void addListener(RedisPubSubListener<K, V> listener);

    /**
     * Remove an existing {@link RedisPubSubListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    void removeListener(RedisPubSubListener<K, V> listener);

    /**
     * Returns the command API created by {@code factory}, bound to this connection. Does not create a new connection.
     * <p>
     * The command API is created once per connection and cached; calling this method again with the same {@code factory}
     * returns the same instance.
     *
     * @param factory the command API factory, must not be {@code null}
     * @param <T> the command API type
     * @return the command API bound to this connection
     * @throws UnsupportedOperationException if the connection implementation does not override this method. The default is
     *         provided only for source compatibility in Lettuce 7.x and becomes an abstract method in Lettuce 8.0.
     * @since 7.7
     */
    default <T> T commands(PubSubCommandsFactory<StatefulRedisPubSubConnection<K, V>, T> factory) {
        throw new UnsupportedOperationException("commands(PubSubCommandsFactory) is not implemented by this connection");
    }

}
