package io.lettuce.core.cluster.api.async;

import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.api.NodeSelectionSupport;

/**
 * Node selection with access to asynchronous executed commands on the set.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public interface AsyncNodeSelection<K, V>
        extends NodeSelectionSupport<RedisAsyncCommands<K, V>, NodeSelectionAsyncCommands<K, V>> {

}
