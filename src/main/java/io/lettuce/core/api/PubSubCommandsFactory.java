/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api;

import java.util.function.Function;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * A {@link CommandsFactory} that creates a command API bound to a {@link StatefulRedisPubSubConnection Pub/Sub connection}, for
 * use with {@link StatefulRedisPubSubConnection#commands(PubSubCommandsFactory)}.
 *
 * @param <C> the Pub/Sub connection type this factory accepts
 * @param <T> the command API type this factory creates
 * @author Aleksandar Todorov
 * @since 7.7
 */
public interface PubSubCommandsFactory<C extends StatefulRedisPubSubConnection<?, ?>, T> extends CommandsFactory<C, T> {

    /**
     * Creates a {@code PubSubCommandsFactory} from a cache {@code key} and a {@code builder}.
     *
     * @param key the cache key, see {@link #key()}
     * @param builder creates the command API from a Pub/Sub connection
     * @param <C> the Pub/Sub connection type
     * @param <T> the command API type
     * @return a new factory
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
