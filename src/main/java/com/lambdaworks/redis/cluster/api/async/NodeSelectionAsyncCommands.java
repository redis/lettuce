package com.lambdaworks.redis.cluster.api.async;

import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;

/**
 * Asynchronous and thread-safe Redis API to execute commands on a {@link NodeSelectionSupport}.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface NodeSelectionAsyncCommands<K, V> extends BaseNodeSelectionAsyncCommands<K, V>,
        NodeSelectionHashAsyncCommands<K, V>, NodeSelectionHLLAsyncCommands<K, V>, NodeSelectionKeyAsyncCommands<K, V>,
        NodeSelectionListAsyncCommands<K, V>, NodeSelectionScriptingAsyncCommands<K, V>,
        NodeSelectionServerAsyncCommands<K, V>, NodeSelectionSetAsyncCommands<K, V>, NodeSelectionSortedSetAsyncCommands<K, V>,
        NodeSelectionStringAsyncCommands<K, V>, NodeSelectionGeoAsyncCommands<K, V> {

}
