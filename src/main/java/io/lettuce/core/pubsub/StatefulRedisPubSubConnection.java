package io.lettuce.core.pubsub;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
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
     *
     * @return the synchronous API for the underlying connection.
     */
    RedisPubSubCommands<K, V> sync();

    /**
     * Returns the {@link RedisPubSubAsyncCommands} API for the current connection. Does not create a new connection.
     *
     * @return the asynchronous API for the underlying connection.
     */
    RedisPubSubAsyncCommands<K, V> async();

    /**
     * Returns the {@link RedisPubSubReactiveCommands} API for the current connection. Does not create a new connection.
     *
     * @return the reactive API for the underlying connection.
     */
    RedisPubSubReactiveCommands<K, V> reactive();

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

}
