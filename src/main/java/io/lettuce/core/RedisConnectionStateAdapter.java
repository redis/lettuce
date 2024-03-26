package io.lettuce.core;

import java.net.SocketAddress;

/**
 * Convenience adapter with an empty implementation of all {@link RedisConnectionStateListener} callback methods.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public class RedisConnectionStateAdapter implements RedisConnectionStateListener {

    @Override
    public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
        // empty adapter method
    }

    @Override
    public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
        // empty adapter method
    }

    @Override
    public void onRedisExceptionCaught(RedisChannelHandler<?, ?> connection, Throwable cause) {
        // empty adapter method
    }

}
