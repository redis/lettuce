package com.lambdaworks.redis.sentinel.api;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.async.RedisSentinelAsyncCommands;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;

/**
 * An asynchronous thread-safe connection to a redis server. Multiple threads may share one
 * {@link StatefulRedisSentinelConnection}.
 * 
 * A {@link ConnectionWatchdog} monitors each connection and reconnects automatically until {@link #close} is called. All
 * pending commands will be (re)sent after successful reconnection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.0
 */
public interface StatefulRedisSentinelConnection<K, V> extends StatefulConnection<K, V> {

    /**
     * Returns the async sentinel API.
     * 
     * @return the asynchronous API for the underlying connection.
     */
    RedisSentinelAsyncCommands<K, V> async();
}
