package io.lettuce.core.cluster;

import java.util.Collection;

import io.lettuce.core.cluster.api.push.RedisClusterPushListener;

/**
 * A handler object that provides access to {@link RedisClusterPushListener}.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public interface ClusterPushHandler {

    /**
     * Add a new {@link RedisClusterPushListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    void addListener(RedisClusterPushListener listener);

    /**
     * Remove an existing {@link RedisClusterPushListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    void removeListener(RedisClusterPushListener listener);

    /**
     * Returns a collection of {@link RedisClusterPushListener}.
     *
     * @return the collection of listeners.
     */
    Collection<RedisClusterPushListener> getPushListeners();

}
