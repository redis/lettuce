package com.lambdaworks.redis.api.sync;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;

/**
 * 
 * A complete synchronous and thread-safe Redis API with 400+ Methods.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public interface RedisCommands<K, V> extends RedisHashCommands<K, V>, RedisKeyCommands<K, V>, RedisStringCommands<K, V>,
        RedisListCommands<K, V>, RedisSetCommands<K, V>, RedisSortedSetCommands<K, V>, RedisScriptingCommands<K, V>,
        RedisServerCommands<K, V>, RedisHLLCommands<K, V>, BaseRedisCommands<K, V>, RedisClusterCommands<K, V>,
        RedisTransactionalCommands<K, V>, RedisGeoCommands<K, V>, RedisConnection<K, V> {

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
     * Change the selected database for the current Commands.
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
