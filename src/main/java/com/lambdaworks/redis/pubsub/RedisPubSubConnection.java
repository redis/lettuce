package com.lambdaworks.redis.pubsub;

import com.lambdaworks.redis.RedisAsyncConnection;

/**
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 08:39
 */
public interface RedisPubSubConnection<K, V> extends RedisAsyncConnection<K, V> {
    void psubscribe(K... patterns);

    void punsubscribe(K... patterns);

    void subscribe(K... channels);

    void unsubscribe(K... channels);
}
