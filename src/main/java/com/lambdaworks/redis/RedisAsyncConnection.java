package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.BaseRedisAsyncConnection;
import com.lambdaworks.redis.api.async.RedisHLLAsyncConnection;
import com.lambdaworks.redis.api.async.RedisHashesAsyncConnection;
import com.lambdaworks.redis.api.async.RedisKeysAsyncConnection;
import com.lambdaworks.redis.api.async.RedisListsAsyncConnection;
import com.lambdaworks.redis.api.async.RedisScriptingAsyncConnection;
import com.lambdaworks.redis.api.async.RedisServerAsyncConnection;
import com.lambdaworks.redis.api.async.RedisSetsAsyncConnection;
import com.lambdaworks.redis.api.async.RedisSortedSetsAsyncConnection;
import com.lambdaworks.redis.api.async.RedisStringsAsyncConnection;
import com.lambdaworks.redis.api.async.RedisTransactionalAsyncConnection;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;

/**
 * A complete asynchronous and thread-safe Redis API with 400+ Methods.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public interface RedisAsyncConnection<K, V> extends RedisHashesAsyncConnection<K, V>, RedisKeysAsyncConnection<K, V>,
        RedisStringsAsyncConnection<K, V>, RedisListsAsyncConnection<K, V>, RedisSetsAsyncConnection<K, V>,
        RedisSortedSetsAsyncConnection<K, V>, RedisScriptingAsyncConnection<K, V>, RedisServerAsyncConnection<K, V>,
        RedisHLLAsyncConnection<K, V>, BaseRedisAsyncConnection<K, V>, RedisClusterAsyncConnection<K, V>,
        RedisTransactionalAsyncConnection<K, V> {

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
