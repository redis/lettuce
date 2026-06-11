/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api;

import java.util.function.Function;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * A {@link CommandsFactory} for the Pub/Sub family. Its connection type is bound to {@link StatefulRedisPubSubConnection}, so a
 * {@code PubSubCommandsFactory} can only ever be built for (and applied to) a Pub/Sub — or Cluster Pub/Sub — connection.
 * <p>
 * Being a distinct type from {@link CommandsFactory} (rather than just a differently-bounded one) is deliberate: it gives
 * connections a {@code commands(PubSubCommandsFactory)} overload with a different erasure from
 * {@code commands(CommandsFactory)}, so the two coexist on the Pub/Sub connection without a name clash and overload resolution
 * picks the right one.
 *
 * @param <C> Pub/Sub connection type the factory accepts.
 * @param <T> command API type the factory produces.
 * @since 7.7
 */
public interface PubSubCommandsFactory<C extends StatefulRedisPubSubConnection<?, ?>, T> extends CommandsFactory<C, T> {

    /**
     * Create a {@link PubSubCommandsFactory} from a cache {@code key} and a {@code builder}.
     *
     * @param key the cache key (see {@link #key()}).
     * @param builder builds the command API from the Pub/Sub connection.
     * @param <C> Pub/Sub connection type.
     * @param <T> command API type.
     * @return a new factory.
     */
    static <C extends StatefulRedisPubSubConnection<?, ?>, T> PubSubCommandsFactory<C, T> of(Object key,
            Function<C, T> builder) {
        return new PubSubCommandsFactory<C, T>() {

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
