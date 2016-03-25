package com.lambdaworks.redis.cluster.api.sync;

import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;

/**
 * Synchronous and thread-safe Redis API to execute commands on a {@link NodeSelectionSupport}.
 * 
 * @author Mark Paluch
 */
public interface NodeSelectionCommands<K, V> extends BaseNodeSelectionCommands<K, V>, NodeSelectionHashCommands<K, V>,
        NodeSelectionHLLCommands<K, V>, NodeSelectionKeyCommands<K, V>, NodeSelectionListCommands<K, V>,
        NodeSelectionScriptingCommands<K, V>, NodeSelectionServerCommands<K, V>, NodeSelectionSetCommands<K, V>,
        NodeSelectionSortedSetCommands<K, V>, NodeSelectionStringCommands<K, V>, NodeSelectionGeoCommands<K, V> {

}
