package io.lettuce.core.cluster.api.push;

import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Interface to be implemented by push message listeners that are interested in listening to {@link PushMessage} using Redis
 * Cluster.
 *
 * @author Mark Paluch
 * @since 6.0
 * @see PushMessage
 * @see io.lettuce.core.api.push.PushListener
 */
@FunctionalInterface
public interface RedisClusterPushListener {

    /**
     * Handle a push message.
     *
     * @param node the {@link RedisClusterNode} from which the {@code message} originates.
     * @param message message to respond to.
     */
    void onPushMessage(RedisClusterNode node, PushMessage message);

}
