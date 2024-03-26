package io.lettuce.core.cluster.pubsub.api.sync;

import io.lettuce.core.cluster.api.NodeSelectionSupport;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

/**
 * Node selection with access to {@link RedisPubSubCommands}.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public interface PubSubNodeSelection<K, V>
        extends NodeSelectionSupport<RedisPubSubCommands<K, V>, NodeSelectionPubSubCommands<K, V>> {

}
