package io.lettuce.core.cluster.api.async;

import io.lettuce.core.cluster.api.NodeSelectionSupport;
import io.lettuce.core.cluster.api.sync.NodeSelectionStreamCommands;

/**
 * Asynchronous and thread-safe Redis API to execute commands on a {@link NodeSelectionSupport}.
 *
 * @author Mark Paluch
 */
public interface NodeSelectionAsyncCommands<K, V> extends BaseNodeSelectionAsyncCommands<K, V>,
        NodeSelectionFunctionAsyncCommands<K, V>, NodeSelectionGeoAsyncCommands<K, V>, NodeSelectionHashAsyncCommands<K, V>,
        NodeSelectionHLLAsyncCommands<K, V>, NodeSelectionKeyAsyncCommands<K, V>, NodeSelectionListAsyncCommands<K, V>,
        NodeSelectionScriptingAsyncCommands<K, V>, NodeSelectionServerAsyncCommands<K, V>, NodeSelectionSetAsyncCommands<K, V>,
        NodeSelectionSortedSetAsyncCommands<K, V>, NodeSelectionStreamCommands<K, V>, NodeSelectionStringAsyncCommands<K, V> {
}
