package io.lettuce.core;

import java.net.SocketAddress;

/**
 * Simple interface for Redis connection state monitoring.
 *
 * @author ze
 * @author Mark Paluch
 */
public interface RedisConnectionStateListener {

    /**
     * Event handler for successful connection event.
     *
     * @param connection Source connection.
     * @deprecated since 4.4, use {@link RedisConnectionStateListener#onRedisConnected(RedisChannelHandler, SocketAddress)}.
     */
    @Deprecated
    default void onRedisConnected(RedisChannelHandler<?, ?> connection) {
    }

    /**
     * Event handler for successful connection event. Delegates by default to {@link #onRedisConnected(RedisChannelHandler)}.
     *
     * @param connection Source connection.
     * @param socketAddress remote {@link SocketAddress}.
     * @since 4.4
     */
    default void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
        onRedisConnected(connection);
    }

    /**
     * Event handler for disconnection event.
     *
     * @param connection Source connection.
     */
    default void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
    }

    /**
     *
     * Event handler for exceptions.
     *
     * @param connection Source connection.
     *
     * @param cause Caught exception.
     */
    default void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, Throwable cause) {

    }

}
