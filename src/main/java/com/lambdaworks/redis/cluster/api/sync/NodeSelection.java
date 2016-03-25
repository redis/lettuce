package com.lambdaworks.redis.cluster.api.sync;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;

/**
 * Node selection with access to synchronous executed commands on the set. Commands are triggered concurrently to the selected
 * nodes and synchronized afterwards.
 *
 * This API is subject to incompatible changes in a future release. The API is exempt from any compatibility guarantees made by
 * lettuce. The current state implies nothing about the quality or performance of the API in question, only the fact that it is
 * not "API-frozen."
 *
 * The NodeSelection command API and its result types are a base for discussions.
 * 
 * @author Mark Paluch
 * @since 4.0
 */
public interface NodeSelection<K, V> extends NodeSelectionSupport<RedisCommands<K, V>, NodeSelectionCommands<K, V>> {

}
