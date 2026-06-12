/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api;

import java.util.function.Function;

import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;

/**
 * A {@link PubSubCommandsFactory} for the Cluster Pub/Sub family. Its connection type is bound to
 * {@link StatefulRedisClusterPubSubConnection}.
 * <p>
 * Being a distinct type from {@link PubSubCommandsFactory} gives Cluster Pub/Sub connections a
 * {@code commands(ClusterPubSubCommandsFactory)} overload with its own erasure, so a Cluster Pub/Sub factory is rejected on a
 * plain Pub/Sub (or standalone) connection at compile time.
 *
 * @param <C> Cluster Pub/Sub connection type the factory accepts.
 * @param <T> command API type the factory produces.
 * @since 7.7
 */
public interface ClusterPubSubCommandsFactory<C extends StatefulRedisClusterPubSubConnection<?, ?>, T>
        extends PubSubCommandsFactory<C, T> {

    /**
     * Create a {@link ClusterPubSubCommandsFactory} from a cache {@code key} and a {@code builder}.
     *
     * @param key the cache key (see {@link #key()}).
     * @param builder builds the command API from the Cluster Pub/Sub connection.
     * @param <C> Cluster Pub/Sub connection type.
     * @param <T> command API type.
     * @return a new factory.
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
