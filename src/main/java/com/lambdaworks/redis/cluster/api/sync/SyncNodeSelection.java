package com.lambdaworks.redis.cluster.api.sync;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.api.NodeSelection;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface SyncNodeSelection<K, V> extends NodeSelection<RedisCommands<K, V>, Object> {

}
