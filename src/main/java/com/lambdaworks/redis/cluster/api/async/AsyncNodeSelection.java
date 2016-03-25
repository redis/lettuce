package com.lambdaworks.redis.cluster.api.async;

import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;

/**
 * Node selection with access to asynchronous executed commands on the set. This API is subject to incompatible changes in a
 * future release. The API is exempt from any compatibility guarantees made by lettuce. The current state implies nothing about
 * the quality or performance of the API in question, only the fact that it is not "API-frozen."
 *
 * The NodeSelection command API and its result types are a base for discussions.
 * 
 * @author Mark Paluch
 * @since 4.0
 */
public interface AsyncNodeSelection<K, V> extends
        NodeSelectionSupport<RedisAsyncCommands<K, V>, NodeSelectionAsyncCommands<K, V>> {

}
