/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api;

import java.util.function.Function;

/**
 * Creates a command API bound to a connection, for use with {@link StatefulRedisConnection#commands(CommandsFactory)}. A
 * command API that can be obtained this way provides a static {@code factory()} method that returns its
 * {@code CommandsFactory}; alternatively, build one with {@link #of(Object, Function)}.
 *
 * @param <C> the connection type this factory accepts
 * @param <T> the command API type this factory creates
 * @author Aleksandar Todorov
 * @since 7.7
 */
public interface CommandsFactory<C, T> extends Function<C, T> {

    /**
     * Returns the key under which the created command API is cached on the connection. A connection holds one instance per key.
     *
     * @return the cache key
     */
    Object key();

    /**
     * Creates a {@code CommandsFactory} from a cache {@code key} and a {@code builder}.
     *
     * @param key the cache key, see {@link #key()}
     * @param builder creates the command API from a connection
     * @param <C> the connection type
     * @param <T> the command API type
     * @return a new factory
     */
    static <C, T> CommandsFactory<C, T> of(Object key, Function<C, T> builder) {
        return new CommandsFactory<C, T>() {

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
