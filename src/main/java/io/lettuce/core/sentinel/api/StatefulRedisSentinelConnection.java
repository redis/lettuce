package io.lettuce.core.sentinel.api;

import io.lettuce.core.api.CommandsFactory;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.protocol.ConnectionWatchdog;
import io.lettuce.core.sentinel.api.async.RedisSentinelAsyncCommands;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;

/**
 * A thread-safe connection to a redis server. Multiple threads may share one {@link StatefulRedisSentinelConnection}.
 *
 * A {@link ConnectionWatchdog} monitors each connection and reconnects automatically until {@link #close} is called. All
 * pending commands will be (re)sent after successful reconnection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface StatefulRedisSentinelConnection<K, V> extends StatefulConnection<K, V> {

    /**
     * Returns the {@link RedisSentinelCommands} API for the current connection. Does not create a new connection.
     * <p>
     * Note: this accessor is scheduled for removal in a future major release; a {@code commands(...)}-based replacement is
     * planned.
     *
     * @return the synchronous API for the underlying connection.
     */
    RedisSentinelCommands<K, V> sync();

    /**
     * Returns the {@link RedisSentinelAsyncCommands} API for the current connection. Does not create a new connection.
     * <p>
     * Note: this accessor is scheduled for removal in a future major release; a {@code commands(...)}-based replacement is
     * planned.
     *
     * @return the asynchronous API for the underlying connection.
     */
    RedisSentinelAsyncCommands<K, V> async();

    /**
     * Returns the {@link RedisSentinelReactiveCommands} API for the current connection. Does not create a new connection.
     *
     * @return the reactive API for the underlying connection.
     * @deprecated since 7.7, use {@link #commands(CommandsFactory)} with {@link RedisSentinelReactiveCommands#factory()}
     *             instead; scheduled for removal in Lettuce 8.0.
     */
    @Deprecated
    RedisSentinelReactiveCommands<K, V> reactive();

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
    <T> T commands(CommandsFactory<StatefulRedisSentinelConnection<K, V>, T> factory);

}
