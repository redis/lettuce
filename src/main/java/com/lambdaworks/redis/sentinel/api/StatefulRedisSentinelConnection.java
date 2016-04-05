package com.lambdaworks.redis.sentinel.api;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;
import com.lambdaworks.redis.sentinel.api.rx.RedisSentinelReactiveCommands;
import com.lambdaworks.redis.sentinel.api.sync.RedisSentinelCommands;

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
     * 
     * @return the synchronous API for the underlying connection.
     */
    RedisSentinelCommands<K, V> sync();

    /**
     * Returns the {@link RedisSentinelAsyncCommands} API for the current connection. Does not create a new connection. *
     * 
     * @return the asynchronous API for the underlying connection.
     */
    RedisSentinelAsyncCommands<K, V> async();

    /**
     * Returns the {@link RedisSentinelReactiveCommands} API for the current connection. Does not create a new connection. *
     *
     * @return the reactive API for the underlying connection.
     */
    RedisSentinelReactiveCommands<K, V> reactive();
}
