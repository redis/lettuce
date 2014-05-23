package com.lambdaworks.redis;

/**
 * Complete async Redis API with 400+ Methods.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 14.05.14 12:24
 */
public interface RedisAsyncConnection<K, V> extends RedisHashesAsyncConnection<K, V>, RedisKeysAsyncConnection<K, V>,
        RedisStringsAsyncConnection<K, V>, RedisListsAsyncConnection<K, V>, RedisSetsAsyncConnection<K, V>,
        RedisSortedSetsAsyncConnection<K, V>, RedisScriptingAsyncConnection<K, V>, RedisServerAsyncConnection<K, V>,
        RedisHLLAsyncConnection<K, V> {

}
