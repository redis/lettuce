package com.lambdaworks.redis.sentinel.api;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.async.RedisSentinelAsyncConnection;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.protocol.RedisCommand;

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

    RedisSentinelAsyncConnection<K, V> async();
}
