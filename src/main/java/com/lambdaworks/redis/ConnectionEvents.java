package com.lambdaworks.redis;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Close Events Facility. Can register/unregister CloseListener and fire a closed event to all registered listeners.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
class ConnectionEvents {
    private final Set<RedisConnectionStateListener> listeners = Sets.newConcurrentHashSet();

    protected void fireEventRedisConnected(RedisChannelHandler<?, ?> connection) {
        for (RedisConnectionStateListener listener : listeners) {
            listener.onRedisConnected(connection);
        }
    }

    protected void fireEventRedisDisconnected(RedisChannelHandler<?, ?> connection) {
        for (RedisConnectionStateListener listener : listeners) {
            listener.onRedisDisconnected(connection);
        }
    }

    protected void fireEventRedisExceptionCaught(RedisChannelHandler<?, ?> connection, Throwable cause) {
        for (RedisConnectionStateListener listener : listeners) {
            listener.onRedisExceptionCaught(connection, cause);
        }
    }

    public void addListener(RedisConnectionStateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RedisConnectionStateListener listener) {
        listeners.remove(listener);
    }

}
