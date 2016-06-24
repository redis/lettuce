package com.lambdaworks.redis.api;

import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;

/**
 * Get the sync ans async method at the same time
 *
 * @author 52STE@sina.com on 2016/5/27.
 *
 */
public interface StatefulCommonConnection<K, V> extends StatefulConnection<K,V> {
    /**
     * Returns the {@link RedisClusterCommands} API for the current connection. Does not create a new connection.
     *
     * @return the synchronous API for the underlying connection.
     */
    RedisClusterCommands<K, V> sync();

    /**
     * Returns the {@link RedisClusterAsyncCommands} API for the current connection. Does not create a new connection.
     *
     * @return the asynchronous API for the underlying connection.
     */
    RedisClusterAsyncCommands<K, V> async();
    /**
     * Returns the {@link RedisClusterReactiveCommands} API for the current connection. Does not create a new connection.
     *
     * @return the reactive API for the underlying connection.
     */
    RedisClusterReactiveCommands<K, V> reactive();
}
