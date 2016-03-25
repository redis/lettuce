package com.lambdaworks.redis.api.async;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;

/**
 * A complete asynchronous and thread-safe Redis API with 400+ Methods.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public interface RedisAsyncCommands<K, V> extends RedisHashAsyncCommands<K, V>, RedisKeyAsyncCommands<K, V>,
        RedisStringAsyncCommands<K, V>, RedisListAsyncCommands<K, V>, RedisSetAsyncCommands<K, V>,
        RedisSortedSetAsyncCommands<K, V>, RedisScriptingAsyncCommands<K, V>, RedisServerAsyncCommands<K, V>,
        RedisHLLAsyncCommands<K, V>, BaseRedisAsyncCommands<K, V>, RedisClusterAsyncCommands<K, V>,
        RedisTransactionalAsyncCommands<K, V>, RedisGeoAsyncCommands<K, V>, RedisAsyncConnection<K, V> {

    /**
     * Set the default timeout for operations.
     * 
     * @param timeout the timeout value
     * @param unit the unit of the timeout value
     */
    void setTimeout(long timeout, TimeUnit unit);

    /**
     * Authenticate to the server.
     * 
     * @param password the password
     * @return String simple-string-reply
     */
    String auth(String password);

    /**
     * Change the selected database for the current connection.
     * 
     * @param db the database number
     * @return String simple-string-reply
     */
    String select(int db);

    /**
     * @return the underlying connection.
     */
    StatefulRedisConnection<K, V> getStatefulConnection();

}
