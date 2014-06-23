package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

/**
 * Complete async Redis API with 400+ Methods.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 14.05.14 12:24
 */
public interface RedisAsyncConnection<K, V> extends RedisHashesAsyncConnection<K, V>, RedisKeysAsyncConnection<K, V>,
        RedisStringsAsyncConnection<K, V>, RedisListsAsyncConnection<K, V>, RedisSetsAsyncConnection<K, V>,
        RedisSortedSetsAsyncConnection<K, V>, RedisScriptingAsyncConnection<K, V>, RedisServerAsyncConnection<K, V>,
        RedisHLLAsyncConnection<K, V>, BaseRedisAsyncConnection<K, V> {

    /**
     * Set the default timeout for operations.
     * 
     * @param timeout
     * @param unit
     */
    void setTimeout(long timeout, TimeUnit unit);

    /**
     * Change the selected database for the current connection.
     * 
     * @param db
     * @return String simple-string-reply
     */
    String select(int db);

    /**
     * Authenticate to the server.
     * 
     * @param password
     * @return String simple-string-reply
     */
    String auth(String password);
}
