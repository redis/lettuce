package io.lettuce.core.cluster.api.sync;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.NodeSelectionSupport;

/**
 * Node selection with access to synchronous executed commands on the set. Commands are triggered concurrently to the selected
 * nodes and synchronized afterwards.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public interface NodeSelection<K, V> extends NodeSelectionSupport<RedisCommands<K, V>, NodeSelectionCommands<K, V>> {

}
