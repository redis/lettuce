/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api;

import java.util.function.Function;

import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;

/**
 * A {@link CommandsFactory} that creates a command API bound to a {@link StatefulRedisClusterPubSubConnection Cluster Pub/Sub
 * connection}, for use with {@link StatefulRedisClusterPubSubConnection#commands(ClusterPubSubCommandsFactory)}.
 *
 * @param <C> the Cluster Pub/Sub connection type this factory accepts
 * @param <T> the command API type this factory creates
 * @author Aleksandar Todorov
 * @since 7.7
 */
public interface ClusterPubSubCommandsFactory<C extends StatefulRedisClusterPubSubConnection<?, ?>, T>
        extends PubSubCommandsFactory<C, T> {

    /**
     * Creates a {@code ClusterPubSubCommandsFactory} from a cache {@code key} and a {@code builder}.
     *
     * @param key the cache key, see {@link #key()}
     * @param builder creates the command API from a Cluster Pub/Sub connection
     * @param <C> the Cluster Pub/Sub connection type
     * @param <T> the command API type
     * @return a new factory
     */
    static <C extends StatefulRedisClusterPubSubConnection<?, ?>, T> ClusterPubSubCommandsFactory<C, T> of(Object key,
            Function<C, T> builder) {
        return new ClusterPubSubCommandsFactory<C, T>() {

            @Override
            public Object key() {
                return key;
            }

            @Override
            public T apply(C connection) {
                return builder.apply(connection);
            }

        };
    }

}
