package io.lettuce.core.cluster.pubsub.api.async;

import io.lettuce.core.cluster.api.NodeSelectionSupport;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * Node selection with access to asynchronous executed commands on the set.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public interface PubSubAsyncNodeSelection<K, V>
        extends NodeSelectionSupport<RedisPubSubAsyncCommands<K, V>, NodeSelectionPubSubAsyncCommands<K, V>> {

}
