package com.lambdaworks.redis.cluster.api.async;

import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.NodeSelection;

/**
 * Node selection with access to asynchronous executed commands on the set.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface AsyncNodeSelection<K, V> extends NodeSelection<RedisAsyncCommands<K, V>, NodeSelectionAsyncCommands<K, V>> {

}
