package io.lettuce.core.cluster.pubsub.api.reactive;

import io.lettuce.core.cluster.api.NodeSelectionSupport;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

/**
 * Node selection with access to {@link RedisPubSubCommands}.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public interface PubSubReactiveNodeSelection<K, V>
        extends NodeSelectionSupport<RedisPubSubReactiveCommands<K, V>, NodeSelectionPubSubReactiveCommands<K, V>> {

}
