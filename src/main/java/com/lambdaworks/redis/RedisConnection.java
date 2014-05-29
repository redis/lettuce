package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

/**
 * 
 * Complete synchronous Redis API with 400+ Methods.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 14.05.14 12:21
 */
public interface RedisConnection<K, V> extends RedisHashesConnection<K, V>, RedisKeysConnection<K, V>,
        RedisStringsConnection<K, V>, RedisListsConnection<K, V>, RedisSetsConnection<K, V>, RedisSortedSetsConnection<K, V>,
        RedisScriptingConnection<K, V>, RedisServerConnection<K, V>, RedisHLLConnection<K, V>, BaseRedisConnection<K, V> {

    void setTimeout(long timeout, TimeUnit unit);

    String auth(String password);

    String select(int db);
}
