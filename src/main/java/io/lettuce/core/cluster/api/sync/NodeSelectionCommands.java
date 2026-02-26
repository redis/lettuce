package io.lettuce.core.cluster.api.sync;

import io.lettuce.core.cluster.api.NodeSelectionSupport;

/**
 * Synchronous and thread-safe Redis API to execute commands on a {@link NodeSelectionSupport}.
 *
 * @author Mark Paluch
 * @author Tihomir Mateev
 */
public interface NodeSelectionCommands<K, V> extends BaseNodeSelectionCommands<K, V>, NodeSelectionFunctionCommands<K, V>,
        NodeSelectionGeoCommands<K, V>, NodeSelectionHashCommands<K, V>, NodeSelectionHLLCommands<K, V>,
        NodeSelectionKeyCommands<K, V>, NodeSelectionListCommands<K, V>, NodeSelectionScriptingCommands<K, V>,
        NodeSelectionServerCommands<K, V>, NodeSelectionSetCommands<K, V>, NodeSelectionSortedSetCommands<K, V>,
        NodeSelectionStreamCommands<K, V>, NodeSelectionStringCommands<K, V>, NodeSelectionJsonCommands<K, V>,
        NodeSelectionVectorSetCommands<K, V>, NodeSelectionSearchCommands<K, V> {
}
