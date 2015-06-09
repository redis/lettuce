package com.lambdaworks.redis.api;

import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;

/**
 * An asynchronous thread-safe connection to a redis server. Multiple threads may share one {@link StatefulRedisConnection}.
 * 
 * A {@link ConnectionWatchdog} monitors each connection and reconnects automatically until {@link #close} is called. All
 * pending commands will be (re)sent after successful reconnection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.0
 */
public interface StatefulRedisConnection<K, V> extends StatefulConnection<K, V> {

    /**
     *
     * @return true, if the connection is within a transaction.
     */
    boolean isMulti();

    /**
     * Returns the {@link RedisAsyncConnection} API for the current connection. Does not create a new connection.
     * 
     * @return the asynchronous API for the underlying connection.
     */
    RedisAsyncConnection<K, V> async();

    /**
     * Returns the {@link RedisConnection} API for the current connection. Does not create a new connection.
     * 
     * @return the synchronous API for the underlying connection.
     */
    RedisConnection<K, V> sync();
}
