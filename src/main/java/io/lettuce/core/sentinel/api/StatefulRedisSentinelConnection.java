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
    default <T> T commands(CommandsFactory<StatefulRedisSentinelConnection<K, V>, T> factory) {
        throw new UnsupportedOperationException("commands(CommandsFactory) is not implemented by this connection");
    }

}
